# MES Development Progress — Phase 1 Archive

## Date: 2026-07-08

---

## Backend (25 Controllers, 223 Endpoints)

| Module | Controllers | Status |
|--------|------------|--------|
| System | UserController, RoleController, DepartmentController, OperationLogController | ✅ |
| Auth | AuthController (JWT login/token), JwtAuthFilter (enforce: ON) | ✅ |
| Factory | FactoryController (workshop/line/station/shift CRUD) | ✅ |
| Master Data | MasterDataController (product/material/BOM/uom CRUD) + BomItem + ProductRoute | ✅ |
| Process | ProcessController (step/route CRUD) + ProcessRouteStep | ✅ |
| Planning | PlanningController (order/work_order/task/dispatch CRUD) | ✅ |
| Production | ProductionController (SN/report/completion CRUD) | ✅ |
| Quality | QualityController (task/defect/rework CRUD) + QualityRecordController + QualityReleaseController | ✅ |
| Inventory | InventoryController (warehouse/batch/transaction) + MaterialRequisition + MaterialIssue + MaterialReturn | ✅ |
| Equipment | EquipmentController (device/repair CRUD) + EquipmentMaintenanceController | ✅ |
| Andon | AndonController (type/reason/event list) + AndonEventWriteController (create/close) | ✅ |
| Barcode | BarcodeController (type/rule/template CRUD) | ✅ |
| Traceability | TraceabilityController | ✅ |
| Reporting | ReportingController | ✅ |
| Dashboard | DashboardController | ✅ |
| Integration | IntegrationController | ✅ |

## Frontend (33 Functional Pages)

| Role | Pages |
|------|-------|
| **Manager (admin)** | Department, Users, Roles, Permissions, Logs (5 pages) |
| **Prod Supervisor (supervisor)** | Dashboard, Orders, Work Orders, Tasks, Dispatch, Workshops, Lines, Stations, Shifts, Products, Materials, BOMs, Routes, Reports, Trace, Andon (16 pages) |
| **Warehouse Clerk (warehouse)** | Dashboard, Batches, Requisitions, Issues, Returns, Warehouses, Andon (7 pages) |
| **Quality Inspector (quality)** | Quality Records + Release, Andon (2 pages) |
| **Equipment Maintainer (repair)** | Devices, Maintenance, Line Monitor, Andon (4 pages) |
| **Line Operator (worker)** | Dashboard, Andon (2 pages) |

## Role-Based Access Control

- 36 permissions in `sys_permission` across 12 resource types
- 6 roles with 3-36 permissions each (63 role_permission records)
- Frontend sidebar computed from user role
- Route guards check `allowedRoles` meta
- Backend JWT enforcement enabled

## Critical Bugs Fixed (8 repository column name mismatches)

| File | Wrong | Correct |
|------|-------|---------|
| ProductRepository | `category` | `product_category` |
| BomRepository | `bom_version` | `version_no` |
| MaterialRepository | `is_critical` | `is_key_material` |
| RouteRepository | `route_version` | `version_no` |
| StepRepository | `standard_hours` | `standard_time_sec` |
| DispatchTaskRepository | `assignee_id` | `operator_id` |
| ProductionTaskRepository | `plan_qty` | `task_qty` |
| CustomerOrderRepository | cross-table JOIN fix | customer_order_item |

## Infrastructure

- `start.cmd` — interactive control panel (start/restart/health check)
- `admin-glass.css` — unified light theme
- `CrudManager.vue` — reusable table + filter + modal component
- Local MySQL (root/praline on localhost:3306)
- Flyway migrations (V1-V4, disabled; schema already in place)

---

## Phase 2 — Critical Gaps to Address

Based on simulated production scenarios, 4 features block real-world usability:

1. **Kitting Analysis** (`kitting_analysis` + `material_shortage`)
2. **Inventory Locking** (`inventory_batch.locked_qty`)
3. **Product Material Binding** (`product_material_binding`)
4. **Andon Task Flow** (`andon_task` + `andon_notice`)
