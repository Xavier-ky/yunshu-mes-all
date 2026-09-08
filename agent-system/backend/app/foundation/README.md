# Agent Foundation Extensions

This package fills the professional-Agent capabilities that should exist
around a LangGraph workflow without changing MES business behavior.

## What is active

| Capability | Storage / source | Safety boundary |
|---|---|---|
| Short-term memory | `agent_runtime_state` with an 8-hour TTL | Owner + session scoped; currently not injected into prompts |
| Long-term memory | `agent_runtime_state` | Explicit/reviewable records only; no autonomous learning or prompt injection |
| Tool catalogue | Static source-controlled Python descriptors | Describes capabilities; never grants permissions or invokes tools |
| Unified real read Tools | `app.foundation.tools` -> existing MES/RAG read gateway | Typed contract only; no SQL, no mock handler, no write method |
| Post-run reflection | `agent_graph_checkpoint` -> `agent_runtime_state` | Deterministic facts only; no chain-of-thought and no MES write |
| Approval pause/resume | Existing `SchedulingOrchestrationSidecar` | Available for future graph interrupts; current order confirmation is unchanged |

## Intentional exclusions

- RAG remains **knowledge retrieval**, not long-term user memory.
- No agent is allowed to run arbitrary scripts, call external web search, or
  analyze images through this package.
- No current scheduling, quality, report, chat, Spring Boot API or transaction
  is imported or changed by these services.
- The catalogue's `CONTROLLED_WRITE` label explains an existing guarded
  capability; it does not add a route or bypass existing confirmation,
  authorization or business validation.

## Read-only operational APIs

- `GET /api/agent/foundation/tools`
- `GET /api/agent/foundation/tools/real`
- `POST /api/agent/foundation/tools/real/{tool_name}`
- `GET /api/agent/foundation/memory/short/{session_id}`
- `GET /api/agent/foundation/memory/long`
- `POST /api/agent/foundation/reflections/{thread_id}`
- `GET /api/agent/foundation/reflections/{thread_id}`

All endpoints require the existing JWT principal.  The only POST creates an
Agent-platform reflection record from an existing owner-owned graph checkpoint
or invokes one explicitly packaged **read-only** Tool. Neither operation
replays, resumes or changes the MES business workflow.
