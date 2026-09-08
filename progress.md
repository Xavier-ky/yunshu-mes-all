# Progress Log

## 2026-07-16 — Main-chat Agent role-parity diagnosis

- Started a read-only comparison of the production-supervisor and tester JWT paths for the two preset Agent workflows.
- Scope is limited to authorization/routing diagnosis first; no scheduling, quality, streaming, or MES write behavior has been changed.
- Verified live JWTs for `supervisor`, `supervisor01`, and `tester`: production supervisor can load the real product list, latest order, and quality overview through the running Agent/Spring APIs.
- Verified the pure scheduling intent route and the first three quality workflow nodes for `PROD_SUPERVISOR` and `TESTER`; both reach identical expected paths. The probes create no MES business writes or chat history.
- Corrected only `AgentHome` route metadata to include `PROD_SUPERVISOR`, matching the existing navigation and the already-allowed backend contracts. No API, workflow order, SSE event, model call, or database-write behavior changed.
- Ran the existing cleanup-safe full scheduling verifier as `supervisor`: it completed order confirmation, work-order creation, kitting/lock, Gantt scheduling, dispatch and material issue; verified 8 workflow events, 9 LangGraph checkpoints, two DeepSeek analyses, RAG citations, persisted chat readback and MES readback. Its `finally` cleanup removed the test order/work order/conversation/checkpoints and restored inventory.

## 2026-07-16 — Phase 8.14 started

- User requested the remaining professional-Agent foundations be added without affecting existing business workflows.
- Scope fixed to sidecar-only runtime memory, governed tool catalogue, deterministic post-run reflection/evaluation, and documentation/verification. Existing scheduling graph, quality flow, API contracts, RAG retrieval, and MES writes remain untouched.
- Implemented `app.foundation` and the JWT-protected `/api/agent/foundation/*` read/reflect endpoints. The existing Agent platform store gained owner-scoped listing and exact maintenance cleanup helpers only; no MES table access was added.
- Corrected a local MySQL/Python timezone mismatch for short-memory TTL and the actual `runtime_state_id` schema name. The cleanup-safe foundation verifier passed against real Agent-platform storage with short memory, explicit long memory, checkpoint reflection and 12-tool catalogue checks; `business_writes: 0`.
- Added `RAG-AGENT-FOUNDATION-001` and re-ingested knowledge. Qdrant is green at 176 points. A separate semantic query process timed out during local embedding-model load after ingestion completed, so that one optional query was not treated as a RAG failure.

## 2026-07-16 — Phase 8.15 started

- User requested actual tools be packaged while preserving all current business workflows. The selected scope is a typed, read-only wrapper over the already verified real MES/RAG gateway; mock tools and all controlled-write actions are excluded.
- Implemented `app.foundation.tools` with a uniform Tool contract for six real read-only abilities, plus separate foundation listing/invocation endpoints. Current scheduling and quality workflows do not import the new package.
- Live verification passed against the real MES backend: five wrapped MES calls completed for `WO-20260701`; kitting/shortage rows stayed `42/49` before and after; `business_writes: 0`.

## 2026-07-16 — Professional Agent orchestration core

- Started a compatibility-first upgrade requested by the user. Scope is limited to an opt-in LangGraph orchestration layer, durable state/checkpoints, policy/audit contracts and tests; existing scheduling/quality routes, business APIs, transaction boundaries and workflow events remain unchanged.
- Confirmed the project has reusable `agent_graph_checkpoint` / `agent_runtime_state` persistence but does not yet have the `langgraph` package in the active runtime. Next: add the pinned dependency, implement the sidecar graph and verify without invoking production business writes.
- Installed `langgraph==0.2.76` in the project `pytorch` environment and added it to `requirements.txt`. Initial graph construction found a Python 3.9 `TypedDict` annotation incompatibility (PEP 604 optional unions); corrected the state schema to `Optional[...]` before retrying. No MES business API was invoked.
- Implemented and verified `SchedulingOrchestrationSidecar`: a real LangGraph StateGraph for plan → approval pause/resume → finalized delegation readiness, plus existing-workflow event observation. `verify_orchestration_sidecar.py` passed normal approval, denied approval and observation paths with `business_writes: 0`; temporary Agent-platform rows were cleaned in `finally`. Only the new sidecar package, its verifier and the pinned dependency changed—existing scheduling/quality workflow modules were not modified.
- Began Phase 8.13: one real LangGraph graph will now take ownership of confirmed intelligent-scheduling execution by delegating the existing wrappers in exactly their current order. The existing user confirmation, HTTP contract, Spring Boot writes and visible event payload remain the compatibility boundary.
- First end-to-end execution reached the graph audit assertion after completing the business chain, but the verifier queried the earlier intent trace rather than the execution graph thread. Correcting the verifier to use the returned graph thread ID and to clean those checkpoint rows before retrying.
- Tightened compatibility: unconfirmed form submissions are now rejected before graph creation exactly as before. The real confirmed request uses a verifier-owned execution trace, so the graph checkpoint thread can be asserted and removed precisely.
- Completed Phase 8.13. `SchedulingExecutionGraph` now owns confirmed intelligent-scheduling orchestration, delegates the unchanged wrapper/transaction chain through explicit nodes, and records a serializable checkpoint after every node. The live verifier passed: 11 real products available, unconfirmed request 422, work order reached `MATERIAL_ISSUED`, 4 production tasks/4 dispatches, finished issue document, original 8 workflow cards and 9 LangGraph checkpoints ending `COMPLETED`; all verifier data was cleaned.
- Synchronized the revised scheduling architecture into the governed RAG card and re-ingested it. Collection is green at 171 points; a supervisor query about LangGraph scheduling hit `RAG-AGENT-SCHED-001`. Final residual check confirmed zero verification orders, work orders, graph checkpoints and runtime rows. A PowerShell quote issue affected only the first read-only RAG query attempt and was corrected with an encoded retry.

## 2026-07-16 — RAG runtime knowledge expansion

- Read the active RAG ingestion, Qdrant filtering and retrieval implementation.
- Confirmed the existing corpus is governed by explicit source registration and approved-only retrieval; started a new Phase 8.11 to add recent Agent runtime knowledge without mixing in live transactional facts.
- Audited the active scheduling, quality and business-Agent contracts and authored six source-governed knowledge cards under `agent-system/knowledge/agent_runtime/`. Registered them as `RAG-AGENT-*` sources; Qdrant baseline is healthy with 130 points.
- Embedded the six cards in Qdrant using the project `pytorch` environment: 40 new chunks, collection now green at 170 points. Existing six workflow retrieval regressions and five new runtime-card checks passed; a role-isolation check confirmed `QUALITY_INSPECTOR` cannot retrieve the scheduling runtime card. Python syntax, whitespace validation and final collection health check passed.

## Session: 2026-07-16 — Quality report continuation workflow implementation

- **Status:** complete
- Delivered `agent-system/QUALITY_REPORT_AGENT_WORKFLOW_PLAN.md` and implemented its report route without changing the existing five-node quality-analysis route. Report phrasing is evaluated first, then uses the same authenticated main-chat session and a fresh live MES quality snapshot.
- Added a bounded Spring report snapshot (`summary`, `trend`, `defectTop`, severity, pending queue, recent finished, pending disposition), an Agent report-artifact store, visible report SSE events, a single ECharts report card with four charts/two tables, and a user-owned Word download flow.
- End-to-end live verification passed using the TESTER JWT: the new report route emitted `report_start`, 4 chart events, 2 table events, streamed report analysis, export-ready and done events; it saved report `QAR-BB070740527B408EB476` with real pending count 29/pass rate 86.5. Its Word export returned a valid 32,320-byte DOCX and the owned snapshot transitioned to `EXPORTED`.
- Spring Boot compile, FastAPI compilation and frontend production build passed. A duplicate Flyway migration version was discovered during startup and corrected from V5 to V33; the restarted backend and Agent service are both listening on 8080/8090.

## Session: 2026-07-16 — Quality-management Agent workflow implementation

- **Status:** complete
- Added a JWT-scoped, side-effect-free Spring quality fact facade and changed `QualityManagementAgent` to use the bounded overview / work-order resources instead of page-facing endpoints.
- Connected quality-intent routing to the main SSE chat without touching the scheduling route. The visible sequence is: external intent card → MES facts card → deterministic risk card → local Qwen3-0.6B streamed card → externally streamed final summary.
- Verification passed against live MES data: pending quality tasks 29 and pass rate 86.5%; local Qwen emitted 43 stream fragments; DeepSeek V4-Pro emitted 338 final-summary fragments; five workflow events and five assistant messages were persisted in the authenticated session.
- Follow-up streaming fix: every local/external node delta now carries its stable `node_id`; the browser consumes SSE callbacks sequentially and drains one card before starting the next. Regression verified 134 node deltas across exactly two node IDs, a 177-character fact card, local-Qwen output and a 625-character DeepSeek final summary.
- Follow-up empty-card fix: `node_start` is now trace-only in the main chat. The browser creates a visible sub-Agent card only after `node`, `node_delta`, or `node_complete` carries actual text, removing the long-lived blank MainAgent/typing card before an external model returns its first token. Frontend production build passed.
- Follow-up final-result fix: removed the unrelated generic “正在规划与路由” placeholder and taught the shared SSE consumer to render a factual `final_complete` fallback when no `final_delta` tokens arrive. This prevents a successful rules fallback from being misreported as “模型暂未返回有效内容”. Frontend production build passed.
- Interaction refinement: added a compact, animated thinking status strip below the current conversation instead of an empty assistant bubble. It appears immediately after send, reflects the current quality/report Agent preparation state, and fades away before the first real card, chart, or streaming character renders. Frontend production build passed.
- Report export follow-up: verified the actual Vite proxy export path returns a valid 32,953-byte DOCX for the latest READY report. Fixed history restoration so a persisted report with status `READY` or `EXPORTED` always shows the Word button even after refresh; exported reports offer “再次导出 Word”. Frontend production build passed.

## Session: 2026-07-15 – Observable conversational scheduling workflow

- **Status:** in_progress
- User wants the scheduling flow to feel like a real Agent: natural-language entry, a paused order form, then visible sub-agent cards in the expanded right-side workflow panel for every analysis, API/tool call, decision and controlled write.
- Existing base: real APIs and write transactions are complete, but the current main chat uses a deterministic router and only exposes aggregate trace steps. The next implementation should turn each sub-agent result into persisted, ordered workflow events with a dedicated visual identity.
- Scope clarification: live MES facts stay grounded in the authenticated MES APIs. Optional external model calls may summarize returned, scope-limited facts; they never decide quantities, write records, or replace MES evidence.
- Implemented the first visible workflow slice: a new scheduling-intent detector accepts several natural-language new-order phrases; `ChatResponse` carries an initial OrderIntakeAgent waiting event; successful order intake returns seven ordered downstream Agent events with API/data evidence; the right workflow panel renders each event as an individual Agent message card with an Agent-specific mini-spirit visual identity and sequential playback.
- Verification: Python and Spring Boot compilation passed; the final real MySQL verifier reached `MATERIAL_ISSUED`, returned seven completed downstream Agent events, and cleaned all test rows. Frontend production build passed with pre-existing CSS/eval/chunk-size warnings.
- Main-chat orchestration refinement: the workflow drawer no longer opens automatically. A new `agent_name` chat metadata field makes the initial reply visibly come from `IntentRoutingAgent`, with a controlled intent-analysis API result before the real order form. After confirmation, the eight real MES-derived sub-agent events (including order confirmation/work-order generation) play as individual slow-streamed cards in the main chat, each with a distinct mini-spirit color. Event records now carry controlled analysis prompts that constrain any later external-model explanation to returned facts. The native delivery-date field explicitly opens a calendar picker.
- Verification: `python -m compileall` passed; a no-write workflow assertion verified the natural-language intent, visible routing speaker, interaction form and all eight prompted workflow events; `npm run build` passed. AI service `:8090` was restarted and its OpenAPI schema confirmed the new `agent_name` field is live.
- Durable conversational audit completed: every visible scheduling sub-agent card and the final main-agent summary is now written to the authenticated user's existing `agent_session` / `agent_message` records before the frontend animates it. A versioned JSON message envelope preserves Agent identity and workflow evidence in the established message table without an unsafe schema change while Flyway is disabled; history APIs unwrap it and retain backward compatibility with historic plain-text messages. Order submission requires the current user-owned open main-chat session, preventing cross-user or orphan workflow output. Verification created and cleaned a real order-to-material-issued run: 8 workflow events, 9 persisted Agent cards, and a successful conversation-history readback; post-cleanup SQL found zero verification orders/work orders.
- Workflow-drawer visual refinement: regrouped the expanded panel's lower plan/Agent/step/tool content into a consistent detail grid, made tools align in a compact two-column table with overflow handling, added a restrained audit tail and responsive single-column fallback. Frontend production build passed (existing project-wide CSS/dependency warnings remain).
- External-model analysis added: the scheduling Agent now calls the selected OpenAI-compatible provider exactly twice in a successful flow—once for schema-validated intent recognition and once for the final, facts-only scheduling summary/risk suggestion. Prompts are versioned and bounded; model output cannot route around the deterministic scheduling guard or alter MES writes. Provider, source, prompt version and output are persisted in the main-chat message envelope; unavailable/invalid API output is visibly marked as a rules fallback. Live cleanup-safe verification completed a real order-to-material-issued run and confirmed both calls returned from `DeepSeek V4-Pro`, with 8 workflow cards and final summary persisted and history-read back successfully. Test rows were removed afterwards.

## Session: 2026-07-15 — Autonomous production-material issue Agent

- **Status:** in_progress
- Scope: after the existing scheduling transaction succeeds, use the same `wm_issue_*` / `wm_material_stock` / `wm_transaction` business path as the Outbound Operations page to create the issue document, allocate actual stock, execute outbound, sync consumption traceability and advance the work-order lifecycle to `MATERIAL_ISSUED`.
- Guardrail: only a scheduled work order with dispatches and a non-empty `work_order_bom` may enter the Agent. Pick quantities must exactly equal issue quantities; any shortage, manual in-progress issue document, frozen stock or reconciliation failure rolls the whole Agent transaction back.
- Validation note: Spring Boot compilation passed. An initial combined Python/frontend check used `agent-system/backend` as the npm working directory and therefore failed before running the frontend build; rerun Python and npm checks in their respective project roots.
- Validation note: the corrected Vite build required unsandboxed child-process permission, then exceeded the 120-second command timeout without emitting a compiler error. Do not repeat the same full build immediately; use the prior successful build baseline plus focused source checks, and rerun it later with a longer CI timeout if required.
- Verification failure 1: the first end-to-end call reached a stale Spring Boot child process after the launcher stopped only its wrapper PID, so the new `/api/agent/production-issue/*` route was not loaded. The failed request left one identifiable `AGENT-VERIFY-*` order/work order; it will be removed with the verifier cleanup path before retrying after a forced process restart.
- Verification success: after restarting the actual listener process, the cleanup-safe live verifier passed order intake -> BOM/route + kitting -> Gantt + dispatch -> production issue. It created a finished issue document, checked stock transaction and consumption-detail counts against pick-detail count, advanced the work order to `MATERIAL_ISSUED`, and reported `readyForShopFloor=true`. Post-cleanup SQL confirmed zero `AGENT-VERIFY-*` orders, work orders or issue documents remained.
- Verification note: a final combined residual-check command accidentally invoked Python from the repository root, where `app/` is not importable. The database residual checks and `git diff --check` completed; rerun only Python compilation from `agent-system/backend`.
- Final verification: after loading the final FastAPI guardrail update, the end-to-end verifier passed again. It reported a `FINISHED` issue document, equal pick/transaction/consume-detail counts, `MATERIAL_ISSUED`, and `readyForShopFloor=true`; SQL then confirmed zero test orders, work orders and Agent issue documents. Spring Boot compilation, Python compilation and `git diff --check` passed. Frontend production build also passed (existing CSS/eval/chunk-size warnings remain).

## Session: 2026-07-15 — Autonomous kitting-to-Gantt scheduling Agent

- **Status:** in_progress
- User authorized the next autonomous scheduling stage after BOM/route and material-risk analysis.
- Scope: reserve material through the real MES lifecycle, create Gantt production tasks, synchronize dispatches, advance lifecycle, then validate the resulting records.
- Guardrail: scheduling writes require an eligible JWT role, confirmed order status, a released BOM/route, sufficient material, and no duplicate complete schedule.
- Verification note: the first test attempt stopped before any business write because its new inventory-snapshot helper treated a dictionary cursor row as a tuple. The helper was corrected before retrying.
- Implemented `AgentSchedulingService` as the Spring transaction boundary. It validates confirmed/released work orders, resolves released BOM + enabled route, reserves qualified FIFO inventory, writes a kitting analysis, advances `KITTING_OK`, assigns enabled workstations using actual existing task end times, creates production tasks, synchronizes dispatches and validates the writeback.
- Added FastAPI execution/advisory gateways and connected successful LOW-risk order intake to autonomous scheduling. Device/maintenance data is reported as unavailable rather than fabricated when the current `fan_mes` database has no `device`/`maintenance_task` tables.
- Final live verifier passed: order `CONFIRMED`; work order `SCHEDULED`; 4 `production_task` rows and 4 `dispatch_task` rows. Test data was removed and inventory lock values were restored. A direct residual query returned 0 test orders and 0 test work orders. Frontend production build passed (existing CSS/font and chunk-size warnings remain).

## Session: 2026-07-15 — LangGraph sub-agent planning
- **Status:** in_progress
- Read `MES-GOLDEN-PATH.md` and identified scheduling steps 1–6 as the initial Agent delivery scope.
- Reviewed the existing `agent-system` MVP workflow, agent registry, and tool registry.
- Next: write the golden-path-driven sub-agent architecture plan in `agent-system/`.

### Phase 6 completion
- **Status:** complete
- Created `agent-system/LANGGRAPH_SUBAGENT_PLAN.md`.
- Verified the document contains the P0/P1/P2 agent groups, P0 scheduling graph, tool and approval matrix, target state model, migration mapping, target directory, and delivery phases.

## Session: 2026-07-14

### Phase 1: 计划与范围确认

- **Status:** complete
- Actions taken:
  - 审阅排产、齐套、派工、操作工报工、完工入库的前后端实现。
  - 明确最小打通范围、现存断点和实施优先级。
  - 创建持久化计划文件，后续实施可从 Phase 2 恢复。
- Files created/modified:
  - `task_plan.md`（创建）
  - `findings.md`（创建）
  - `progress.md`（创建）

## Test Results

| Test | Input | Expected | Actual | Status |
|------|-------|----------|--------|--------|
| 代码链路审阅 | 排产至入库相关实现 | 确定可运行链路与断点 | 已确认任务/派工、报工 ID、完工/入库三类断点 | 完成 |

## Error Log

| Timestamp | Error | Attempt | Resolution |
|-----------|-------|---------|------------|
| 2026-07-14 | 未找到既有 planning 文件 | 1 | 创建新的项目级规划文件。 |

## Session: 2026-07-15 — Phase 7

- **Status:** complete
- Actions taken:
  - 以十步黄金流程为索引，核对生命周期服务、派工同步、领料/报工/质检/入库写回及追溯 Controller。
  - 核对 Agent MVP 的真实状态：目前为硬编码场景与 Mock 工具，Spring Boot 只读网关未覆盖黄金流程。
  - 将实现级数据库、接口、工具、权限和异常分支映射补充至 `agent-system/LANGGRAPH_SUBAGENT_PLAN.md`。
- Verification:
  - 文档中的关键端点和生命周期门禁已与 Spring Boot 源码交叉核对。

## Session: 2026-07-15 — Phase 8 kickoff

- **Status:** in_progress
- Next implementation milestone:
  - Build one authenticated, read-only Agent loop around a real work-order pipeline before adding more agents or any write action.
- Rationale:
  - Current Agent orchestration is hard-coded and the registered tools are mock data, while MES already exposes real pipeline and trace facts guarded by JWT.

## Session: 2026-07-15 — RAG architecture correction

- Decision:
  - Start Phase 8 with a governed RAG MVP, then connect real-time MES tools, and only then compose the LangGraph routing graph.
- Boundary:
  - RAG is the read-only enterprise knowledge layer; real MES state remains tool/API sourced, and task state/memory remains separately scoped.

## Session: 2026-07-15 — RAG knowledge inventory

- **Status:** Phase 8.1a complete
- Actions taken:
  - Reviewed the real golden-path guides, role views, database design/landscape, seed-data story, Agent boundary, and lifecycle/quality migrations.
  - Created `agent-system/RAG_KNOWLEDGE_CATALOG.md` with P0 sources, implementation-derived knowledge cards, master-data snapshot rules, required metadata, exclusions, and real-business evaluation questions.
- Verification:
  - Every P0 source is mapped to real pages, APIs, tables, roles, or lifecycle services. Dynamic execution facts are explicitly excluded from RAG and assigned to authenticated MES tools.

## Session: 2026-07-15 — Vector store selection

- **Decision:** self-hosted Qdrant for `mes_knowledge_v1`.
- **Reason:** low-friction FastAPI integration plus payload filtering and hybrid retrieval support needed for MES knowledge scope and permissions.
- **Deferred:** Milvus and pgvector are not needed in the present local, MySQL-centred deployment.

## Session: 2026-07-15 — Qdrant infrastructure deployment

- **Status:** in_progress
- Completed:
  - Added a localhost-only Qdrant Compose service, persistent storage exclusion, FastAPI Qdrant settings/client, collection initializer, and README instructions.
  - Started the `mes-qdrant` Docker container, installed `qdrant-client`, created `mes_knowledge_v1` (1024-dimension cosine), and verified an upsert → filtered query → delete round trip.
- Errors handled:
  - Initial PyPI install was blocked by the restricted network; retried with approved network access and installed successfully.
  - Project-root `.gitignore` is absent in the current dirty worktree, so no root file was recreated; the Qdrant data exclusion was scoped to `agent-system/.gitignore`.
  - PowerShell `HEAD` on the dashboard failed client-side; health API had passed and dashboard verification will use an ordinary GET request.

## Session: 2026-07-15 — RAG ingestion pipeline

- **Status:** in_progress
- Added:
  - A lazy local embedding adapter (BAAI/bge-m3, 1024 dimensions), controlled source registry for K01–K05, Markdown heading-aware chunking, deterministic re-ingestion, citation-bearing retrieval, and import/query scripts.
- Boundary:
  - Only approved baseline business documentation is eligible for initial ingestion. Dynamic MES facts and raw seed SQL remain excluded.
- Errors and resolution:
  - The initial BGE-M3 download repeatedly stalled at a 2GB incomplete weight file. Switched before ingesting any data to `BAAI/bge-small-zh-v1.5` (512 dimensions), which is adequate for the current P0 document corpus and allows a future versioned migration to a larger hybrid embedding model.
  - The active pytorch environment is Python 3.9, which cannot install Qdrant client 1.17+. The Compose image is therefore pinned to Qdrant 1.16.1, matching the supported 1.16.1 client, instead of using the incompatible `latest` server image.
  - The first small-model ingestion exposed one Python 3.10-only `zip(..., strict=True)` call. Replaced it with an explicit length check plus Python 3.9-compatible `zip`; the embedding model download itself succeeded.
  - A non-escalated model load was blocked by the restricted network proxy. Retrying with approved network access imported 88 real MES knowledge chunks successfully.

## Session: 2026-07-15 — Qdrant RAG MVP completed

- **Status:** Phase 8.1b complete
- Completed:
  - Started localhost-only `mes-qdrant` (`qdrant/qdrant:v1.16.1`) with persistent storage at `agent-system/data/qdrant/`.
  - Recreated `mes_knowledge_v1` as a 512-dimension cosine collection, built payload indexes for review status, role, factory, product, process and lifecycle filtering, then imported 100 approved chunks from seven real MES sources.
  - Added a heading-aware ingestion path, local `BAAI/bge-small-zh-v1.5` embedding adapter, deterministic re-ingestion and citation-bearing retrieval.
  - Added JWT-protected `GET /api/rag/health` and `POST /api/rag/search`; the client role is read from the MES token and cannot be supplied by the request body.
- Verification:
  - Qdrant REST API reports `1.16.1`; dashboard `http://127.0.0.1:6333/dashboard/` returned HTTP 200; Docker reports `mes-qdrant` Up.
  - Authenticated API smoke test returned HTTP 200, 100 indexed points, and the exact lifecycle source for `QC_FAILED` receipt gating. The same health endpoint without a bearer token returned HTTP 401.
  - `python -m compileall -q app/rag app/main.py app/schemas.py` passed.

## Session: 2026-07-15 — RAG knowledge expansion

- **Status:** complete
- Scope: derive K08–K13 from the real MES scheduling, dispatch, issue, feedback, IPQC, receipt and traceability implementations. Each card must state the authoritative API/service, table writebacks, lifecycle transition and downstream connection; raw code, SQL and live transaction rows remain excluded from the vector corpus.
- Completed:
  - Added six source-backed knowledge cards: `SCHEDULING_TO_DISPATCH.md`, `MATERIAL_ISSUE_EXECUTION.md`, `SHOP_FLOOR_FEEDBACK.md`, `IPQC_AND_QUALITY_GATE.md`, `FINISHED_GOODS_RECEIPT.md`, and `WORK_ORDER_TRACEABILITY.md`.
  - Registered K08–K13 as approved, role-scoped sources and re-ingested all baseline sources. `mes_knowledge_v1` now contains 130 chunks from 13 real MES documents.
  - Restricted the internal Compat database-landscape source to `MANAGER` and `TESTER`; production-supervisor retrieval no longer receives engineering test-data material from that source.
- Verification:
  - The six questions for dispatch, issue/consume, feedback task identity, IPQC failure, finished-goods receipt and work-order batch/SN traceability all returned their intended K08–K13 source within the default Top-5 retrieval result.
  - Added and executed `agent-system/backend/scripts/evaluate_knowledge.py`; the repeatable regression set passed 6/6 cases against the live Qdrant collection.

## Session: 2026-07-15 — P0 Agent persistence foundation

- **Status:** complete
- Implemented `V32__agent_p0_persistence.sql` with durable LangGraph checkpoints and runtime state. Because the local Flyway profile is disabled, the idempotent bootstrap applies that migration explicitly.
- Mapped all 13 approved sources / 130 Qdrant points into the existing MySQL `knowledge_*` audit tables, retaining deterministic vector IDs and source metadata.
- Registered eight P0 agents, six read-only tool contracts, 31 permissions, four policies, six guardrail rules, local model metadata, a versioned P0 system prompt, and 30 evaluation cases. No write tool is granted.
- Replaced memory-only traces and confirmations with MySQL-first persistence plus an in-process outage fallback. Separate-process recovery verification passed for a trace, confirmation and `INTERRUPTED` checkpoint; test rows were removed afterwards.

## Session: 2026-07-15 — Real Agent next-step roadmap

- **Status:** planned
- Confirmed the immediate implementation priority is Phase 8.2b: JWT-protected, real MES read tools for pipeline, kitting, production tasks, dispatch and traceability.
- Wrote `agent-system/AGENT_IMPLEMENTATION_ROADMAP.md` with the ordered implementation gates: real read tools → minimal read-only LangGraph → scheduling analysis drafts → approval-gated write facade → observability/evaluation and staged rollout.

## Session: 2026-07-15 — P0 real read tools

- **Status:** complete
- Confirmed that existing `GET /api/planning/kitting/{workOrderId}` inserts `kitting_analysis` and `material_shortage`, while `GET /api/traceability/work-order/{workOrderNo}` runs trace synchronization. These endpoints cannot be presented as Agent read-only tools.
- Added `/api/agent/read/*`: side-effect-free pipeline, kitting, tasks, dispatch and trace snapshots. The facade requires a valid MES JWT and restricts sensitive material facts by role; an operator receives only their own dispatches.
- Added FastAPI `GET /api/agent/read-tools` and `POST /api/agent/read-tools/{tool_name}`. It validates named tool arguments, forwards only the caller JWT through an explicit Spring API allowlist, and returns source / query time / trace metadata.
- Fixed the global `ResponseStatusException` mapping so a role denial is a true HTTP 403 rather than an internal 500. Anonymous tool requests return 401.
- Verification passed with real seeded work order `WO-20260701`: five MES tools returned live facts; kitting rows remained `[42, 49]` before and after the read; anonymous request was 401; line operator material-read attempt was 403.
- Known scope boundary: the current `work_order` data model has no factory ownership field, so factory-level filtering cannot be truthfully enforced yet. The current P0 facade enforces JWT and roles; add a work-order-to-factory data contract before multi-factory rollout.

## Session: 2026-07-15 — 云枢小智主对话持久化

- **Status:** complete
- Replaced browser-memory-only main-chat history with user-scoped Agent platform persistence:
  - `POST /api/agent/chat` now resolves the user and role from the MES JWT, records the USER message before orchestration and the AGENT message after it returns.
  - Added authenticated `GET /api/agent/sessions` and `GET /api/agent/sessions/{session_id}` endpoints. Both enforce ownership by user and `AGENT_MAIN`; sessions cannot be read across users.
  - Updated `AgentHome.vue` to load saved sessions at page entry and fetch persisted message history when a user selects one.
- Verification:
  - `python scripts/verify_main_chat_persistence.py --password 123456` passed: unauthenticated chat returned 401; authenticated chat created a database session, reloaded two persisted messages through the history APIs, and then cleaned up its own test records.
  - `npm run build` passed.

## Session: 2026-07-15 — 智能排产节点 1：订单接收 Agent

- **Status:** complete
- Added `OrderIntakeAgent` as the first bounded scheduling sub-agent. The exact prompt “我有一个新订单，需要排产。” routes to `scheduling_order_intake` and supplies a controlled order-entry interaction in the main chat.
- The form reads products from the real MES product master and collects order number, customer, product, quantity, and delivery date. Its only write action requires an explicit click on “创建订单”, validates the selected product again server-side, forwards the caller JWT, and reuses `POST /api/planning/orders` with status `CREATED`.
- It intentionally stops after creation and directs the user to perform the next real manual gates in Order Center: confirm order, then generate work order. Those gates will become the next Agent nodes after the user walks through them.
- Verification:
  - `python scripts/verify_order_intake_agent.py --password 123456` passed: 11 real product options, unconfirmed write rejected with 422, confirmed order created as `CREATED`, and the Order Center API read it back. Test order/session/trace were cleaned up.
  - `npm run build` passed.

## Session: 2026-07-15 — 智能排产 Agent 的受控 RAG 接入

- **Status:** complete
- Added role-scoped Qdrant retrieval before the two external-model explanation calls: intent recognition and the final scheduling/risk summary.
- Safety boundary: retrieved SOP passages are read-only explanatory context only. The order, BOM, kitting, scheduling, dispatch and material-issue services remain the sole authority for real-time facts, decisions and database writes. If Qdrant or embeddings are unavailable, the workflow automatically continues without RAG context.
- Auditability: citations and excerpts are retained in the existing persisted `model_analysis` envelope; the intent workflow card records its Qdrant sources and the final summary visibly includes its rule basis.
- Verification: live `mes_knowledge_v1` reported 130 points; complete real order-to-material-issued regression passed with DeepSeek V4-Pro for both model calls, 3 RAG citations at both call sites, 8 workflow events, 9 persisted cards and successful history/MES readback. The cleanup check confirmed 0 remaining `AGENT-VERIFY-*` orders.

## Session: 2026-07-15 — Qdrant 启动时索引损坏自动恢复

- **Status:** complete
- Updated `start.ps1` to detect only known Qdrant persistent-index corruption signatures. Docker unavailability, ordinary slow startup and other health failures leave the existing vector data untouched.
- On confirmed corruption it stops only the scoped Compose service, moves (never deletes) `agent-system/data/qdrant/` to a timestamped `qdrant-corrupt-*` backup, recreates the collection, re-ingests approved sources and runs the RAG retrieval regression suite before continuing with the MES services.
- Recovery was exercised manually after a corrupt-index restart loop: the rebuilt collection is green with 130 points and the 6-case evaluation passed. The normal startup path was subsequently executed successfully with Qdrant, backend, Agent API and frontend all healthy.

## Session: 2026-07-16 — 智能排产子 Agent 独立包装

- **Status:** complete
- Created `app/scheduling/agents/` with a shared in-memory `SchedulingAgentState`, a common base wrapper and individually packaged modules for intent routing, order intake, BOM/route, kitting risk, lock execution, capacity scheduling, dispatch synchronization, scheduling validation and production issue.
- `OrderIntakeService.create_order()` remains the unchanged public gateway but now delegates internally to `SchedulingSubAgentWorkflow`; the original MES order API calls, analysis service, atomic scheduling command, production-issue command, response schema and workflow-event order are preserved.
- Registered all nine active scheduling-related Agent identities in `agents/registry.py`. Added `app/scheduling/agents/README.md` with the extension contract for future Agent modules.
- Verification: Python compile/import check passed; restarted the Agent API; `verify_order_intake_agent.py` passed a real cleanup-safe order-to-material-issued run with 8 workflow events, 9 persisted cards, 4 production tasks, 4 dispatch tasks, two DeepSeek analyses and two RAG retrievals. The verifier cleaned the generated business and conversation records.

## Session: 2026-07-16 — 全 MES 业务子 Agent 盘点与路线图

- **Status:** complete
- Reviewed the live MES module boundaries outside intelligent scheduling: production execution, quality gates, warehouse receipt/issue/return, barcode and packaging, field response and Andon, equipment, traceability, master data/process/calendar, reporting, integration and system governance.
- Added `agent-system/MES_FULL_AGENT_CATALOG.md`. Every proposed Agent is tied to a real MES module, its fact source, permitted write boundary, lifecycle/approval gate, RAG role and implementation readiness; it explicitly separates safe read-only capabilities, approval-gated execution and capabilities that must remain reserved.
- Recommended the next closed-loop package as `ProductionFeedbackAgent → IPQCQualityGateAgent → FinishedGoodsReceiptAgent → TraceabilityAgent`. This continues the already-running scheduling flow from `MATERIAL_ISSUED` through production, quality release, finished-goods inventory and traceability, without inventing a new parallel workflow.
- No API, database schema, business-service or UI behavior was changed during this inventory pass.

## Session: 2026-07-16 — 新增业务子 Agent 独立包装

- **Status:** complete
- Added the non-writing `app/business_agents/` layer with seven separately importable Agents: `ProductionExecutionAgent`, `QualityManagementAgent`, `WarehouseLogisticsAgent`, `TraceabilityAgent`, `EquipmentMaintenanceAgent`, `AndonResponseAgent` and `OperationsInsightAgent`.
- Each package requires a caller JWT and retrieves only live MES facts through the Spring Boot GET allowlist. The return envelope marks the source as `MES_DATABASE_VIA_SPRING_BOOT`, `read_only: true` and `workflow_connected: false`; future write actions are contracts only and cannot execute.
- Registered the seven identities with only their currently usable real-data resources and documented their boundaries in `app/business_agents/README.md`.
- Verification: compile/import passed. `verify_business_agent_packages.py --password 123456` passed against `WO-20260701`, reading real task/dispatch, quality pending/summary, inventory/kitting, traceability, equipment workbench, Andon and dashboard facts for all seven packages with zero writes. The test also identified and avoided two stale 500 endpoints (`/api/quality/records`, `/api/equipment/devices`) in favor of active Yunshu compat endpoints.

## Session: 2026-07-16 — Agent 总览抽屉

- **Status:** complete
- Replaced the third left-sidebar tool label from “插件” to “Agent 总览”. Clicking it opens a right-side, responsive 4×4 overview drawer containing the 16 real packaged sub-Agent identities.
- Added `AgentOverviewPanel.vue` with a cool-warm glass gradient, cute no-mouth companion avatars, 16 distinct color tones, numbered cards, and truthful status labels: 9 scheduling Agents are “已接入”; 7 real-data business packages are “待接入”.
- The overview drawer is mutually exclusive with the existing workflow trace drawer. Added matching display labels and avatar color mappings for all seven future business-Agent messages without connecting them to the default workflow.
- Verification: frontend `npm run build` passed. The restricted environment initially blocked esbuild child-process startup; the normal production build then completed successfully outside that restriction. Existing third-party bundle warnings remain pre-existing and do not concern this change.

## Session: 2026-07-16 — Agent 总览可读性与头像微动优化

- **Status:** complete
- Removed the aggregate status legend requested by the user. Increased the overview title, introduction, card label, module and status typography while retaining the 4×4 desktop grid.
- Replaced the flat dark eyes with blue-grey gradient eyes, subtle white catchlights, small gaze shifts and natural blink cycles. Card groups use staggered animation delays so the sixteen companions do not move in lockstep.
- Verification: frontend `npm run build` passed; existing third-party bundle warnings are unchanged.

## Session: 2026-07-16 — 质量管理 Agent 工作流设计

- **Status:** complete
- Added `agent-system/QUALITY_AGENT_WORKFLOW_PLAN.md`, an implementation-ready design for a quality workflow around the already packaged `QualityManagementAgent`, without creating extra empty quality sub-Agents.
- The plan fixes the model boundary: exactly two external API calls (bounded intent routing and final fact-grounded summary); local Qwen3-0.6B performs only compact, token-streamed queue/defect/work-order micro-analysis between real data retrieval and final summary.
- It specifies the missing Agent-only quality read facades, real quality endpoints, RAG scope, SSE/persistence behavior, deterministic risk rules, strict read-only first release, future human-confirmed write gates, fallbacks and zero-write evaluation cases.

## GitHub upload — 2026-09-08
- Verified source and inventoried all files. Inspecting target visibility and credential files before staging.


### GitHub upload completed
- Pushed main to https://github.com/Xavier-ky/yunshu-mes-all.git; remote/local commit IDs match.
- Git reported all 6 LFS objects (3.1 GB) uploaded. Git LFS fsck passed.

