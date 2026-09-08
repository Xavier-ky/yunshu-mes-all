# 云枢小智 — 第一批 RAG 知识目录

> 版本：0.1（2026-07-15）  
> 范围：以 `fan_mes` 当前十步黄金流程为唯一业务主线。  
> 原则：只收录已经在真实页面、Spring Boot 接口、生命周期服务或数据库主链中使用的知识；实时业务事实仍通过 MES API 查询。

## 1. RAG 的事实边界

### 可以进入 RAG 的内容

- 稳定的业务流程、角色职责、SOP、生命周期规则、异常处置规则、工艺/质量规范。
- 经审核后导出的产品、物料、BOM、工艺路线、检验模板等**版本化主数据说明**。
- 从后端代码或迁移脚本自动生成、并由业务负责人确认的“系统规则说明”。

### 不可以直接进入 RAG 的内容

| 内容 | 正确入口 | 原因 |
|---|---|---|
| 当前工单状态、计划量、完成量、派工与待办 | `/api/planning/workflow/pipeline/{key}`、任务/派工 API | 随时变化，必须受 JWT 和数据范围控制 |
| 当前库存余额、批次可用量、预留量 | 仓储实时 API | 向量索引无法保证时效与账实一致 |
| 当前 IPQC 判定、领料/报工/入库单据 | 质量、仓储、生产实时 API | 是执行门禁事实，不能由文档替代 |
| 用户密码、连接信息、Token、`.env` | 不入库 | 敏感信息 |
| 原始种子 SQL、操作日志、聊天记录 | 评测库/审计库，不入知识库 | 包含测试数据、动态事实或潜在敏感字段 |

## 2. 已核对的真实业务知识源

### 2.1 P0：第一批直接入库的业务知识

| ID | 来源文件 | 已验证的真实业务内容 | 关联页面 / API / 表 | RAG 知识域 | 访问范围 | 入库处理 |
|---|---|---|---|---|---|---|
| K01 | `MES-GOLDEN-PATH.md` | 订单→工单→齐套→排产→派工→领料→报工→IPQC→入库→追溯的页面操作、角色和演示路径 | 10 个实际页面；`customer_order` 至 `product_sn` 全链 | `workflow_sop` | 全体业务角色，按步骤过滤 | 以“每一步 + 常见下一步”为语义单元切分；保留路由、角色、工单状态 |
| K02 | `docs/workflow-golden-path.md` | 十步 API、主表、前/后生命周期、桥接服务、验收标准 | `WorkflowPipelineService`、`DispatchSyncService`、领料/报工/IPQC/入库服务 | `workflow_rule` | 主管、仓库、操作工、质检、管理员 | 按“步骤矩阵、状态门禁、桥接、验收”切分；保留 API 与表名 |
| K03 | `docs/role-views.md` | 六类角色的职责、可访问页面、业务对象和操作语境 | 真实路由、核心表、角色编码 | `role_sop` | 按 `role_scope` 严格过滤 | 按角色切分；检索时强制与 JWT 角色交集过滤 |
| K04 | `docs/core-database-design.md` | 工单主链、单头—单行规则、库存余额/流水规则、Agent 平台关系 | `work_order`、任务、派工、库存、质检、追溯、`agent_*` | `business_data_rule` | 管理员、主管；一般用户仅可使用业务解释片段 | 不直接回答 SQL；按主题切分，保留表/字段语义与文档版本 |
| K05 | `docs/database-landscape.md` | V29 后 Compat 唯一主线、已退役重复表、十步应使用的当前表 | `wm_issue_*`、`pro_feedback`、`qc_ipqc`、`wm_product_recpt` | `system_rule` | 管理员、Agent 内部 | 仅纳入“当前唯一主线/禁用旧表”规则；不纳入历史删表明细全文 |

### 2.2 P0：从真实实现派生、审核后入库的规则知识

这些不是把 Java/SQL 原文直接向量化，而是由脚本生成简洁 Markdown，再由业务负责人确认。每份派生知识都要保存源码路径、Git 提交号/文件哈希和审核记录。

| ID | 权威源码 | 应派生出的知识卡 | 真实业务约束 | 关联 Agent |
|---|---|---|---|---|
| K06 | `planning/workflow/WorkOrderLifecycleService.java` + `V28__work_order_lifecycle.sql` | 《工单生命周期与门禁规则》 | 领料仅 RELEASED/KITTING_OK/SCHEDULED；报工仅 MATERIAL_ISSUED/IN_PROGRESS/QC_PENDING；入库仅 QC_PASSED/COMPLETED；`QC_FAILED` 需回主管处理 | A0、A6、A7、A8、B1、B3、B4；**已派生入库** |
| K07 | `planning/controller/KittingController.java` + `docs/workflow-golden-path.md` | 《齐套分析与预留操作规则》 | 以 `work_order_id` 汇总 BOM/库存；reserve/release 会改变预留业务结果 | A3、A4、A6；**已派生入库** |
| K08 | `planning/compat/service/DispatchSyncService.java` + ProTask 接口 | 《排产任务到派工规则》 | `production_task` 保存后，受 `mes.workflow.auto-dispatch` 控制自动同步 `dispatch_task` | A4、A5、A7 |
| K09 | `inventory/compat/service/WmIssueService.java` + `V8__wm_ktg_p0_documents.sql` | 《生产领料执行与消耗同步规则》 | 领料数量校验；执行后写 `wm_item_consume` 并推进 MATERIAL_ISSUED | A6、B2 |
| K10 | `production/compat/service/ProFeedbackService.java` + `V13__pro_feedback_wm_consume_produce.sql` | 《现场报工与进度回写规则》 | 报工执行后回写派工完成量、同步 `production_report`、推进 QC_PENDING | B1、A7 |
| K11 | `quality/compat/controller/QcIpqcController.java` + `V15__qc_ktg_quality.sql` | 《IPQC 判定与质量放行规则》 | ACCEPT → QC_PASSED；其他结果 → QC_FAILED；检验模板/合格量需校验 | B3、C10 |
| K12 | `inventory/compat/service/WmProductRecptService.java` + `V9__wm_ktg_p0_sales.sql` | 《成品入库与追溯同步规则》 | 入库受 QC 门禁；执行后 COMPLETED 并同步追溯 | B4、B5 |
| K13 | `traceability/controller/ReverseTraceController.java` | 《工单、SN、批次追溯说明》 | 使用工单、领料、报工、质检、入库、SN、物料绑定、库存批次形成全链 | B5、C10 |

### 2.3 P1：应从真实主数据生成的受控知识快照

### 2.2a 已部署的系统派生规则卡（2026-07-15）

K06–K13 已全部整理为可引用 Markdown 并导入 `mes_knowledge_v1`。其中 K06、K07 是第一轮基础规则；本轮新增 K08（排产→派工）、K09（领料→消耗）、K10（报工→生产报告）、K11（IPQC）、K12（入库→完工/追溯）和 K13（工单/批次/SN 追溯）。每张卡均包含：调用 API、主键、实际读写表、生命周期推进、下游连接及“必须改走实时工具”的边界。

`database-landscape.md` 中的工程测试/数据现状说明不再向 `PROD_SUPERVISOR` 暴露；该来源仅保留给 `MANAGER` 与 `TESTER` 的内部系统说明检索，避免业务回答引用测试数据。

以下资料确实服务业务，但目前不应直接把整张表做成 RAG。先由 MES 提供只读、按版本/权限导出的知识快照；没有版本、生效时间和责任人的数据不能发布到知识库。

| 知识快照 | 实际业务源 | 可回答的问题 | 发布条件 | 动态事实仍走哪里 |
|---|---|---|---|---|
| 产品与物料说明 | `product`、`material`（以及 Compat 主数据） | “FS40-A 使用哪些关键物料？” | 产品/物料编码、启停状态、负责人、版本齐全 | 当前库存、批次、可用量 → 仓储 API |
| BOM 说明 | `bom`、BOM 明细、`work_order_bom` | “该产品理论需要哪些物料、齐套分析依据是什么？” | BOM 版本/生效状态、产品范围、审核人齐全 | 某工单实际预留/缺料 → Kitting API |
| 工艺路线与工序说明 | `process_route`、`route_step`、`process_step` / Compat 路线表 | “该产品要经过哪些工序、为何这样排产？” | 路线版本、生效时间、产品范围、工艺负责人齐全 | 当前任务/工位/排产时间 → ProTask API |
| 检验模板与标准 | `qc_template`、`qc_template_index`、产品关联 | “该产品 IPQC 检什么、标准是什么？” | 模板启用状态、产品/工序范围、质量负责人齐全 | 本次检验结论 → IPQC API |
| 设备点检/保养规范 | `dv_*` 设备主数据及后续上传手册 | “设备异常时先检查什么？” | 设备类型、适用产线、版本、设备负责人齐全 | 当前设备状态/维修单 → 设备 API |

### 2.4 不进入正式 RAG、但必须保留的真实资料

| 来源 | 用途 | 处理方式 |
|---|---|---|
| `docs/seed-data.md` 与 `R__seed_story_*.sql` | 构造真实业务评测场景，例如 FS40-A、WO-GP-*、欠料、IPQC、追溯路径 | 提取并脱敏为评测题与期望事实；不将 SQL、账号、密码、连接信息写入 RAG |
| `scripts/workflow-golden-path-*.ps1` | 验证 Agent 是否正确读取和解释黄金链 | 作为回归测试，不作检索语料 |
| 数据库迁移 SQL | 追溯系统规则和生成派生知识卡 | 作为来源证据，原文不向普通业务用户检索 |
| `docs/data-gaps.md`、开发协作文档、Agent 方案文档 | 研发计划、缺口和架构决策 | 仅可作为研发/管理员内部知识，默认不进入业务助手语料 |
| `agent_session`、`agent_message`、用户对话 | 会话和审计 | 进入用户隔离的 Memory / 审计库；禁止写回共享知识库 |

## 3. 首批知识包与真实问题覆盖

第一轮只发布以下 5 个知识包。每一个知识包都必须能用当前系统的真实业务路径验证，而不是只验证“检索到了类似文本”。

| 知识包 | 来源 ID | 真实业务问题示例 | 需要联动的实时事实 |
|---|---|---|---|
| P0-01 黄金生产流程 | K01、K02、K06 | “工单从订单到入库要走哪些步骤？现在下一步是什么？” | `get_workflow_pipeline` |
| P0-02 齐套与领料 | K02、K07、K09 | “齐套不足怎么处理？为什么这张工单不能领料？” | `get_kitting_analysis`、领料单/库存 API |
| P0-03 排产与派工 | K01、K02、K08 | “排产保存后为什么没看到派工？应该找谁处理？” | `get_work_order_tasks`、派工结果 |
| P0-04 报工、IPQC 与入库 | K02、K06、K10、K11、K12 | “为什么报工后还不能入库？质检不通过怎么办？” | 报工、IPQC、入库、流程快照 API |
| P0-05 工单与批次追溯 | K01、K02、K13 | “某工单用了哪些批次，已产生哪些 SN？” | `get_work_order_trace` |

## 4. 必填元数据规范

### 4.1 `rag_document`（文档级）

| 字段 | 必填 | 说明 |
|---|---:|---|
| `document_id` | 是 | 稳定 ID，例如 `RAG-WF-GOLDEN-001` |
| `title`、`knowledge_type`、`domain` | 是 | 标题；SOP/rule/manual/master_data_snapshot；业务域 |
| `source_type`、`source_path`、`source_hash` | 是 | Markdown / PDF / 派生规则；可复查来源与内容哈希 |
| `authority_level` | 是 | `business_approved`、`system_derived`、`draft`、`retired` |
| `owner`、`reviewer`、`review_status` | 是 | 业务责任人与审核状态；仅 `approved` 可检索 |
| `version`、`effective_from`、`effective_to` | 是 | 版本与生效范围，失效后自动排除 |
| `role_scope`、`factory_scope` | 是 | 可见角色与工厂/组织范围 |
| `product_codes`、`material_codes`、`process_codes`、`lifecycle_states` | 按需 | 用于混合问题的结构化过滤 |
| `language`、`created_at`、`updated_at` | 是 | 语言及审计时间 |

### 4.2 `rag_chunk`（片段级）

| 字段 | 必填 | 说明 |
|---|---:|---|
| `chunk_id`、`document_id`、`chunk_index` | 是 | 片段唯一性与回链 |
| `heading_path`、`anchor`、`page_number` | 是 | 回答引用必须能定位到章节/页码/行锚点 |
| `content`、`content_hash`、`token_count` | 是 | 可重建、可去重的正文 |
| `step`、`role_scope`、`domain` | 是 | 适配十步、角色和领域过滤 |
| `product_codes`、`process_codes`、`lifecycle_states` | 按需 | 精准检索过滤 |
| `embedding_model`、`embedding_version`、`index_status` | 是 | 支持重建、灰度与回滚 |

## 5. 入库与更新流程

```text
业务负责人提交 / 系统规则自动派生
  → 文档校验（敏感信息、版本、责任人、范围）
  → 审核通过
  → 解析与结构保留切分
  → 生成向量与关键词索引
  → 检索评测通过
  → 发布为 approved
  → 版本过期或规则变更时撤回 / 重建
```

- `K01`–`K05` 初次可人工审核并入库；`K06`–`K13` 必须在代码/迁移变更后重新派生、审核和发布。
- 不允许模型自行写入 `rag_document`、`rag_chunk` 或共享制度知识。用户纠错先创建“待审核反馈”，由责任人处理。
- 动态主数据快照采用“按版本发布、按版本撤回”；任何快照命中后，涉及具体工单的答案仍须二次调用实时工具。

## 6. 第一批验收题（使用真实项目业务语义）

| 编号 | 问题 | 预期知识来源 | 必须验证的实时工具 / 结果 |
|---|---|---|---|
| E01 | “订单到成品入库完整要走哪十步？” | K01、K02 | 无；回答必须引用步骤和角色 |
| E02 | “工单处于 QC_PENDING，下一步谁来处理？” | K02、K03、K06 | `get_workflow_pipeline`；应指向质检员/IPQC |
| E03 | “为什么 QC_FAILED 不能直接入库？” | K06、K11、K12 | 流程快照；不得建议绕过质量门禁 |
| E04 | “齐套不足时我可以直接发布排产吗？” | K02、K07 | `get_kitting_analysis`；结果应包含缺料事实与规则依据 |
| E05 | “排产任务保存后没有派工，可能是什么原因？” | K08 | 任务/派工工具；需说明 `auto-dispatch` 与实际派工结果区别 |
| E06 | “WO-GP-20260714 的领料、报工、质检和入库记录是什么？” | K13 | `get_work_order_trace`；禁止用种子文本冒充实时结果 |
| E07 | “FS40-A 的 IPQC 要检查什么？” | P1 检验模板快照 | 检验模板实时/受控快照；若未发布应明确说明暂不可依据回答 |

## 7. 当前缺口与下一步

当前仓库中已经具备流程和系统规则类知识，但尚未发现经业务签字/版本管理的正式工艺 SOP、产品作业指导书、设备维修手册或质量标准原文。第一轮先以 K01–K13 构建可追溯的“系统已实现规则知识”，同时向生产、质量、仓储、设备负责人收集并审核这些正式资料。

下一步实施顺序：

1. 创建 `rag_document` / `rag_chunk` 的元数据迁移和本地向量索引适配层。
2. 生成 K01、K02、K03、K06 的首批规范化 Markdown，完成脱敏扫描与人工审核。
3. 建立 `search_knowledge` 工具、引用返回格式和 E01–E06 自动评测。
4. 再将其接入 LangGraph 的 `knowledge_router`；与实时 MES 工具组合回答混合问题。

## 7.1 已补充的 Agent 运行知识（2026-07-16）

近期落地的 Agent 能力已整理为 6 份受控 Markdown，并纳入初始入库清单。它们不是模型对话或代码原文，而是根据当前真实接口和运行边界整理的可回链知识卡。

| ID | 知识卡 | 解决的问题 | 动态数据边界 |
|---|---|---|---|
| K14 | `AGENT_RUNTIME_GOVERNANCE.md` | RAG、实时事实、模型建议和写入权限如何分工 | 不收录订单、库存、会话或密钥 |
| K15 | `INTELLIGENT_SCHEDULING_AGENT.md` | 最新订单确认、九节点排产责任、写入与门禁 | 实际订单、BOM、库存、任务和派工必须实时读取 |
| K16 | `QUALITY_ANALYSIS_AGENT.md` | 今日质量分析的五段式流程、本地/外部模型边界 | 待检、合格率、缺陷和趋势必须实时读取 |
| K17 | `QUALITY_REPORT_AND_EXPORT.md` | 图文报告、可选模型补充与 Word 导出机制 | 图表、表格和报告快照仅用于会话/审计，不进共享 RAG |
| K18 | `BUSINESS_SUBAGENTS.md` | 16 个子 Agent 的接入状态、只读资源和未来能力边界 | Agent 包装层每次经 JWT 读取当前事实 |
| K19 | `REALTIME_DATA_AND_AGENT_CONTRACTS.md` | P0 工具、受控写入动作和 LangGraph 节点合同 | 交易表不允许被 Agent 或 RAG 直接写入 |

这些知识卡统一标注为 `system_derived`，仅用于解释已落地能力和指导工具路由；它们不能替代领域服务的实时校验。任何接口、状态机、数据主线变化后，必须同步更新知识卡并重新入库、执行检索回归。

## 8. 向量库选型决定

### 8.1 结论：第一版使用自托管 Qdrant

| 项目现状 | 对选型的影响 | Qdrant 的对应能力 |
|---|---|---|
| FastAPI + LangGraph 独立于 Spring Boot 运行 | 需要 Python 侧调用简单、可独立演进的检索服务 | 官方 Python 客户端、REST / gRPC 服务接口 |
| 业务库为 MySQL `fan_mes` | 不能把向量索引与生产交易库耦合，也不值得额外引入 PostgreSQL | 独立向量服务；MySQL 仅保存文档治理和审计元数据 |
| RAG 必须按角色、工厂、产品、工序、版本、生效期过滤 | 不能只做“相似文本 Top-K” | JSON payload 过滤与 payload index，支持 `must` / `should` / `must_not` 组合 |
| MES 术语、编码、状态名很重要 | 需要语义召回与关键词精确匹配并用 | 支持 dense / sparse 向量及混合检索，可后续加入重排 |
| 当前为本地单机 MVP，后续可能扩展 | 需要低门槛启动，保留独立扩容路径 | 本地 Docker 服务即可启动；数据卷持久化，后续可扩展部署 |

第一版部署为 **仅本机可访问的独立服务**，不暴露到公网：Qdrant REST `127.0.0.1:6333`，数据卷位于 `agent-system/data/qdrant/`；FastAPI 的 RAG 适配层是唯一调用方。MySQL 不存 embedding，只存 `rag_document`、`rag_chunk`、审核、版本和审计记录；Qdrant 存 `chunk_id`、向量和可检索 payload。

### 8.2 首个 Collection 设计

```text
collection: mes_knowledge_v1
point_id: chunk_id（与 rag_chunk.chunk_id 一致）
payload: document_id, domain, knowledge_type, authority_level, review_status,
         role_scope, factory_scope, product_codes, material_codes, process_codes,
         lifecycle_states, version, effective_from, effective_to, source_path, anchor
vectors: dense_vector（第一版 512 维）；预留 sparse_vector（混合检索）
```

建库即创建 payload index：`review_status`、`authority_level`、`domain`、`role_scope`、`factory_scope`、`product_codes`、`process_codes`、`lifecycle_states`、`effective_to`。第一版使用轻量中文 Dense Embedding（512 维）保障本地可稳定导入；检索请求必须固定携带 `review_status=approved`，再叠加当前 JWT 的角色/组织范围和问题中解析出的产品、工序或生命周期过滤。

### 8.3 不选其他方案的原因

| 方案 | 本阶段结论 | 适用条件 |
|---|---|---|
| **Qdrant** | **选择** | 本地 MES RAG、元数据权限过滤、可逐步启用混合检索的最佳平衡 |
| Chroma / 内存向量库 | 不作为正式库 | 仅用于单元测试或临时检索实验，不用于持久化业务知识 |
| pgvector | 暂不选择 | 只有当项目明确引入并长期运维 PostgreSQL 时才有意义；当前业务库是 MySQL，引入 PostgreSQL 只为向量会增加运维面 |
| Milvus | 暂不选择 | 它适合大规模、高吞吐、多租户向量集群；当前知识量和单机部署阶段会过重，未来向量规模、并发或物理隔离需求显著增长时再评估 |
| MySQL 业务库 | 不选择 | 继续作为交易事实与元数据权威库，不承担向量检索和 RAG 召回 |

### 8.4 上线门禁

1. Qdrant 仅绑定回环地址，并设置服务访问密钥；生产环境由反向代理/TLS 和网络策略保护。
2. Qdrant 与 MySQL 均需备份：前者备份 collection snapshot，后者备份元数据与发布记录；恢复后按 `content_hash` 校验一致性。
3. 先以 dense 检索 + 元数据过滤上线；E01–E06 的命中、引用、权限、拒答测试通过后，才评估 sparse/hybrid 和 reranker。
4. 任何检索结果都必须回链 `source_path`、`anchor`、`version`；没有有效引用的内容不得作为业务规则答案输出。
