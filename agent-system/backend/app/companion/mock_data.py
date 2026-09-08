from __future__ import annotations

from typing import Any


def read_mock(resource: str) -> Any:
    data = {
        "dashboard": {"alerts": 3, "qualityPassRate": 97.6, "equipmentFault": 1},
        "work_orders": [{"workOrderNo": "WO20260708001", "status": "DELAYED"}],
        "inventory": [{"material": "电机壳体", "availableQty": 120, "lockedQty": 80}],
        "andon": [{"title": "电机异响", "status": "OPEN", "line": "A-01"}],
        "equipment": [{"deviceName": "总装线 A-01", "status": "RUNNING"}],
        "quality": [{"status": "PENDING", "defectType": "异响"}],
    }
    return data.get(resource, {})