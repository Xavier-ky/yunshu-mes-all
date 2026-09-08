# Task Plan: 最小排产全链路打通

## Goal

在不重构现有 Yunshu UI 兼容层的前提下，建立并验证一条可审计的最小生产闭环：订单/工单 → 齐套锁料 → 排产任务 → 派工 → 报工 → 质量放行 → 完工 → 成品入库与库存流水。

## Current Phase
Phase 8.10 — Quality report continuation workflow implementation (complete)

Phase 8.9 — Quality-management Agent workflow implementation (complete)

Phase 6 — LangGraph sub-agent planning for the MES golden path (complete)

Phase 1 — 计划与范围确认

## Phases

### Phase 6: LangGraph sub-agent planning (golden-path driven)
- [x] Read MES-GOLDEN-PATH.md; set scheduling steps 1–6 as the first delivery scope
- [x] Review current agent-system workflow, registry, and tool skeleton
- [x] Define core and reserved sub-agents, tool boundaries, and approval levels
- [x] Deliver the plan under agent-system/
- **Status:** complete

### Phase 1: 链路基线与验收口径

- [x] 识别当前两套接口与共享表的边界
- [x] 定义最小闭环及明确排除项
- [x] 识别当前断链和数据一致性风险
- **Status:** complete

### Phase 2: 数据与状态契约

- [ ] 为任务、派工、报工、质检、入库确定唯一 ID 语义
- [ ] 增加派工拆分、物料预留明细、入库库存流水所需迁移
- [ ] 定义所有状态转换与阻断校验
- **Status:** pending

### Phase 3: 后端事务闭环

- [ ] 实现任务确认后生成/维护派工
- [ ] 实现报工原子回写派工、任务、工单进度
- [ ] 实现质量放行与完工前校验
- [ ] 实现成品入库、库存余额与库存流水原子更新
- **Status:** pending

### Phase 4: 前端最小操作面

- [ ] 在排产页提供“确认并生成派工”动作和状态反馈
- [ ] 在操作工页仅对派工单报工，并展示真实剩余量
- [ ] 在完工/入库页显示阻断原因和可执行下一步
- **Status:** pending

### Phase 5: 数据迁移、种子与全链路验收

- [ ] 准备一套订单到成品入库的可重复种子数据
- [ ] 添加服务级集成测试与 JWT API 冒烟脚本
- [ ] 按验收场景验证正常、欠料、不良、取消四条路径
- **Status:** pending

## Key Questions

### Phase 7: 基于实现核对完善 LangGraph Agent 设计

- [x] 对照十步黄金流程、Controller、生命周期服务和迁移 SQL，确认真实业务主键、写回与门禁。
- [x] 区分现有可复用的只读接口、现有兼容写接口与需要新增的受控 Agent 工具门面。
- [x] 将实现级映射、工具边界、异常分支和分期改造补充到 `agent-system/LANGGRAPH_SUBAGENT_PLAN.md`。
- **Status:** complete

### Phase 8: LangGraph Agent 真实数据闭环（下一实施主线）

- [x] 阶段 8.1a：盘点真实十步业务链的第一批 RAG 知识源、排除动态事实与敏感资料，并定义文档/片段元数据。
- [x] 阶段 8.1a-1：完成向量库选型：第一版自托管 Qdrant，MySQL 保留为交易事实与 RAG 元数据权威库。
- [x] 阶段 8.1b：建立可治理的 RAG MVP（元数据迁移、解析切分、索引、检索与引用评测）；Qdrant 服务、`mes_knowledge_v1` Collection、首批真实业务知识、JWT 检索接口与端到端验证均已完成。
- [x] 阶段 8.1c：根据真实 Controller、Service、Repository 与迁移 SQL，补齐排产→派工→领料→报工→IPQC→入库→追溯的系统派生知识卡，并重新导入与评测。
- [x] 阶段 8.2：建立 P0 Agent 持久化基座：将 Qdrant 知识映射到既有 `knowledge_*` 审计表，初始化排产 Agent/只读工具权限/策略守卫/评测用例，并为 LangGraph 审批中断建立 checkpoint 持久化。
- [x] 阶段 8.2b：以 JWT 打通 Agent 到 MES 的真实只读工具（工单流程快照、齐套、任务、追溯）。
  - **Status:** complete — implemented a dedicated side-effect-free Spring Boot facade, FastAPI JWT-forwarding allowlist and schema-validated tool gateway. Factory scope is explicitly deferred because the current work-order model has no factory ownership field.
- [x] 阶段 8.2c：主对话持久化：将“云枢小智”的用户问题与 Agent 回复写入用户隔离的 `agent_session` / `agent_message`，并提供历史列表与按会话回读接口。
  - **Status:** complete — JWT identity is authoritative; browser refresh can restore persisted sessions.
- [x] 阶段 8.3a：智能排产工作流节点 1（订单接收）：以“我有一个新订单，需要排产。”触发 OrderIntakeAgent，展示真实产品下拉表单，并经显式确认写入 CREATED 订单。
  - **Status:** complete — bounded Agent write gateway, real product-master validation, user confirmation, persisted MES order, and regression verification are in place.
- [ ] 阶段 8.3：将现有硬编码 `workflow.py` 替换为最小 LangGraph `StateGraph`，实现知识 / 实时工具 / 混合问题路由。
- [ ] 阶段 8.4：为状态、工具调用、引用数据与流式事件建立可观测性和评测，再完成排产草稿、审批中断与受控写入门面。
- **Status:** in_progress

1. 是否以现有 `production_task → dispatch_task` 作为唯一执行模型？（计划：是）
2. 质量放行采用现有质量记录/放行表还是新增最小的工单级放行凭据？
3. 成品库存使用现有 `wm_material_stock` 作为唯一余额账，还是先继续维护独立成品入库表？（计划：前者）

### Phase 8.3b: Autonomous kitting-to-Gantt scheduling workflow
- [x] Verify production write contracts and lifecycle preconditions
- [x] Add controlled Agent writes for kitting reservation and scheduling execution
- [x] Orchestrate readiness, capacity, advisory checks, plan, execute and validation
- [x] Verify real database writes with a cleanup-safe end-to-end test
- [x] Surface the workflow result in the main Agent conversation
- **Status:** complete

## Decisions Made

### Phase 8.3c: Autonomous production-material issue workflow
- [x] Add a controlled ProductionIssueAgent transaction boundary using the existing WMS issue documents
- [x] Chain successful SCHEDULED work orders into real pick allocation, outbound execution and lifecycle advancement
- [x] Reconcile work-order BOM, warehouse stock, inventory batch, transaction and consumption records
- [x] Verify a full order-to-material-issued run against MySQL and clean up all generated test records
- **Status:** complete

### Phase 8.3d: Observable conversational scheduling Agent workflow
- [x] Expand natural-language scheduling intent routing beyond one fixed phrase
- [ ] Persist a resumable graph state that pauses for the order form and resumes after confirmation
- [x] Emit an ordered event for every scheduling sub-agent, including API evidence, summary, decision and failure state
- [x] Render sub-agent event cards with distinct names and avatars in the Agent workflow side panel
- [x] Render the same ordered sub-agent events as slow, streamed main-chat cards; keep the workflow side panel optional and closed by default
- [x] Persist each visible sub-agent card and final orchestration summary in the user-owned main-chat session, and restore its Agent identity/evidence from history
- [x] Add bounded external-model analysis at intent recognition and final scheduling summary, with API-source audit, prompt versioning and deterministic fallback
- [x] Ground intent recognition and final scheduling explanation with role-scoped Qdrant SOP retrieval; keep retrieved knowledge read-only and non-authoritative for MES writes
- [ ] Verify normal, shortage and blocked-issue paths remain truthful and recoverable
- **Status:** in_progress — visible event playback and normal-path verification are complete; server-sent per-node runtime events, LangGraph checkpoint resume and negative-path tests remain.

### Phase 8.3e: Scheduling sub-Agent packaging without business behavior changes

- [x] Define a shared scheduling Agent state/result contract and a non-writing base wrapper.
- [x] Wrap the current intent, order intake, BOM/route, kitting risk, lock, scheduling, dispatch, validation and production issue steps as individual Agent modules.
- [x] Replace only the internal orchestration calls with the wrappers; preserve existing API paths, database writes, event payloads and execution order.
- [x] Register active scheduling Agent definitions and leave extension points for future Agents.
- [x] Run compilation and the cleanup-safe real order-to-material-issued regression.
- **Status:** complete

### Phase 8.5: Full MES business-aligned sub-Agent inventory

- [x] Inspect actual backend modules, controllers/services, frontend routes and data boundaries outside the scheduling flow.
- [x] Classify candidate sub-Agents as immediately packageable, dependent on a new controlled tool, or reserved because the business capability is incomplete.
- [x] Map each candidate to its real inputs, writes, lifecycle gates, RAG role and upstream/downstream business link.
- [x] Deliver the complete prioritized Agent inventory under `agent-system/` for later incremental implementation.
- **Status:** complete — catalog delivered in `agent-system/MES_FULL_AGENT_CATALOG.md`; no new business writes were introduced in this planning pass.

### Phase 8.6: Independent packaging for the next MES business Agents

- [x] Create a shared, non-executing business-Agent contract separate from the active scheduling state.
- [x] Package production execution, quality management, warehouse logistics, traceability, equipment maintenance, Andon response and operations insight as standalone Agent modules.
- [x] Register only their truthful current capability contracts; do not expose unavailable tools or connect them to the main workflow.
- [x] Add extension documentation and run compile/import verification.
- **Status:** complete — all seven packages completed a JWT-backed read-only verification against live MES data; no default workflow or business write changed.

### Phase 8.7: Agent overview UI

- [x] Replace the left-sidebar third tool label with Agent overview and define the 16 truthful Agent display records.
- [x] Add a right-side 4×4 Agent overview drawer with distinct companion avatars, status disclosure and responsive layout.
- [x] Keep Agent overview and execution trace drawers mutually exclusive; extend future message-avatar mappings for the seven new Agent packages.
- [x] Run the frontend production build and inspect for integration issues.
- **Status:** complete — `npm run build` passed; no workflow, API or business-data behavior changed.

### Phase 8.8: Quality-management Agent workflow plan

- [x] Map the real quality workbenches, lifecycle gates, available Agent data snapshots and user-role boundaries.
- [x] Define the quality workflow stages, the precise division between local Qwen analysis and external-model intent/final-summary analysis, and the streamed event contract.
- [x] Define the first safe read-only rollout, future approval-gated write points, failure fallbacks and evaluation cases.
- [x] Publish an implementation-ready quality workflow plan under `agent-system/` without connecting it to the main workflow yet.
- **Status:** complete — implementation plan delivered in `agent-system/QUALITY_AGENT_WORKFLOW_PLAN.md`; no runtime behavior changed.

### Phase 8.11: Agent runtime knowledge expansion for RAG

- [x] Audit the recently implemented scheduling, quality-analysis, quality-report and independently packaged business-Agent contracts against their actual code and MES data sources.
- [x] Author approved, non-temporal RAG knowledge cards covering Agent responsibilities, call sequence, real database/API evidence, lifecycle gates, permissions, fallbacks and explicit exclusions.
- [x] Register the curated cards as governed Qdrant sources and ingest them without replacing unrelated approved knowledge.
- [x] Verify point growth, role-filtered retrieval, citations and representative scheduling/quality/traceability queries.
- **Status:** complete — six runtime cards added as 40 approved chunks; collection verified green at 170 points and retrieval regressions passed.

### Phase 8.12: Professional Agent orchestration core (compatibility-first)

- [x] Add an optional LangGraph orchestration core with explicit state, node contracts, deterministic routing and no direct MES table access.
- [x] Persist owner-scoped checkpoints and run traces using the existing Agent platform tables; support a safe approval pause/resume contract.
- [x] Build an adapter that can observe and normalize the existing scheduling workflow without changing its node order, Spring Boot APIs, write transactions or SSE event schema.
- [x] Add offline/cleanup-safe verification for normal completion, pause/resume, policy denial and audit retrieval.
- [x] Keep this core opt-in until it proves compatible; the existing scheduling and quality entry points remain the production path.
- **Status:** complete — the LangGraph sidecar is verified and intentionally not imported by current scheduling/quality routes.

### Phase 8.13: LangGraph-owned intelligent scheduling execution

- [x] Build one real `StateGraph` whose nodes delegate to the existing nine scheduling Agent wrappers in their current order.
- [x] Preserve the existing LOW-risk gate and blocked path as graph conditional edges; do not duplicate the atomic scheduling or material-issue writes.
- [x] Persist user-owned, serializable progress checkpoints after each graph node using the existing Agent platform storage.
- [x] Switch only the existing confirmed-order execution entry point to the graph while preserving its API request/response contract, conversation events and Spring Boot business boundaries.
- [x] Run the cleanup-safe real order-to-material-issued regression and verify graph audit/checkpoint evidence.
- **Status:** complete — the confirmed scheduling execution path is now LangGraph-owned and passed the live cleanup-safe regression with 9 checkpoints.

### Phase 8.14: Professional Agent foundation extensions (sidecar-only)

- [x] Add owner-scoped short-term and long-term Agent memory abstractions on the existing Agent runtime-state store; do not change chat prompts or MES business writes.
- [x] Add a governed, descriptive tool catalogue that maps real MES/RAG capabilities without exposing new execution permissions.
- [x] Add fact-only post-run reflection/evaluation records for completed Agent runs; never expose model chain-of-thought.
- [x] Reuse the existing sidecar approval contract as the documented future human-in-loop bridge; do not route current user confirmations through it yet.
- [x] Add isolated verification and update orchestration documentation/RAG knowledge, then compile and run compatibility checks.
- **Status:** complete — foundation verifier passed against the real Agent platform with `business_writes: 0`; RAG collection is green at 176 points.

### Phase 8.15: Unified real read-tool packaging (compatibility-first)

- [x] Package the existing JWT-forwarding MES read gateway and governed RAG retrieval behind one typed, read-only Tool contract.
- [x] Expose a separate foundation Tool API for listing/invoking only the packaged read tools; do not import it into scheduling or quality workflows.
- [x] Explicitly exclude the legacy mock `app.tools.registry` and every controlled-write business action from this package.
- [x] Verify the wrappers against live MES read APIs with an authenticated production-supervisor token and prove no kitting rows are created.
- [x] Document the wrapper location, tool-to-source mapping, safety boundary and future controlled-write extension path.
- **Status:** complete — six real read-only tools are wrapped; five live MES calls passed with unchanged kitting rows and `business_writes: 0`.

### Phase 8.16: Main-chat Agent workflow role-parity diagnosis

- [x] Compare production-supervisor and tester JWT claims across the scheduling and quality workflow entry dependencies.
- [x] Confirm whether the failure occurs in frontend routing, FastAPI authorization, Spring Boot read authorization, or the Agent persistence layer.
- [x] Apply only a narrowly scoped, read/role correction if the evidence identifies one; preserve workflow order, SSE schema and MES write behavior.
- **Status:** complete — production supervisor passed every workflow dependency; the AgentHome route declaration now truthfully includes `PROD_SUPERVISOR` without changing any workflow or business permission.

| Decision | Rationale |
|----------|-----------|
| 以 `dispatch_task` 作为现场执行唯一对象 | 操作工、工序、工位、人员和实际时间均应绑定派工。 |
| 保留 `/api/mes/pro/*` 兼容接口，但由其调用统一领域服务 | 避免破坏已迁移 Yunshu UI，同时阻断两套写逻辑继续分叉。 |
| 用事务维护数量、状态和库存流水 | 防止部分成功导致工单、库存和追溯数据不一致。 |

## Errors Encountered

| Error | Attempt | Resolution |
|-------|---------|------------|
| 未发现既有规划文件 | 1 | 已在项目根目录初始化本计划与发现、进度文件。 |

## GitHub full upload — 2026-09-08
- Target: Xavier-ky/yunshu-mes-all
- Inventory complete: 80,485 files, 12,191,528,127 bytes excluding Git internals.
- Upload and remote verification pending. Windows schannel failed; per-command OpenSSL access succeeded.


### GitHub upload result — complete
- Uploaded complete project excluding dependencies, caches, build outputs and incomplete downloads, as confirmed by the user.
- 3,176 files; 6 LFS assets (about 3.1 GB); sanitized three upload-copy configuration files while preserving local originals.
- Target main commit 07b6f4fc8da9c67c1a6cb9e03d934da5453d79a8 verified against GitHub. Git LFS integrity check passed.


### Team clone verification — complete (2026-09-08)
- Independent GitHub clone tree matches uploaded source; all six LFS files downloaded and fsck passed.
- npm ci and frontend build passed; UReport3 and backend package passed; 94 Python syntax checks passed.
- Existing frontend tests: 66/71 passed, five failures retained and documented. Clean-database and end-to-end business validation not performed.
- Repaired upload-only sanitization of two numeric token limits; local secrets unchanged. Added docs/team-clone-check.md.

