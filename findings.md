# Findings & Decisions

## Requirements

- 用户要先获得最小打通计划和工程量评估；尚未授权直接实施。
- 目标是数据库和业务数据从订单到成品入库完整关联，而非重做所有 MES 功能。

## Agent foundation extension scope (2026-07-16)

- The user authorized completion of the previously identified Agent foundations only when they do not alter existing MES business behavior. New capabilities therefore must be sidecar-only and must use `agent_runtime_state` / `agent_graph_checkpoint`, never MES transactional tables.
- Existing Qdrant RAG is governed business knowledge retrieval, not user long-term memory. Conversation history is persisted, but it is not yet a policy-controlled short-term/long-term memory service.
- Existing `SchedulingOrchestrationSidecar` already provides an owner-scoped approval pause/resume contract. It should be documented and tested as a future human-in-loop bridge, not wired into the currently working order-confirmation flow.
- Safe initial delivery: a typed runtime-memory service, a static truthful tool catalogue, and a deterministic fact-only reflection record. No new LLM calls, no prompt changes, no new external/network tools, and no agent execution permissions.

## Agent foundation implementation findings (2026-07-16)

- Implemented `app.foundation` as a standalone, non-imported-by-workflow layer. It uses only `agent_runtime_state` and `agent_graph_checkpoint`; current scheduling/quality executors and Spring Boot business APIs were not modified.
- Short-term memory is owner + session scoped and has an eight-hour TTL. Long-term memory accepts only explicit, reviewable `preference`, `decision_pattern` or `operational_note` records and is deliberately not automatically injected into prompts.
- Tool catalogue covers 12 truthful capabilities. Existing controlled-write capabilities retain their current confirmation, role and domain-service gates; planned image analysis and arbitrary script execution remain explicitly unavailable.
- Reflection turns an owner-owned graph checkpoint into structured factual outcome, completed/attention nodes, decision basis and next action. It does not reveal chain-of-thought, resume/replay a graph or change MES business data.
- The MySQL local profile evaluates `CURRENT_TIMESTAMP` in local time while Python `utcnow()` is UTC. The initial TTL test made newly written short memory appear expired; the service now uses the database-aligned local clock for this naive DATETIME field. The actual runtime-state primary key is `runtime_state_id`, not `state_id`; the list query was corrected after live schema inspection.
- `verify_agent_foundation.py` passed using real Agent-platform tables and finally cleaned its exact temporary memory, reflection and checkpoint rows. The new RAG governance card ingested five chunks; Qdrant is green with 176 points. A follow-up standalone semantic query exceeded the shell timeout while loading the local embedding model; ingestion itself completed successfully and no business function was affected.

## Unified real-tool packaging scope (2026-07-16)

- The project already has one real, schema-validated, JWT-forwarding read gateway: `app.tools.mes_read.MesReadToolGateway`. It exposes five MES snapshots (pipeline, kitting, task, dispatch and trace) plus approved Qdrant knowledge search.
- The legacy `app.tools.registry` contains mock handlers and must not be reused for a professional real-tool package.
- Safe packaging can be a thin typed adapter over `mes_read_tools.invoke`: preserve its whitelist, parameter validation, JWT propagation and source labels; add no direct MySQL reads, no new Spring Boot route and no write tool.
- A separate `/api/agent/foundation/tools/real/*` surface can be added without touching current workflow routes. It must remain read-only and caller-authorized.

## Unified real-tool packaging implementation (2026-07-16)

- Added `app.foundation.tools` as a typed wrapper over exactly six real tools: five authenticated MES snapshots and one role-filtered Qdrant search. Each call delegates to `MesReadToolGateway`; the wrapper adds no SQL, no URL allowlist, no role bypass and no write method.
- Added the independent `GET /api/agent/foundation/tools/real` and `POST /api/agent/foundation/tools/real/{tool_name}` APIs. They are not imported by the scheduling graph or quality workflows.
- `verify_packaged_real_tools.py` logged in as a production supervisor and called pipeline, kitting, production-task, dispatch and trace wrappers against a real work order. Kitting-analysis/shortage counts remained `42/49` before and after; no business write occurred.

## Research Findings

## LangGraph sub-agent planning findings (2026-07-15)

- `work_order_id` is the golden-path spine. Scheduling steps 1–6 are: order confirmation, work-order release, kitting reservation, Gantt scheduling, process dispatch, and material issue.
- Steps 7–10 (shop-floor reporting, in-process quality, finished-goods receipt, traceability) remain part of the same lifecycle graph and should be planned as later stages.
- The current `agent-system` is an MVP: `workflow.py` uses hard-coded scenario branches, `agents/registry.py` already defines nine business agents plus MainAgent, and `tools/registry.py` still relies mainly on mock tools.
- Equipment, personnel, process, and master-data agents should be included as reserved capability boundaries but excluded from the default route until their MES tools and data contracts are available.
- Deliverable: `agent-system/LANGGRAPH_SUBAGENT_PLAN.md` defines P0 scheduling agents A0–A8, P1 execution agents B1–B5, P2 reserved agents C1–C16, deterministic graph nodes, tool contracts, approvals, graph state, and a migration path from the MVP.

## Implementation-backed LangGraph findings (2026-07-15)

- The implemented golden path is anchored on `work_order_id`: `customer_order` / `customer_order_item` → `work_order` → `kitting_analysis` → `production_task` → `dispatch_task` → issue / feedback / IPQC / receipt documents.
- `WorkflowPipelineController` already exposes role todos and a work-order pipeline at `/api/planning/workflow/todos` and `/api/planning/workflow/pipeline/{key}`. `ReverseTraceController` exposes the complete work-order trace at `/api/traceability/work-order/{workOrderNo}`.
- `WorkOrderLifecycleService` is the business gate: issue only allows RELEASED/KITTING_OK/SCHEDULED, feedback only allows MATERIAL_ISSUED/IN_PROGRESS/QC_PENDING, and receipt only allows QC_PASSED/COMPLETED. Existing legacy rows with null lifecycle are intentionally not blocked.
- Execution writebacks are real: Gantt task save can create `dispatch_task` through `DispatchSyncService`; issue execute writes `wm_item_consume` and MATERIAL_ISSUED; feedback execute writes `production_report` and QC_PENDING; IPQC writes QC_PASSED/QC_FAILED; receipt execute writes COMPLETED and synchronizes trace data.
- The FastAPI Agent MVP remains hard-coded and its registry tools are mock data. Its `SpringBootClient` read allowlist currently omits pipeline, trace, kitting, production-task, dispatch, issue, feedback, IPQC, and receipt resources. A controlled MES Agent API facade is required before a LangGraph agent can use the real process safely.

## Professional Agent architecture decision (2026-07-15)

- RAG should be built as an early knowledge foundation, but it must not replace real-time MES tools. Transactional facts (work-order lifecycle, stock, dispatch, quality result, receipt status) stay behind JWT-protected APIs; RAG contains versioned SOPs, rules, manuals and explanatory knowledge.
- RAG, short-term graph state/checkpoints, and long-term user memory require separate stores and separate write permissions. Shared enterprise knowledge is read-only to the model and updated only through a governed ingestion workflow.
- The first Agent graph should route questions to knowledge retrieval, MES tools, or a hybrid path; hybrid answers must ground current facts in tools and cite the applicable SOP/rule chunks.

## First RAG knowledge inventory (2026-07-15)

- Verified P0 business sources are `MES-GOLDEN-PATH.md`, `docs/workflow-golden-path.md`, `docs/role-views.md`, `docs/core-database-design.md`, and the current compat database landscape.
- Real implementation rules should be derived from lifecycle, kitting, dispatch, issue, feedback, IPQC, receipt, and traceability services rather than indexing raw Java or migration SQL for business users.
- `docs/seed-data.md` and `R__seed_story_*.sql` are valuable for realistic evaluation fixtures (FS40-A, WO-GP-*, shortage, quality, traceability scenarios), but must be sanitized and must not be used as a source of live answers.
- The catalog records RAG scope, source authority, role filters, version/effective-date requirements, dynamic-data exclusions, and the first E01–E07 evaluation cases in `agent-system/RAG_KNOWLEDGE_CATALOG.md`.

## Vector store decision (2026-07-15)

- Select self-hosted Qdrant for the first MES RAG implementation. Its payload filters map directly to the required role, factory, product, process, lifecycle, version, and review-status restrictions; it also leaves a clean path to dense+sparse hybrid retrieval.
- Keep MySQL `fan_mes` as the transactional source of truth and RAG governance metadata store. Qdrant stores vectors and searchable payload only; it must not contain live business facts or become the authority for writes.
- Defer Milvus until scale/multi-tenant isolation requires it, and do not add PostgreSQL solely to run pgvector while the project is MySQL-based.

## Implemented RAG MVP findings (2026-07-15)

- Qdrant is running locally as `mes-qdrant` (v1.16.1) and bound only to `127.0.0.1:6333-6334`; its dashboard and REST API were both verified.
- `mes_knowledge_v1` is a 512-dimension cosine collection populated with 100 approved, citation-bearing chunks. The corpus is sourced from seven real MES golden-path, role, data, compatibility, lifecycle and kitting-rule documents; it deliberately contains no live stock, order, task or quality facts.
- The embedding adapter loads `BAAI/bge-small-zh-v1.5` from the local model cache. BGE-M3 was not retained for this MVP because its multi-gigabyte first download repeatedly stalled; retrieval quality for the present governed corpus is verified and the embedding/collection version can be migrated later.
- FastAPI now exposes only JWT-protected read endpoints for RAG. `POST /api/rag/search` derives role scope from `Principal.roles`, always requires `review_status=approved`, and returns citations so later LangGraph nodes can ground their answer. It is not yet wired into the LLM answer path; that belongs to Phase 8.3 after real MES tool contracts are added.

## Expanded system-derived MES rules (2026-07-15)

- K08–K13 are now source-backed by the production code rather than inferred from diagrams: `production_task → dispatch_task`; `wm_issue_* → wm_item_consume*`; `pro_feedback → dispatch_task/production_task → production_report*`; `qc_ipqc*`; `wm_product_recpt* → pro_card/wm_product_produce/inventory_batch`; and the `work_order_id`-centred trace graph through SN/material bindings.
- Execution facts are deliberately not embedded. The RAG cards name the tables and APIs required to explain a result, but a user-specific answer still needs a later JWT-protected MES tool call for the actual work order, batch, quantity, status or SN.
- Retrieval evaluation covers one natural-language question per expanded step. All six expected source IDs were present in the default Top-5; role scoping was tightened after the evaluation found that the broad Compat landscape document could otherwise surface engineering test-data text to a production supervisor.

- 现有数据主链为 `customer_order → work_order → production_task → dispatch_task → pro_feedback`。
- Yunshu 甘特排产页面主要创建 `production_task`；操作工工作台读取 `dispatch_task`，任务到派工尚未自动衔接。
- 报工创建与执行阶段对 `taskId` 的解释不一致，存在派工 ID 与生产任务 ID 混用风险。
- 完工当前可直接完成工单；成品入库当前未形成库存余额与库存流水的强制联动。

## Technical Decisions

## Quality report continuation workflow findings (2026-07-16)

- The existing quality workflow is a read-only five-node flow and must remain unchanged. A report request can safely become a separate continuation route that refreshes a real MES snapshot rather than relying on prior natural-language output.
- `AgentQualityReadService.overview()` already supplies live summary, trend, defect-top, severity, type statistics and pending-task data. `QcAnalyticsController` also exposes live `recentFinished` and `pendingDisposition` data; adding them to a dedicated report snapshot is sufficient for real chart/table content.
- The frontend already contains ECharts and a sequential SSE/card writer. Use new structured report events and one report card keyed by `report_id`; do not stream base64 chart images or create one card per chart/token.
- The Spring application already includes `poi-ooxml:5.2.5`, while the old weekly report endpoint is an explicit 501 stub. A new JWT-scoped quality-report export endpoint should generate DOCX from the frozen server-side report snapshot and remain isolated from the legacy stub.
- Persist report snapshots separately from mutable QC tables. The existing versioned `agent_message` envelope remains suitable for individual visible cards; an immutable report artifact is needed for safe, repeatable export and ownership checks.
- Implementation verification: the report route is ordered before the broad quality-analysis detector, so “生成质量分析报告” cannot fall back into the five-node route. It emitted four controlled ECharts payloads and two real-data tables from the frozen `fan_mes` snapshot, then exported the identical snapshot as a valid DOCX through JWT checks in both FastAPI and Spring Boot.
- Migration versioning matters even in the local profile: an initially-added V5 migration conflicted with existing `V5__planning_ktg_compat.sql` at Spring startup. The report artifact migration is V33; Agent runtime uses `CREATE TABLE IF NOT EXISTS` because the current local profile keeps Flyway disabled.

## Scheduling sub-Agent packaging findings (2026-07-16)

- The currently active scheduling flow is already partitioned by real service boundaries, but most displayed Agent names are workflow-event labels assembled by `scheduling/order_intake.py`; only some generic agents are represented in `agents/registry.py`.
- Safe refactor path: introduce thin Agent wrappers that call the existing `OrderIntakeService`, `SchedulingAnalysisService`, `SchedulingExecutionService`, `ProductionIssueService`, and model-analysis service. Do not move database logic into wrappers or change Spring API contracts.
- The immediate target is a sequential orchestration facade with the same workflow event schema and order. LangGraph `StateGraph` migration remains a later structural change, not part of this compatibility-first packaging step.
- The MES scheduling write facade remains atomic across lock, task creation, Gantt scheduling and dispatch synchronization. The wrappers for capacity, dispatch and validation therefore project their own responsibilities from the one write result instead of issuing duplicate writes.

## Full MES Agent inventory findings (2026-07-16)

- The system exposes business-capable modules beyond planning: inventory inbound/outbound and barcode/package, shop-floor feedback, IQC/IPQC/OQC/RQC quality workbenches, equipment account/check/maintenance/repair, Andon, traceability, reports, integration, factory/process/master data and calendar resources.
- The real lifecycle continues past the current scheduling Agents: `MATERIAL_ISSUED → IN_PROGRESS → QC_PENDING → QC_PASSED/QC_FAILED → COMPLETED`. Existing services provide hard gates for feedback, IPQC and finished-goods receipt, so these are the strongest next Agent candidates.
- Candidate Agents must be classified by live MES tool boundary rather than the presence of a UI page. Existing generic registry entries are not sufficient evidence of a safe, real Agent capability.
- Current FastAPI real-read gateway is deliberately narrow: only work-order pipeline, BOM/route, kitting, production tasks, dispatches, traceability and RAG are safe Agent tools today. New business Agents need dedicated side-effect-free snapshots and, separately, approval-gated write facades before they can be activated.
- The backend has concrete controller/service support for production feedback, IPQC/IQC/OQC/RQC, finished-product receipt, material receipt/issue/returns/sales, equipment workbench/repair/maintenance, Andon assignment/closure, reverse traceability, calendar/teams/shifts, master-data/BOM/SOP, reporting and integration. These are viable Agent domains; the priority must follow lifecycle continuity and permissions, not controller count.
- The next lifecycle package has strong existing gates: feedback validates `MATERIAL_ISSUED/IN_PROGRESS/QC_PENDING` and advances through `IN_PROGRESS → QC_PENDING`; IPQC advances to `QC_PASSED/QC_FAILED`; finished-product receipt requires `QC_PASSED`, writes inventory, advances to `COMPLETED` and synchronizes traceability. This supports a safe production-to-quality-to-receipt Agent subgraph.
- Equipment workbench and Andon board already expose aggregation-ready operational data. Reverse traceability has useful business data but its current work-order endpoint invokes a sync service, so a new Agent-facing read-only snapshot must be added instead of exposing that endpoint directly.

## Standalone business Agent packaging findings (2026-07-16)

- Seven business Agents are now independently packaged under `agent-system/backend/app/business_agents/`: production execution, quality management, warehouse logistics, traceability, equipment maintenance, Andon response and operations insight. They are intentionally not registered with the default scheduling/main-chat workflow.
- The package base permits only `SpringBootClient` GET-allowlisted calls with the caller's MES JWT. A data-source failure propagates as an error; no Agent is allowed to return demo facts, inferred facts or a model-generated fallback.
- Live verification exposed two stale generic page endpoints: `/api/quality/records` and `/api/equipment/devices` both returned 500. The new packages instead use the active Yunshu quality pending/analytics and equipment workbench endpoints, which completed the real-data verification.
- `verify_business_agent_packages.py --password 123456` read one real work order plus real quality, inventory, equipment, Andon and dashboard facts for all seven packages and reported `writes_created: 0`.

## Quality-management workflow planning findings (2026-07-16)

- The existing `QualityManagementAgent` is already a truthful read-only wrapper: it obtains live `quality_pending`, `quality_analytics_summary` and, when a work order is supplied, `agent_work_order_trace` through the caller's MES JWT. It is not yet connected to the default main-chat workflow.
- The local `Qwen3-0.6B` runtime supports thread-safe token streaming through `stream_generate`; the existing external client supports configured DeepSeek/Qwen/GLM calls. This permits a bounded hybrid design: external API only at intent routing and final decision summary, local Qwen for short, fact-constrained intermediate explanations.
- No quality workflow implementation or quality write API has been enabled in this planning pass.

## Quality-management workflow implementation findings (2026-07-16)

- Added a dedicated JWT-protected Agent read facade at `/api/agent/read/quality/overview` and `/api/agent/read/work-orders/{workOrderNo}/quality`. It aggregates the live IQC/PQC/OQC/RQC pending queue, summary, trend, defects, level distribution and type statistics without calling any quality write or lifecycle service.
- `QualityManagementAgent` now reads this bounded fact package through the existing GET-only Spring Boot allowlist. The main-chat quality workflow is still read-only: the only database writes are the established user-owned `agent_session` / `agent_message` audit records.
- The quality SSE route emits five ordered workflow events: external intent explanation, live fact collection, deterministic risk rules, local Qwen3-0.6B micro-analysis and external final summary. External prompts use role-scoped Qdrant SOP context only as non-authoritative explanation material.
- End-to-end test with the real phrase `帮我分析今天的质量情况` returned 5 workflow events, 43 local-Qwen stream fragments and 338 external-summary stream fragments. The final summary came from DeepSeek V4-Pro, the local card was marked `local_qwen`, and the persisted session retained all five Agent messages plus the user message.

## RAG runtime knowledge expansion findings (2026-07-16, in progress)

- Existing Qdrant ingestion is source-governed: `initial_sources.py` registers explicit Markdown sources, `ingestion.py` chunks by headings/paragraphs, and every point is tagged with document, role scope, lifecycle states, review status, version and source path.
- The approved collection must remain explanatory and versioned. It must not contain live orders, inventory, quality metrics, user conversations, API tokens, or transient UI copy; those belong to JWT-protected MES tools and MySQL.
- Recent implementation adds material knowledge not yet represented in the static corpus: latest-order scheduling confirmation, the sequential scheduling sub-Agent contract, quality daily analysis, quality report continuation/export, and seven standalone business-Agent read boundaries.
- The next corpus additions will therefore be curated implementation cards rather than raw code dumps or future-only plans. Each card will explicitly identify live MES source tables/endpoints, write/approval limits, and whether it is actively routed or independently packaged.
- Six curated runtime cards have now been authored under `agent-system/knowledge/agent_runtime/`: governance, intelligent scheduling, quality analysis, quality reporting/export, business sub-Agent inventory, and real-time data/Agent contracts. They are registered as `RAG-AGENT-*` sources with role and lifecycle filters; the unmodified collection baseline is 130 points before ingestion.
- Ingestion completed in the project `pytorch` Python environment (the shell-default interpreter lacks `sentence_transformers`). The Qdrant collection is green at 170 points: 40 newly embedded runtime chunks, while the prior governed documents remain registered. Existing 6-case workflow regression and 5 new Agent-runtime retrieval checks passed. A `QUALITY_INSPECTOR` retrieval did not return the scheduling-runtime card, confirming the intended role filter.

## Professional orchestration core findings (2026-07-16, in progress)

- Existing production scheduling and quality paths must remain untouched. The safe first delivery is therefore an opt-in orchestration core that owns only plan/state/checkpoint/audit data and delegates business work to the existing wrapped workflows.
- MySQL already provides `agent_graph_checkpoint` and `agent_runtime_state`; `AgentPlatformStore` can persist owner-scoped graph checkpoints without accessing MES production tables.
- `langgraph` is not installed in the active `pytorch` environment and is absent from `requirements.txt`. A pinned dependency and compatibility verification are required before a real `StateGraph` can be compiled.
- The existing scheduling wrapper already preserves the intended business order: order intake → BOM/route → kitting risk → atomic execution → capacity projection → dispatch projection → validation → production issue. The new graph must not reorder, duplicate or independently invoke those writes.
- First sidecar verification exposed a Python 3.9 compatibility issue: LangGraph resolves `TypedDict` annotations while constructing a graph, so PEP 604 unions (`int | None`, then `bool | None`) in the state schema fail even with postponed annotations. The state fields have been changed to `Optional[...]`; no MES route or business write was invoked during either failed test.
- `SchedulingOrchestrationSidecar` now compiles a real LangGraph `StateGraph` with plan → approval gate → finalize transitions. Its verified states are `WAITING_APPROVAL/INTERRUPTED`, `READY_FOR_EXISTING_WORKFLOW/COMPLETED`, `CANCELLED`, and `OBSERVED`. It persists only user-owned Agent-platform rows and is not imported by current scheduling/quality entry routes.

## LangGraph-owned scheduling execution findings (2026-07-16, in progress)

- The single safe execution handoff point is `OrderIntakeService.create_order()`, called only after the existing front-end confirmation form submits `confirmed=true`. Its public endpoint and response already drive the persisted conversation cards, so the new graph must return the same result shape.
- The existing sequential wrapper gives the exact preservation order: OrderIntake → BOM/Route → KittingRisk → (LOW only) KittingExecution → Capacity → Dispatch → Validation → ProductionIssue → stable event projection. The graph can delegate each wrapper as a node without moving any underlying domain transaction.
- The actual approval UI remains the existing form confirmation in this release. The graph begins only after that approval, so it can take over execution without changing the current screen, request or API contract. Native LangGraph `interrupt()` migration remains a later compatible step because current confirmation is already implemented and persisted outside the graph.
- `agent_graph_checkpoint` has a stable status contract (`RUNNING/INTERRUPTED/COMPLETED/FAILED/CANCELLED`) suitable for serializable node-progress audit. A direct `BaseCheckpointSaver` adapter is not required for this first execution handoff, and should be introduced separately with migration tests rather than risking the existing confirmed-order path.
- The first end-to-end graph verification completed the business chain and returned a valid graph audit, but its assertion queried the intent-routing trace instead of the confirmed-execution graph thread. The verifier is being corrected to use `graph_audit.thread_id` and clean that exact thread in all outcomes; this is a verifier-only correction, not a graph or business-flow failure.
- An unconfirmed form was initially allowed to enter the graph and then failed at the original order gateway. Although it created no business data, that differed from the former “reject before orchestration” behavior and left a failed audit checkpoint. `OrderIntakeService.create_order()` now performs the same `confirmed` guard before creating a graph run; only confirmed requests enter LangGraph.
- `SchedulingExecutionGraph` is now the real executor behind the confirmed-order entry point. It uses `StateGraph` nodes for all eight existing business Agents plus final event projection, has the LOW-risk conditional edge, returns the unchanged scheduling result plus `graph_audit`, and persists one user-owned checkpoint per node. Live regression reached `MATERIAL_ISSUED`, returned the original eight UI workflow events, persisted nine graph checkpoints ending in `COMPLETED`, and cleaned the test order, work order, inventory changes, messages and graph rows.
- Final RAG verification initially hit a PowerShell quoting error for a Chinese query; no query or write was performed. The encoded retry succeeded and returned `RAG-AGENT-SCHED-001`. Final database residual checks found zero `AGENT-VERIFY-*` orders/work orders and zero verifier graph/runtime rows; Qdrant is green at 171 points.

| Decision | Rationale |
|----------|-----------|
| 建立单一 `ProductionExecutionService` 作为写入编排层 | 避免 Planning、compat、production controller 分别修改同一业务状态。 |
| 用 `dispatch_id` 明确承载现场报工关联 | 能精确回写工序/工位级进度，并由其汇总任务与工单。 |
| 增加工单物料预留明细 | 支持准确释放，不再按 BOM 反推并可能误释放其他工单的库存。 |

## Resources

- `backend/mes-server/src/main/java/com/yunshu/mes/planning/compat/repository/ProTaskRepository.java`
- `backend/mes-server/src/main/java/com/yunshu/mes/production/compat/service/ProFeedbackService.java`
- `backend/mes-server/src/main/java/com/yunshu/mes/planning/controller/KittingController.java`
- `backend/mes-server/src/main/java/com/yunshu/mes/production/controller/ProductionCompletionController.java`

## GitHub upload inventory
- Actual path D:\own_groceries\MES. Existing origin points to yunshu-mes; requested target yunshu-mes-all has no refs.
- Includes local runtimes, dependencies, models and a 2 GiB incomplete download. Git LFS available; gh CLI unavailable.


### GitHub upload verification
- 3,172 unmodified files passed SHA-256 comparison with source. Three files have credential placeholders in the upload copy; .gitattributes adds LFS rules.
- Windows sandbox prevented credential and LFS subprocess operations; approved Git commands outside sandbox completed successfully.

