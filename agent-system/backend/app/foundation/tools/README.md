# Unified Real Read Tools

`app.foundation.tools` packages the project's already-working real read
gateway into a uniform Agent Tool contract.

## Packaged tools

| Tool | Real source | Required input |
|---|---|---|
| `query_work_order_pipeline` | Spring Boot Agent Read API | `work_order_no` |
| `query_kitting_snapshot` | Spring Boot Agent Read API | `work_order_id` |
| `query_production_task_snapshot` | Spring Boot Agent Read API | `work_order_id` |
| `query_dispatch_snapshot` | Spring Boot Agent Read API | `work_order_id` |
| `query_work_order_trace` | Spring Boot Agent Read API | `work_order_no` |
| `search_knowledge` | Qdrant, approved role-filtered knowledge | `query` |

## Safety boundary

- Every call delegates to the existing `app.tools.mes_read.MesReadToolGateway`.
- The gateway retains schema validation, JWT forwarding, Spring Boot resource
  allowlisting, role enforcement and source attribution.
- This package contains no SQL, no business write method, no mock-tool import,
  no dynamic script runner and no direct call from scheduling/quality flows.
- The optional API is `/api/agent/foundation/tools/real/*`; it remains outside
  the current workflow routes.  Listing a Tool does not grant a model any
  additional authority.

## Future extension

Controlled-write tools must be packaged in a separate module only after each
one has an explicit approval contract, idempotency policy, role gate and a
regression suite proving that it preserves the current MES transaction path.
