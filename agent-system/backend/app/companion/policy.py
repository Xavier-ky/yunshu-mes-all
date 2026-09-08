from __future__ import annotations

from typing import Any


ROUTE_RESOURCES: tuple[tuple[str, str], ...] = (
    ("/app/planning", "work_orders"),
    ("/app/inventory", "inventory"),
    ("/app/andon", "andon"),
    ("/app/equipment", "equipment"),
    ("/app/quality", "quality"),
)


def select_resource(route_path: str) -> str:
    for prefix, resource in ROUTE_RESOURCES:
        if route_path.startswith(prefix):
            return resource
    return "dashboard"


def build_suggestion(route_title: str, resource: str, data: Any, source: str) -> tuple[str, str, list[dict[str, str]]]:
    title = route_title or "当前页面"
    labels = {
        "work_orders": ("计划与工单", "先核对工单状态和物料齐套，再决定是否需要调整排程。", "查看工单中心"),
        "inventory": ("库存与齐套", "先查看锁定库存和欠料批次，避免影响正在执行的工单。", "查看库存明细"),
        "andon": ("现场安灯", "优先处理未关闭的异常，并确认责任人已经接手。", "查看安灯事件"),
        "equipment": ("设备状态", "关注停机与待点检设备，避免让计划在现场失去支撑。", "查看设备台账"),
        "quality": ("质量任务", "优先查看待判定项目，避免质检积压阻塞后续放行。", "查看质量任务"),
        "dashboard": ("系统总览", "我已为你准备好当前页面的查看方向，需要时可以进入 AI 工作台继续分析。", "进入 AI 工作台"),
    }
    topic, message, action_label = labels[resource]
    count = len(data) if isinstance(data, list) else 0
    suffix = f" 当前读取到 {count} 条相关记录。" if count else ""
    if source == "mock":
        suffix += " 当前为模拟数据，仅用于界面预览。"
    return f"{title} · {topic}", message + suffix, [
        {"label": action_label, "type": "open_agent"},
        {"label": "稍后再看", "type": "dismiss"},
    ]