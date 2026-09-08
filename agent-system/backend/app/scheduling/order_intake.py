"""The first real scheduling sub-agent: authenticated customer-order intake."""

from __future__ import annotations

from typing import Any

import httpx

from app.core.config import settings
from app.core.security import Principal
from app.schemas import SchedulingOrderIntakeRequest


class OrderIntakeError(RuntimeError):
    pass


class OrderIntakePermissionError(OrderIntakeError):
    pass


class OrderIntakeService:
    """Small, explicitly scoped write gateway for the order-intake Agent.

    This deliberately does not extend the read-tool gateway: it has one write
    action only, validates the selected product against MES master data, and
    forwards the caller's own JWT to the existing Order Center API.
    """

    permitted_roles = {"MANAGER", "TESTER", "PROD_SUPERVISOR"}

    @staticmethod
    def _ensure_permission(principal: Principal) -> None:
        if not set(principal.roles).intersection(OrderIntakeService.permitted_roles):
            raise OrderIntakePermissionError("当前角色无权接收排产订单")

    @staticmethod
    def _headers(principal: Principal, trace_id: str) -> dict[str, str]:
        headers = {"X-Trace-Id": trace_id}
        if principal.token:
            headers["Authorization"] = f"Bearer {principal.token}"
        return headers

    def _call(
        self,
        method: str,
        path: str,
        principal: Principal,
        trace_id: str,
        *,
        json_body: dict[str, Any] | None = None,
    ) -> Any:
        try:
            with httpx.Client(timeout=settings.companion_request_timeout_seconds) as client:
                response = client.request(
                    method,
                    f"{settings.spring_boot_base_url.rstrip('/')}{path}",
                    headers=self._headers(principal, trace_id),
                    json=json_body,
                )
                response.raise_for_status()
                payload = response.json()
        except httpx.HTTPStatusError as exc:
            if exc.response.status_code in (401, 403):
                raise OrderIntakePermissionError("MES 拒绝了当前订单操作") from exc
            try:
                detail = exc.response.json().get("message") or exc.response.json().get("detail")
            except Exception:
                detail = None
            raise OrderIntakeError(detail or "订单中心未能完成本次操作") from exc
        except httpx.HTTPError as exc:
            raise OrderIntakeError("订单中心暂时不可用") from exc
        return payload.get("data") if isinstance(payload, dict) and "data" in payload else payload

    def list_products(self, principal: Principal, trace_id: str) -> list[dict[str, Any]]:
        self._ensure_permission(principal)
        raw_products = self._call("GET", "/api/master-data/products", principal, trace_id)
        if not isinstance(raw_products, list):
            raise OrderIntakeError("产品主数据返回格式异常")
        products = []
        for product in raw_products:
            product_id = product.get("productId")
            product_name = str(product.get("productName") or "").strip()
            if product_id and product_name:
                products.append(
                    {
                        "product_id": int(product_id),
                        "product_name": product_name,
                        "product_code": str(product.get("productCode") or ""),
                    }
                )
        return products

    def get_latest_order(self, principal: Principal, trace_id: str) -> dict[str, Any]:
        """Return the first row shown by the real Order Center.

        ``/api/planning/orders`` is deliberately used instead of duplicating
        its SQL here.  Its stable ordering is ``created_at DESC, order_id
        DESC``, which is also the ordering users see in Order Center.
        """
        self._ensure_permission(principal)
        raw_orders = self._call("GET", "/api/planning/orders", principal, trace_id)
        if not isinstance(raw_orders, list) or not raw_orders:
            raise OrderIntakeError("订单中心当前没有可用于智能排产的订单")

        latest = raw_orders[0]
        product_id = latest.get("productId")
        order_qty = latest.get("orderQty")
        delivery_date = str(latest.get("deliveryDate") or "").strip()
        if not latest.get("orderId") or not latest.get("orderNo") or not latest.get("customerName"):
            raise OrderIntakeError("最新订单缺少订单号或客户信息，无法进入智能排产")
        if not product_id or not order_qty or not delivery_date:
            raise OrderIntakeError("最新订单缺少产品、数量或交付日期，无法进入智能排产")

        return {
            "order_id": int(latest["orderId"]),
            "order_no": str(latest["orderNo"]),
            "customer_name": str(latest["customerName"]),
            "product_id": int(product_id),
            "product_name": str(latest.get("productName") or ""),
            "order_qty": int(order_qty),
            "delivery_date": delivery_date,
            "status": str(latest.get("status") or ""),
            "source": "MES_ORDER_CENTER_LATEST",
        }

    def create_order(
        self,
        request: SchedulingOrderIntakeRequest,
        principal: Principal,
        trace_id: str,
    ) -> dict[str, Any]:
        """Run the confirmed scheduling chain through the LangGraph executor.

        The graph delegates to the same packaged Agents, existing Spring Boot
        interfaces and domain transactions as the former sequential facade.
        The public request/response contract is intentionally unchanged.
        """
        # Preserve the former public-gateway behavior: an unconfirmed form is
        # rejected before any orchestration/checkpoint is created.
        if not request.confirmed:
            raise OrderIntakeError("请在订单表单中确认后再创建订单")
        # Local import avoids a circular dependency: OrderIntakeAgent inside
        # the graph calls this gateway only for its original order boundary.
        from app.orchestration.scheduling_execution_graph import scheduling_execution_graph

        return scheduling_execution_graph.run(
            request,
            principal,
            trace_id,
            order_intake_gateway=self,
        )

    def create_order_and_work_order(
        self,
        request: SchedulingOrderIntakeRequest,
        principal: Principal,
        trace_id: str,
    ) -> dict[str, Any]:
        self._ensure_permission(principal)
        if not request.confirmed:
            raise OrderIntakeError("请在订单表单中确认后再创建订单")

        products = self.list_products(principal, trace_id)
        selected = next((item for item in products if item["product_id"] == request.product_id), None)

        if request.source_order_id:
            # The confirmation card is intentionally only a confirmation UI.
            # Re-read the selected real order and ignore editable client
            # fields, which prevents stale data or an accidental duplicate
            # order from entering the existing downstream workflow.
            source = self._call(
                "GET",
                f"/api/planning/orders/{request.source_order_id}",
                principal,
                trace_id,
            )
            if not isinstance(source, dict) or not source.get("orderId"):
                raise OrderIntakeError("订单中心未找到待确认订单，请重新发起智能排产")

            source_product_id = source.get("productId")
            source_qty = source.get("orderQty")
            source_delivery = str(source.get("deliveryDate") or "").strip()
            if not source_product_id or not source_qty or not source_delivery:
                raise OrderIntakeError("待确认订单缺少产品、数量或交付日期，无法进入智能排产")

            selected = next((item for item in products if item["product_id"] == int(source_product_id)), None)
            if not selected:
                raise OrderIntakeError("待确认订单的产品已失效，请先在订单中心调整订单")

            work_orders = self._call("GET", "/api/planning/work-orders", principal, trace_id)
            existing = next(
                (
                    item for item in work_orders
                    if isinstance(item, dict) and int(item.get("orderId") or 0) == int(source["orderId"])
                ),
                None,
            ) if isinstance(work_orders, list) else None
            if existing:
                raise OrderIntakeError(
                    f"订单 {source.get('orderNo')} 已关联工单 {existing.get('workOrderNo') or ''}，"
                    "为避免重复排产，本次未再次创建工单"
                )

            order_id = int(source["orderId"])
            order_payload = {
                "orderNo": str(source.get("orderNo") or "").strip(),
                "customerName": str(source.get("customerName") or "").strip(),
                "productId": int(source_product_id),
                "orderQty": int(source_qty),
                "deliveryDate": source_delivery,
            }
            confirmed_order = self._call(
                "PUT",
                f"/api/planning/orders/{order_id}",
                principal,
                trace_id,
                json_body={**order_payload, "status": "CONFIRMED"},
            )
            if not isinstance(confirmed_order, dict) or not confirmed_order.get("orderId"):
                raise OrderIntakeError("订单确认失败，未启动智能排产")

            order_items = self._call("GET", f"/api/planning/orders/{order_id}/items", principal, trace_id)
            first_item = order_items[0] if isinstance(order_items, list) and order_items else {}
            work_order_no = f"WO-{order_payload['orderNo'].removeprefix('CO-')}"
            work_order = self._call(
                "POST",
                "/api/planning/work-orders",
                principal,
                trace_id,
                json_body={
                    "workOrderNo": work_order_no,
                    "productId": order_payload["productId"],
                    "orderId": order_id,
                    "orderItemId": first_item.get("orderItemId"),
                    "planQty": order_payload["orderQty"],
                    "status": "CONFIRMED",
                },
            )
            if not isinstance(work_order, dict) or not work_order.get("workOrderId"):
                raise OrderIntakeError("订单已确认，但工单生成失败")
            return {
                "order": {
                    "order_id": order_id,
                    "order_no": str(confirmed_order.get("orderNo") or order_payload["orderNo"]),
                    "customer_name": str(confirmed_order.get("customerName") or order_payload["customerName"]),
                    "product_id": order_payload["productId"],
                    "product_name": selected["product_name"],
                    "order_qty": order_payload["orderQty"],
                    "delivery_date": order_payload["deliveryDate"],
                    "status": str(confirmed_order.get("status") or "CONFIRMED"),
                    "source": "MES_ORDER_CENTER_LATEST",
                },
                "work_order": {
                    "work_order_id": int(work_order["workOrderId"]),
                    "work_order_no": str(work_order.get("workOrderNo") or work_order_no),
                    "status": str(work_order.get("status") or "CREATED"),
                },
            }

        if not selected:
            raise OrderIntakeError("所选产品不存在或已不可用，请重新选择")

        order_payload = {
            "orderNo": request.order_no.strip(),
            "customerName": request.customer_name.strip(),
            "productId": request.product_id,
            "orderQty": request.order_qty,
            "deliveryDate": request.delivery_date.isoformat(),
        }
        order = self._call(
            "POST",
            "/api/planning/orders",
            principal,
            trace_id,
            json_body={**order_payload, "status": "CREATED"},
        )
        if not isinstance(order, dict) or not order.get("orderId"):
            raise OrderIntakeError("订单中心未返回有效订单编号")
        order_id = int(order["orderId"])
        # One controlled form confirmation authorizes the scheduling Agent to
        # execute the same confirm-and-generate sequence as Order Center.
        confirmed_order = self._call(
            "PUT",
            f"/api/planning/orders/{order_id}",
            principal,
            trace_id,
            json_body={**order_payload, "status": "CONFIRMED"},
        )
        if not isinstance(confirmed_order, dict) or not confirmed_order.get("orderId"):
            raise OrderIntakeError("Order was created but could not be confirmed")

        order_items = self._call("GET", f"/api/planning/orders/{order_id}/items", principal, trace_id)
        first_item = order_items[0] if isinstance(order_items, list) and order_items else {}
        work_order_no = f"WO-{request.order_no.strip().removeprefix('CO-')}"
        work_order = self._call(
            "POST",
            "/api/planning/work-orders",
            principal,
            trace_id,
            json_body={
                "workOrderNo": work_order_no,
                "productId": request.product_id,
                "orderId": order_id,
                "orderItemId": first_item.get("orderItemId"),
                "planQty": request.order_qty,
                "status": "CONFIRMED",
            },
        )
        if not isinstance(work_order, dict) or not work_order.get("workOrderId"):
            raise OrderIntakeError("Order was confirmed but the work order could not be generated")
        return {
            "order": {
                "order_id": order_id,
                "order_no": str(confirmed_order.get("orderNo") or request.order_no),
                "customer_name": str(confirmed_order.get("customerName") or request.customer_name),
                "product_id": request.product_id,
                "product_name": selected["product_name"],
                "order_qty": request.order_qty,
                "delivery_date": request.delivery_date.isoformat(),
                "status": str(confirmed_order.get("status") or "CONFIRMED"),
            },
            "work_order": {
                "work_order_id": int(work_order["workOrderId"]),
                "work_order_no": str(work_order.get("workOrderNo") or work_order_no),
                "status": str(work_order.get("status") or "CREATED"),
            },
        }

order_intake_service = OrderIntakeService()
