"""Opt-in professional orchestration primitives.

Nothing in this package is imported by the current scheduling or quality
entry points.  It is a compatibility-first LangGraph sidecar that can be
validated and observed before any production workflow chooses to delegate to
it.
"""

from app.orchestration.scheduling_sidecar import SchedulingOrchestrationSidecar
from app.orchestration.scheduling_execution_graph import SchedulingExecutionGraph

__all__ = ["SchedulingExecutionGraph", "SchedulingOrchestrationSidecar"]
