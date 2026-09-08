# 数据库开发实施建议

## 1. 当前设计的定位

这套数据库不是为了报告展示，而是为了后续真实开发使用。它覆盖 `MES.xlsx` 中列出的核心能力，但开发时需要分阶段落地。

整体结构分为三层：

- 基础主数据层：组织、用户、车间、产线、工位、产品、物料、BOM、工艺、条码。
- MES 业务交易层：订单、工单、任务、库存、报工、质检、安灯、设备、报表、接口。
- Agent 智能平台层：Agent 配置、会话、任务、工具、知识库、记忆、审批、审计。

## 2. 推荐开发顺序

### 阶段一：基础主数据

先开发这些表对应的 CRUD：

- `sys_department`
- `sys_user`
- `sys_role`
- `sys_permission`
- `workshop`
- `production_line`
- `workstation`
- `product`
- `material`
- `bom`
- `bom_item`
- `process_step`
- `process_route`
- `process_route_step`
- `product_route`
- `sop_file`

完成后，系统具备“知道生产什么、由谁生产、在哪里生产、按什么工艺生产”的基础。

### 阶段二：订单到派工

开发：

- `customer_order`
- `customer_order_item`
- `work_order`
- `kitting_analysis`
- `material_shortage`
- `production_task`
- `dispatch_task`

完成后，系统可以从订单生成工单、做齐套分析、生成产线任务和工位派工。

### 阶段三：库存与生产执行

开发：

- `warehouse`
- `storage_location`
- `inventory_batch`
- `material_requisition`
- `material_requisition_item`
- `material_issue`
- `material_issue_item`
- `inventory_transaction`
- `product_sn`
- `product_process_state`
- `production_report`
- `product_material_binding`

完成后，系统可以支持领料、发料、扫码、关键物料绑定、生产报工和产品追溯。

### 阶段四：质量、安灯、设备

开发：

- `inspection_item`
- `quality_standard`
- `quality_task`
- `quality_record`
- `quality_record_item`
- `defect_record`
- `rework_order`
- `andon_type`
- `andon_reason`
- `andon_event`
- `andon_task`
- `andon_result`
- `device`
- `repair_order`
- `repair_record`

完成后，MES 的生产闭环基本成立。

### 阶段五：Agent MVP

先开发只读 Agent，不做自动写操作：

- `agent_profile`
- `llm_provider`
- `llm_model`
- `agent_model_config`
- `agent_session`
- `agent_message`
- `agent_context_binding`
- `agent_task`
- `agent_tool`
- `agent_tool_permission`
- `agent_tool_call`
- `knowledge_space`
- `knowledge_document`
- `knowledge_chunk`
- `knowledge_embedding`
- `agent_retrieval_query`
- `agent_retrieval_result`

第一版 Agent 只做五件事：

- 根据工单号解释工单进度。
- 根据工单号解释欠料原因。
- 根据产品码查询追溯链路。
- 根据质检记录分析不良原因。
- 根据设备编号查询故障和维修历史。

### 阶段六：Agent 高级能力

后续再开发：

- `agent_approval_request`
- `agent_action_result`
- `agent_memory`
- `agent_case`
- `agent_event_subscription`
- `agent_event_inbox`
- `agent_workflow`
- `agent_recommendation`
- `agent_risk_alert`
- `agent_policy`
- `agent_guardrail_rule`
- `agent_evaluation_run`
- `agent_audit_log`

此阶段 Agent 才允许在审批后创建安灯任务、生成维修建议、生成排产建议或触发报表。

## 3. 后端开发建议

建议后端采用分模块包结构：

```text
modules/
  auth/
  master-data/
  process/
  planning/
  inventory/
  production/
  quality/
  andon/
  equipment/
  reporting/
  integration/
  agent/
```

数据库表也按这些模块归属，开发时不要把所有业务写在一个 service 中。

## 4. 关于外键

当前脚本保留了强外键，便于开发早期发现数据错误。正式上生产后，如果性能压力较大，可以评估取消部分日志表、采集表的外键，改由应用层保证一致性。

不建议取消核心业务链路外键，例如：

- `work_order.product_id`
- `production_report.work_order_id`
- `product_material_binding.sn_id`
- `quality_record.quality_task_id`
- `andon_event.work_order_id`
- `repair_order.device_id`

这些关系是追溯链路的基础。

## 5. 关于状态字段

状态字段暂时使用 `VARCHAR(32)`，开发中先在后端定义枚举。

示例：

- 工单：`CREATED`、`KITTING`、`READY`、`DISPATCHED`、`RUNNING`、`PAUSED`、`COMPLETED`、`CLOSED`
- 派工：`CREATED`、`ASSIGNED`、`RUNNING`、`DONE`、`CANCELLED`
- 质检：`CREATED`、`INSPECTING`、`PASS`、`FAIL`、`CLOSED`
- 安灯：`OPEN`、`ASSIGNED`、`PROCESSING`、`RESOLVED`、`CLOSED`
- 设备：`NORMAL`、`FAULT`、`REPAIRING`、`STOPPED`、`SCRAPPED`
- Agent 任务：`CREATED`、`PLANNING`、`RUNNING`、`WAITING_APPROVAL`、`DONE`、`FAILED`、`CANCELLED`

## 6. 需要尽早补充的内容

进入后端开发前，建议你后续确认这些问题：

- 后端技术栈是 Java Spring Boot、Python FastAPI，还是 Node/NestJS。
- 是否使用 MyBatis、JPA、Prisma、SQLAlchemy 等 ORM。
- 是否需要真实 MySQL Docker Compose。
- Agent 是否使用独立服务，还是先放在同一个后端工程里。
- 向量库准备使用 Milvus、pgvector、Elasticsearch、Qdrant，还是先用外部 API。

确认后，我可以继续生成：

- Docker Compose MySQL 环境；
- Flyway 迁移目录；
- 后端实体类；
- Mapper/Repository；
- 基础 CRUD；
- 工单、报工、追溯和 Agent 查询接口。
