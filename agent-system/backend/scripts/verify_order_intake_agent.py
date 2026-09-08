"""Verify the real, confirmation-gated Order Intake Agent without leaving test data."""

from __future__ import annotations

import argparse
import uuid
from pathlib import Path
import sys

import httpx


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))


def cleanup_test_order(order_id: int, work_order_id: int | None = None) -> None:
    """Remove only an order created by this verification script.

    The current generic Order Center DELETE endpoint does not cascade its item
    row, so the test performs the minimal child-then-parent cleanup directly.
    This is test cleanup, never Agent production behaviour.
    """
    from app.storage.mysql_store import conversation_store

    with conversation_store._connection() as connection:
        with connection.cursor() as cursor:
            if work_order_id:
                cursor.execute("SELECT issue_id FROM wm_issue_header WHERE workorder_id = %s", (work_order_id,))
                issue_ids = [int(row["issue_id"]) for row in cursor.fetchall()]
                for issue_id in issue_ids:
                    cursor.execute(
                        """
                        DELETE d FROM wm_item_consume_detail d
                        JOIN wm_item_consume_line l ON l.line_id = d.line_id
                        JOIN wm_item_consume c ON c.record_id = l.record_id
                        WHERE c.attr1 = %s
                        """,
                        (str(issue_id),),
                    )
                    cursor.execute(
                        """
                        DELETE l FROM wm_item_consume_line l
                        JOIN wm_item_consume c ON c.record_id = l.record_id
                        WHERE c.attr1 = %s
                        """,
                        (str(issue_id),),
                    )
                    cursor.execute("DELETE FROM wm_item_consume WHERE attr1 = %s", (str(issue_id),))
                    cursor.execute(
                        "DELETE FROM wm_transaction WHERE source_doc_type = 'IS' AND source_doc_id = %s",
                        (issue_id,),
                    )
                    cursor.execute("DELETE FROM wm_issue_detail WHERE issue_id = %s", (issue_id,))
                    cursor.execute("DELETE FROM wm_issue_line WHERE issue_id = %s", (issue_id,))
                    cursor.execute("DELETE FROM wm_issue_header WHERE issue_id = %s", (issue_id,))
                cursor.execute("DELETE FROM dispatch_task WHERE work_order_id = %s", (work_order_id,))
                cursor.execute("DELETE FROM production_task WHERE work_order_id = %s", (work_order_id,))
                cursor.execute("DELETE FROM material_shortage WHERE analysis_id IN (SELECT analysis_id FROM kitting_analysis WHERE work_order_id = %s)", (work_order_id,))
                cursor.execute("DELETE FROM kitting_analysis WHERE work_order_id = %s", (work_order_id,))
                cursor.execute("DELETE FROM work_order_bom WHERE work_order_id = %s", (work_order_id,))
                cursor.execute("DELETE FROM work_order WHERE work_order_id = %s", (work_order_id,))
            cursor.execute("DELETE FROM customer_order_item WHERE order_id = %s", (order_id,))
            cursor.execute("DELETE FROM customer_order WHERE order_id = %s", (order_id,))


def snapshot_inventory(product_id: int) -> tuple[list[tuple[int, object, object]], list[tuple[int, object, object]]]:
    """Capture both inventory ledgers before Agent writes so cleanup leaves no business delta."""
    from app.storage.mysql_store import conversation_store

    with conversation_store._connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute(
                """
                SELECT ib.batch_id, ib.available_qty, ib.locked_qty
                FROM inventory_batch ib
                WHERE ib.material_id IN (
                    SELECT bi.material_id FROM bom_item bi WHERE bi.bom_id = (
                        SELECT bom_id FROM bom
                        WHERE product_id = %s AND status IN ('RELEASED', 'ENABLED')
                        ORDER BY bom_id DESC LIMIT 1
                    )
                )
                """,
                (product_id,),
            )
            batches = [
                (int(row["batch_id"]), row["available_qty"], row["locked_qty"])
                for row in cursor.fetchall()
            ]
            cursor.execute(
                """
                SELECT stock.material_stock_id, stock.quantity_onhand, stock.quantity_reserved
                FROM wm_material_stock stock
                WHERE stock.item_id IN (
                    SELECT bi.material_id FROM bom_item bi WHERE bi.bom_id = (
                        SELECT bom_id FROM bom
                        WHERE product_id = %s AND status IN ('RELEASED', 'ENABLED')
                        ORDER BY bom_id DESC LIMIT 1
                    )
                )
                """,
                (product_id,),
            )
            stocks = [
                (int(row["material_stock_id"]), row["quantity_onhand"], row["quantity_reserved"])
                for row in cursor.fetchall()
            ]
            return batches, stocks


def restore_inventory(snapshot: tuple[list[tuple[int, object, object]], list[tuple[int, object, object]]]) -> None:
    batches, stocks = snapshot
    if not batches and not stocks:
        return
    from app.storage.mysql_store import conversation_store

    with conversation_store._connection() as connection:
        with connection.cursor() as cursor:
            for batch_id, available_qty, locked_qty in batches:
                cursor.execute(
                    "UPDATE inventory_batch SET available_qty = %s, locked_qty = %s WHERE batch_id = %s",
                    (available_qty, locked_qty, batch_id),
                )
            for stock_id, onhand, reserved in stocks:
                cursor.execute(
                    "UPDATE wm_material_stock SET quantity_onhand = %s, quantity_reserved = %s WHERE material_stock_id = %s",
                    (onhand, reserved, stock_id),
                )


def cleanup_test_chat(session_id: str | None, trace_id: str | None, graph_thread_id: str | None = None) -> None:
    if not session_id and not trace_id and not graph_thread_id:
        return
    from app.storage.mysql_store import conversation_store

    with conversation_store._connection() as connection:
        with connection.cursor() as cursor:
            if session_id and session_id.isdigit():
                cursor.execute("DELETE FROM agent_message WHERE session_id = %s", (session_id,))
                cursor.execute("DELETE FROM agent_session WHERE session_id = %s", (session_id,))
            if trace_id:
                cursor.execute(
                    "DELETE FROM agent_runtime_state WHERE state_type = 'TRACE' AND state_key = %s",
                    (trace_id,),
                )
            if graph_thread_id:
                cursor.execute(
                    "DELETE FROM agent_runtime_state WHERE state_type = 'SCHEDULING_GRAPH_RUN' AND state_key = %s",
                    (graph_thread_id,),
                )
                cursor.execute(
                    "DELETE FROM agent_graph_checkpoint WHERE thread_id = %s",
                    (graph_thread_id,),
                )


def load_scheduling_graph_audit(thread_id: str) -> dict:
    """Read only the graph audit rows created by this verifier's trace."""
    from app.storage.mysql_store import conversation_store

    with conversation_store._connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute(
                """
                SELECT checkpoint_status, graph_code, graph_version, state_json
                FROM agent_graph_checkpoint
                WHERE thread_id = %s
                ORDER BY checkpoint_id ASC
                """,
                (thread_id,),
            )
            rows = cursor.fetchall()
    return {"count": len(rows), "rows": rows}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--mes-url", default="http://127.0.0.1:8080")
    parser.add_argument("--agent-url", default="http://127.0.0.1:8090")
    parser.add_argument("--username", default="supervisor")
    parser.add_argument("--password", required=True)
    parser.add_argument("--cleanup-order-id", type=int)
    parser.add_argument("--cleanup-work-order-id", type=int)
    args = parser.parse_args()

    if args.cleanup_order_id:
        cleanup_test_order(args.cleanup_order_id, args.cleanup_work_order_id)
        print({"cleaned_order_id": args.cleanup_order_id})
        return

    created_order_id: int | None = None
    created_work_order_id: int | None = None
    flow_session_id: str | None = None
    flow_trace_id: str | None = None
    graph_thread_id: str | None = None
    execution_trace_id = f"SCHEDULING-GRAPH-VERIFY-{uuid.uuid4().hex[:16]}"
    inventory_snapshot: tuple[list[tuple[int, object, object]], list[tuple[int, object, object]]] = ([], [])
    # The full real chain includes two bounded external-model calls and two
    # local RAG lookups.  Keep the verifier patient without changing runtime
    # business behavior or masking endpoint errors.
    with httpx.Client(timeout=120.0) as client:
        login = client.post(
            f"{args.mes_url}/api/auth/login",
            json={"username": args.username, "password": args.password},
        )
        login.raise_for_status()
        headers = {"Authorization": f"Bearer {login.json()['data']['token']}"}
        try:
            agent_flow = client.post(
                f"{args.agent_url}/api/agent/chat",
                headers=headers,
                json={"message": "我有一个新订单，需要排产。"},
            )
            agent_flow.raise_for_status()
            flow = agent_flow.json()
            flow_session_id, flow_trace_id = str(flow["session_id"]), flow["trace_id"]
            assert flow["intent"] == "scheduling_order_intake", flow
            assert flow["interaction"].get("type") == "scheduling_order_intake", flow
            intent_analysis = flow.get("model_analysis", {}).get("intent") or {}
            assert intent_analysis.get("source") == "llm_api", flow
            assert intent_analysis.get("rag", {}).get("source") == "QDRANT_RAG", flow
            assert intent_analysis.get("rag", {}).get("citations"), flow

            products_response = client.get(
                f"{args.agent_url}/api/agent/scheduling/order-intake/products", headers=headers
            )
            products_response.raise_for_status()
            products = products_response.json()["products"]
            assert products, "No real MES products are available for the order-intake form"
            product = products[0]
            inventory_snapshot = snapshot_inventory(int(product["product_id"]))
            payload = {
                "session_id": flow_session_id,
                "order_no": f"AGENT-VERIFY-{uuid.uuid4().hex[:10].upper()}",
                "customer_name": "Agent 验证客户",
                "product_id": product["product_id"],
                "order_qty": 1,
                "delivery_date": "2030-01-01",
            }
            denied = client.post(
                f"{args.agent_url}/api/agent/scheduling/order-intake/orders",
                headers=headers,
                json={**payload, "confirmed": False},
            )
            assert denied.status_code == 422, denied.text

            created = client.post(
                f"{args.agent_url}/api/agent/scheduling/order-intake/orders",
                headers={**headers, "X-Trace-Id": execution_trace_id},
                json={**payload, "confirmed": True},
            )
            created.raise_for_status()
            result = created.json()
            order = result["order"]
            created_order_id = int(order["order_id"])
            work_order = result["work_order"]
            created_work_order_id = int(work_order["work_order_id"])
            analysis = result["analysis"]
            execution = result.get("scheduling_execution")
            production_issue = result.get("production_issue_execution")
            workflow_events = result.get("workflow_events") or []
            conversation_messages = result.get("conversation_messages") or []
            graph_audit = result.get("graph_audit") or {}
            graph_thread_id = str(graph_audit.get("thread_id") or "") or None
            assert order["status"] == "CONFIRMED", order
            assert order["product_id"] == product["product_id"], order
            assert analysis["bom_route_agent"]["bom_ready"], analysis
            assert analysis["bom_route_agent"]["route_ready"], analysis
            assert analysis["kitting_risk_agent"]["all_sufficient"], analysis
            assert execution and execution["lifecycleStatus"] == "SCHEDULED", result
            assert execution["productionTaskCount"] == analysis["bom_route_agent"]["process_step_count"], result
            assert execution["dispatchTaskCount"] == execution["productionTaskCount"], result
            assert production_issue and production_issue["readyForShopFloor"], result
            assert production_issue["lifecycleStatus"] == "MATERIAL_ISSUED", result
            assert production_issue["stockTransactionCount"] == production_issue["pickDetailCount"], result
            assert production_issue["consumeDetailCount"] == production_issue["pickDetailCount"], result
            assert graph_audit.get("graph_code") == "MES_SCHEDULING_EXECUTION", graph_audit
            assert graph_audit.get("status") == "COMPLETED", graph_audit
            assert graph_audit.get("persistence_status") == "ACTIVE", graph_audit
            assert [item.get("agent_name") for item in graph_audit.get("node_records") or []] == [
                "OrderIntakeAgent", "BomRouteAgent", "KittingRiskAgent", "KittingExecutionAgent",
                "CapacitySchedulingAgent", "DispatchExecutionAgent", "SchedulingValidationAgent",
                "ProductionIssueAgent", "MainAgent",
            ], graph_audit
            assert graph_thread_id, graph_audit
            assert graph_thread_id == f"scheduling-execution:{execution_trace_id}", graph_audit
            persisted_graph = load_scheduling_graph_audit(graph_thread_id)
            assert persisted_graph["count"] == 9, persisted_graph
            assert persisted_graph["rows"][-1]["checkpoint_status"] == "COMPLETED", persisted_graph
            assert all(row["graph_code"] == "MES_SCHEDULING_EXECUTION" for row in persisted_graph["rows"]), persisted_graph
            assert [event.get("agent") for event in workflow_events] == [
                "OrderIntakeAgent", "BomRouteAgent", "KittingRiskAgent", "KittingExecutionAgent",
                "CapacitySchedulingAgent", "DispatchExecutionAgent", "SchedulingValidationAgent",
                "ProductionIssueAgent",
            ], workflow_events
            assert result.get("session_id") == flow_session_id, result
            assert len(conversation_messages) == len(workflow_events) + 1, result
            assert [item.get("agent_name") for item in conversation_messages[:-1]] == [
                event.get("agent") for event in workflow_events
            ], conversation_messages
            assert conversation_messages[-1].get("agent_name") == "云枢小智", conversation_messages[-1]
            final_analysis = conversation_messages[-1].get("model_analysis") or {}
            assert final_analysis.get("source") == "llm_api", conversation_messages[-1]
            assert final_analysis.get("rag", {}).get("source") == "QDRANT_RAG", conversation_messages[-1]
            assert final_analysis.get("rag", {}).get("citations"), conversation_messages[-1]

            history = client.get(f"{args.agent_url}/api/agent/sessions/{flow_session_id}", headers=headers)
            history.raise_for_status()
            persisted_messages = history.json()["messages"]
            persisted_tail = persisted_messages[-len(conversation_messages):]
            assert [item.get("content") for item in persisted_tail] == [
                item.get("content") for item in conversation_messages
            ], persisted_messages
            assert [item.get("agent_name") for item in persisted_tail] == [
                item.get("agent_name") for item in conversation_messages
            ], persisted_messages

            read_back = client.get(f"{args.mes_url}/api/planning/orders/{created_order_id}", headers=headers)
            read_back.raise_for_status()
            persisted = read_back.json()["data"]
            assert persisted["orderNo"] == payload["order_no"], persisted
            assert persisted["status"] == "CONFIRMED", persisted
            print(
                {
                    "real_product_options": len(products),
                    "agent_intent": flow["intent"],
                    "unconfirmed_write": denied.status_code,
                    "created_order_id": created_order_id,
                    "created_status": order["status"],
                    "work_order_id": created_work_order_id,
                    "lifecycle_status": execution["lifecycleStatus"],
                    "production_task_count": execution["productionTaskCount"],
                    "dispatch_task_count": execution["dispatchTaskCount"],
                    "issue_id": production_issue["issueId"],
                    "issue_status": production_issue["issueStatus"],
                    "post_issue_lifecycle_status": production_issue["lifecycleStatus"],
                    "ready_for_shop_floor": production_issue["readyForShopFloor"],
                    "workflow_event_count": len(workflow_events),
                    "langgraph_checkpoint_count": persisted_graph["count"],
                    "langgraph_checkpoint_status": persisted_graph["rows"][-1]["checkpoint_status"],
                    "persisted_agent_cards": len(conversation_messages),
                    "conversation_history_readback": "passed",
                    "intent_model_api": intent_analysis.get("provider_label"),
                    "intent_rag_citations": len(intent_analysis.get("rag", {}).get("citations", [])),
                    "final_model_api": final_analysis.get("provider_label"),
                    "final_rag_citations": len(final_analysis.get("rag", {}).get("citations", [])),
                    "mes_readback": "passed",
                }
            )
        finally:
            if created_order_id:
                cleanup_test_order(created_order_id, created_work_order_id)
            restore_inventory(inventory_snapshot)
            cleanup_test_chat(flow_session_id, flow_trace_id, graph_thread_id)


if __name__ == "__main__":
    main()
