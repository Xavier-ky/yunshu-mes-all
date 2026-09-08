# 云枢小智 — 多 Agent 协同子系统

独立于 MES Spring Boot 主后端的 FastAPI + LangGraph（MVP 骨架）服务。

## 目录

- `backend/` — FastAPI 服务（默认 `:8090`）
- 前端集成在 `frontend/web-admin/src/views/yunshu-ai/`

## 快速启动

```bash
cd agent-system/backend
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
copy .env.example .env
uvicorn app.main:app --host 127.0.0.1 --port 8090 --reload
```

MES 前端通过 Vite 代理 `/yunshu-agent` → `http://127.0.0.1:8090/api`。

## RAG 向量库（Qdrant）

业务知识使用本机 Qdrant，MySQL `fan_mes` 继续只负责 MES 交易事实和 RAG 元数据。Qdrant 仅绑定 `127.0.0.1`，不对局域网或公网开放。

```powershell
cd D:\ClaudeCode\MES
docker compose -f agent-system/docker-compose.rag.yml up -d

cd agent-system\backend
python -m pip install -r requirements.txt
python scripts\init_qdrant_collection.py --verify
```

- 管理界面：`http://127.0.0.1:6333/dashboard`
- REST API：`http://127.0.0.1:6333`
- Collection：`mes_knowledge_v1`（512 维、余弦相似度）
- 本地持久化目录：`agent-system/data/qdrant/`（已忽略，不提交到版本库）

初始化脚本会创建角色、工厂、产品、工序、生命周期、审核状态等检索索引，并进行一次不保留数据的写入/检索/删除验证。实际知识入库前，请先遵循 [`RAG_KNOWLEDGE_CATALOG.md`](RAG_KNOWLEDGE_CATALOG.md) 的来源、版本、权限与引用规范。

首批真实业务知识导入与检索验证：

```powershell
cd D:\ClaudeCode\MES\agent-system\backend
D:\Anaconda2024.10\envs\pytorch\python.exe scripts\ingest_initial_knowledge.py
D:\Anaconda2024.10\envs\pytorch\python.exe scripts\query_knowledge.py "为什么 QC_FAILED 不能直接入库？"
D:\Anaconda2024.10\envs\pytorch\python.exe scripts\evaluate_knowledge.py
```

FastAPI 服务启动后，可通过受 MES JWT 保护的接口访问知识库：

- `GET /api/rag/health`：查看 Collection 是否可用及已入库数量。
- `POST /api/rag/search`：检索已审核的业务知识；角色范围由登录 JWT 自动注入，前端不能传入或伪造角色。

请求体示例：

```json
{
  "query": "为什么 QC_FAILED 不能直接入库？",
  "lifecycle_state": "QC_FAILED",
  "limit": 5
}
```

返回结果包含 `content`、相似度 `score` 和可追溯的 `citation`。它只回答版本化的 SOP/规则；今日产量、库存、工单状态等实时事实仍必须通过后续的 MES 只读工具查询。

`evaluate_knowledge.py` 是当前的 RAG 回归集：覆盖排产派工、领料消耗、报工任务关联、IPQC、成品入库和工单批次/SN 追溯。补充或替换知识源后，应先重新导入再执行该脚本，所有预期规则卡必须在默认 Top-5 中命中。

首次导入会下载中文 Embedding 模型 `BAAI/bge-small-zh-v1.5` 到 `agent-system/mymodel/models/` 缓存。导入的首批资料严格限定为已盘点的十步流程、接口/生命周期规则、角色职责、业务主链和 Compat 主线说明；工单、库存、质检等实时数据不会被导入。

## 陪伴式小精灵

管理端登录后的各业务页面右下角会挂载 `SpiritCompanion`。点击后，它将当前路由和经过格式校验的可选业务标识发送到 `POST /api/companion/suggest`，由本服务生成页面级提示；点击“进入小智”仍会进入完整 AI 工作台。

启动小精灵服务时，请使用与 MES 前端代理一致的地址：

```bash
python -m uvicorn app.main:app --host 127.0.0.1 --port 8090
```

陪伴接口的安全边界：

- 身份只从 MES `Authorization: Bearer <JWT>` 获取，不接受前端提交的用户或角色字段。
- `SpringBootClient` 是唯一的 MES 数据入口，仅允许读取仪表盘、工单、库存、安灯、设备和质量六类已登记资源。
- 适配层没有 SQL、通用 URL 调用或写入方法；未登记资源和非 `GET` 请求会被拒绝。
- MES 服务短暂不可用且 `ENABLE_MOCK_TOOLS=true` 时，响应会明确标记 `source: "mock"`，前端不会将其表述为实时结论。

## 环境变量

见 `backend/.env.example`。第一版默认 `ENABLE_MOCK_TOOLS=true`，无需 API Key。

## MVP 能力

- MainAgent + 9 子 Agent 注册与权限模型
- Mock 查询/写入工具 + 确认流程
- Trace 记录
- 4 类典型任务场景（延期分析、安灯创建、系统总览、质量分析）
- 陪伴式小精灵：受 JWT 保护的页面提示、只读 MES 数据适配与可辨识 Mock 回退
