# 排产任务到现场派工规则

> 权威来源：`planning/compat/controller/ProTaskController.java`、`planning/compat/service/ProTaskService.java`、`planning/compat/service/DispatchSyncService.java`、`planning/compat/repository/ProTaskRepository.java`  
> 适用范围：黄金流程第 4–5 步（甘特排产、工序派工）；版本 `0.1`；生效日期 `2026-07-15`。

## 主键与调用入口

排产对象是 `production_task`，主键为 `task_id`；它用 `work_order_id` 回连 `work_order`，并可绑定 `step_id`、`workstation_id`、计划数量和开始/结束时间。前端甘特页面通过以下接口新增或修改排产任务：

- `POST /api/mes/pro/protask`：创建 `production_task`。
- `PUT /api/mes/pro/protask`：更新 `production_task`。
- `GET /api/mes/pro/protask/listTaskListByWorkorder?workorderId={id}`：查询工单实际排产任务。

排产保存后，`ProTaskService` 会刷新该工单的已排产数量；只有任务真实保存成功后，才会尝试进入派工同步。

## 从 production_task 到 dispatch_task

`DispatchSyncService.syncFromProductionTask(taskId)` 只在配置 `mes.workflow.auto-dispatch=true` 时执行。它以 `production_task.task_id` 查询任务，并以 `dispatch_task.task_id` 保持一对一幂等关系：已有派工则更新，不存在才插入。

派工记录写入 `dispatch_task` 的核心关联是：`task_id`、`work_order_id`、`step_id`、`station_id`、计划数量和操作员。派工单号规则是 `DT-` 加排产任务号；若排产任务没有 `step_id`，服务会从该工单的 `work_order.route_id → process_route_step` 中取首道工序。默认操作员仅是当前实现的兜底值，不能被 Agent 当作“已完成正式人员派工”。

同步成功且存在 `work_order_id` 时，工单生命周期被推进到 `SCHEDULED`。任务状态为 `RUNNING` 或 `NORMAL` 时，自动生成的派工初始状态仍是 `CREATED`；只有任务为 `COMPLETED` 或 `FINISHED` 时才同步为 `COMPLETED`。

## 真实业务边界

`auto-dispatch` 是配置开关，而非业务事实。排产保存并不保证一定已生成派工：开关关闭、任务缺少可解析工序、保存失败或人员/工位信息不完整时，都不能声称现场已具备可执行派工。

Agent 回答“为什么排产后没有派工”时，应读取实际 `production_task` 和 `dispatch_task` 结果，并检查自动派工配置；只能给出缺口和下一步建议，不得自行假设或虚构 `dispatch_task`。

## 下游连接

已生成的 `dispatch_task` 是现场报工的优先关联对象。报工的 `taskId` 可以传派工 ID 或排产任务 ID，后端会先按 `dispatch_id` 查找，再回退到按 `task_id` 查找。因此后续报工、进度和追溯必须保留这两个 ID 的实际映射，不能混用为同一个业务主键。
