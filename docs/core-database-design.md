# 云枢智造 MES 核心数据库设计说明

> 版本：1.0  
> 数据库：`fan_mes`（MySQL）  
> 范围：62 张重点表，包含 45 张 MES 核心表与 17 张 AI/Agent 核心表。  
> 依据：当前后端迁移脚本、Controller/Service 与 Yunshu UI 实际业务入口。

## 1. 设计目标与范围

本设计描述当前系统的统一逻辑数据模型，而非建表历史。系统以工单为生产执行中心，业务主线为：

```text
工厂资源 + 主数据 + 工艺
            ↓
客户订单 → 工单 → 生产任务 → 工序派工
                              ↓
                  现场报工 → 投料 / 产出
                              ↓
         来料检验 / 过程检验 / 出货检验 / 退料检验
                              ↓
       库存批次、SN 追溯、设备、安灯、智能 Agent
```

不纳入本设计主体的表包括：普通日志、报表配置、IoT 能耗、外部系统同步明细、非核心退货/销售扩展单据，以及二期 AI 记忆、评测、工作流等表。

## 2. 统一建模规则

1. 所有业务关联以 `xxx_id` 为准；`xxx_code`、`xxx_name`、`specification` 是单据快照，用于保留历史业务语义，不能作为关联条件。
2. 主数据采用“编码唯一、状态软停用”的原则；已被业务单据引用的主数据不得物理删除。
3. 单据采用“单头—单行”模型：单头描述归属、日期、状态和业务上下文；单行描述物料、数量、批次和规格。
4. 库存余额表不能替代库存流水表：`wm_material_stock` 保存现存余额，`wm_transaction` 保存所有可追溯变动。
5. 兼容表部分未在数据库中声明物理外键；本文规定的逻辑外键必须由 Service 层事务、校验和索引共同保证。
6. 除明确列出的字段外，兼容业务表一般还包含 `remark`、`create_by`、`create_time`、`update_by`、`update_time` 及扩展字段；这些字段用于审计和兼容，不改变主业务关系。

## 3. 核心关系总览

```text
sys_user ──< 生产报工 / 质检 / 安灯 / Agent会话

workshop ──< production_line ──< workstation
                                      │
md_item_type ──< md_item ──< md_product_bom
                         │              │
                         │              └── 物料组成
                         └──< pro_route_product >── pro_route ──< pro_route_process >── pro_process

customer_order ──< customer_order_item ──< work_order ──< production_task ──< dispatch_task
                                                │                 │
                                                │                 └── pro_feedback
                                                ├──< work_order_bom      ├──< wm_item_consume ──< wm_item_consume_line
                                                │                         └──< wm_product_produce ──< wm_product_produce_line
                                                └──< product_sn

warehouse ──< storage_zone ──< storage_bin
      │                              │
      └──< inventory_batch ──< wm_material_stock ──< wm_transaction

qc_template ──< qc_template_index
      ├──< qc_iqc       （来料）
      ├──< qc_ipqc      （过程）
      ├──< qc_oqc       （出货）
      └──< qc_rqc       （退料）

agent_profile ──< agent_session ──< agent_message
       │                  │
       │                  └──< agent_context_binding ──> 工单/SN/设备/安灯
       ├──< agent_model_config >── llm_model >── llm_provider
       ├──< agent_tool_permission >── agent_tool >── agent_connector
       └──< agent_task ──< agent_plan ──< agent_plan_step ──< agent_action ──< agent_tool_call
```

## 4. MES 核心表（45 张）

### 4.1 用户与工厂资源（4 张）

| # | 表名 | 主键与核心字段 | 规范关系与约束 | 职责 |
|---:|---|---|---|---|
| 1 | `sys_user` | `user_id`；`username`、`password_hash`、`real_name`、`dept_id`、`status` | `username` 唯一；被报工、质检、安灯、Agent 会话引用；停用用户不得再接收任务。 | 系统人员与操作主体。 |
| 2 | `workshop` | `workshop_id`；`workshop_code`、`workshop_name`、`status` | `workshop_code` 唯一；被产线、设备引用；禁止删除已有产线的车间。 | 生产车间。 |
| 3 | `production_line` | `line_id`；`workshop_id`、`line_code`、`line_name`、`rated_capacity`、`status` | `workshop_id → workshop.workshop_id`；同车间内 `line_code` 唯一。 | 车间下的生产线及额定产能。 |
| 4 | `workstation` | `station_id`；`line_id`、`station_code`、`station_name`、`station_type`、`status` | `line_id → production_line.line_id`；同产线工位编码唯一；报工、派工、安灯以此为现场位置。 | 最小生产执行位置。 |

### 4.2 物料与客户供应商主数据（6 张）

| # | 表名 | 主键与核心字段 | 规范关系与约束 | 职责 |
|---:|---|---|---|---|
| 5 | `md_item_type` | `item_type_id`；`item_type_code`、`item_type_name`、`parent_type_id`、`item_or_product`、`enable_flag` | `parent_type_id` 自关联；编码唯一；通过 `ancestors` 保存层级路径。 | 产品/物料分类树。 |
| 6 | `md_unit_measure` | `measure_id`；`measure_code`、`measure_name`、`change_rate`、`enable_flag` | `measure_code` 唯一；业务单据保存单位代码快照。 | 计量单位及换算率。 |
| 7 | `md_item` | `item_id`；`item_code`、`item_name`、`specification`、`unit_of_measure`、`item_or_product`、`item_type_id`、`batch_flag`、`min_stock`、`max_stock` | `item_type_id → md_item_type`；`item_code` 唯一；`item_or_product` 区分 `ITEM/PRODUCT`。 | 统一产品与物料中心。 |
| 8 | `md_product_bom` | `bom_id`；`item_id`、`bom_item_id`、`quantity`、`unit_of_measure`、`enable_flag` | `item_id → md_item` 为父项，`bom_item_id → md_item` 为子项；父项与子项组合唯一。 | 产品/半成品 BOM 用量。 |
| 9 | `md_vendor` | `vendor_id`；`vendor_code`、`vendor_name`、`vendor_nick`、`enable_flag` | `vendor_code` 唯一；来料、批次及 IQC 单据引用。 | 供应商主数据。 |
| 10 | `md_client` | `client_id`；`client_code`、`client_name`、`client_nick`、`enable_flag` | `client_code` 唯一；订单、领料/出货场景可保存客户快照。 | 客户主数据。 |

### 4.3 工艺路线（5 张）

| # | 表名 | 主键与核心字段 | 规范关系与约束 | 职责 |
|---:|---|---|---|---|
| 11 | `pro_process` | `process_id`；`process_code`、`process_name`、`attention`、`enable_flag` | `process_code` 唯一；被路线、报工、质检、安灯引用。 | 工序定义。 |
| 12 | `pro_process_content` | `content_id`；`process_id`、`order_num`、`content_text`、`device`、`material`、`doc_url` | `process_id → pro_process`；同工序 `order_num` 唯一。 | 工序作业内容、设备/物料提示、SOP 链接。 |
| 13 | `pro_route` | `route_id`；`route_code`、`route_name`、`route_desc`、`enable_flag` | `route_code` 唯一；被工单和产品路线配置引用。 | 工艺路线主表。 |
| 14 | `pro_route_process` | `record_id`；`route_id`、`process_id`、`order_num`、`next_process_id`、`link_type`、`is_check` | `route_id → pro_route`，`process_id/next_process_id → pro_process`；同路线工序顺序唯一。 | 路线工序顺序、前后置关系和质检标识。 |
| 15 | `pro_route_product` | `record_id`；`route_id`、`item_id`、`quantity`、`production_time`、`time_unit_type` | `route_id → pro_route`，`item_id → md_item`；路线与产品组合唯一。 | 产品适用路线及标准工时。 |

### 4.4 订单、工单与派工（6 张）

| # | 表名 | 主键与核心字段 | 规范关系与约束 | 职责 |
|---:|---|---|---|---|
| 16 | `customer_order` | `order_id`；`order_no`、`customer_name`、`order_date`、`delivery_date`、`status` | `order_no` 唯一；不能早于下属订单行交期。 | 客户订单单头。 |
| 17 | `customer_order_item` | `order_item_id`；`order_id`、`product_id`、`order_qty`、`technical_requirement` | `order_id → customer_order`；`product_id` 为产品主数据映射；数量必须大于 0。 | 客户订单产品行。 |
| 18 | `work_order` | `work_order_id`；`work_order_no`、`order_id`、`order_item_id`、`product_id`、`item_id`、`bom_id`、`route_id`、`plan_qty`、`completed_qty`、`defect_qty`、计划/实际起止时间、`status` | `order_id/order_item_id` 关联订单；`item_id → md_item`；工单号唯一；`completed_qty + defect_qty` 不应超过计划数量。 | 生产全链路的业务锚点。 |
| 19 | `work_order_bom` | `line_id`；`work_order_id`、`material_id`、`material_code`、`quantity`、`item_or_product` | `work_order_id → work_order`；每工单物料行唯一；创建时冻结 BOM 快照。 | 工单实际物料需求快照。 |
| 20 | `production_task` | `task_id`；`task_no`、`work_order_id`、`line_id`、`shift_id`、`task_date`、`task_qty`、`completed_qty`、`status`、`start_time`、`end_time` | 关联工单、产线、班次；任务号唯一；报工执行累加 `completed_qty`。 | 产线/班次级生产任务。 |
| 21 | `dispatch_task` | `dispatch_id`；`dispatch_no`、`task_id`、`work_order_id`、`step_id`、`station_id`、`operator_id`、`planned_qty`、`completed_qty`、计划/实际时间、`status` | 关联任务、工单、工序、工位和操作员；派工量不得超过任务量。 | 工序和工位级派工。 |

### 4.5 生产执行与 SN（6 张）

| # | 表名 | 主键与核心字段 | 规范关系与约束 | 职责 |
|---:|---|---|---|---|
| 22 | `pro_feedback` | `record_id`；`feedback_type`、`feedback_code`、`workstation_id`、`workorder_id`、`route_id`、`process_id`、`task_id`、`item_id`、`quantity_feedback`、`quantity_qualified`、`quantity_unquanlified`、`quantity_uncheck`、`status` | 关联工位、工单、工序、任务和物料；执行后状态为 `FINISHED`；合格数量累计到生产任务。 | 现场生产报工单。 |
| 23 | `wm_item_consume` | `record_id`；`workorder_id`、`task_id`、`workstation_id`、`process_id`、`feedback_id`、`consume_date`、`status` | `feedback_id → pro_feedback`；单头描述一次投料。 | 报工关联的投料单头。 |
| 24 | `wm_item_consume_line` | `line_id`；`record_id`、`material_stock_id`、`item_id`、`quantity_consume`、`batch_id`、`batch_code` | `record_id → wm_item_consume`；关联库存余额与批次；投料数量不得超过可用库存。 | 一次投料中的物料、批次和数量。 |
| 25 | `wm_product_produce` | `record_id`；`workorder_id`、`task_id`、`workstation_id`、`process_id`、`feedback_id`、`produce_date`、`status` | `feedback_id → pro_feedback`；作为过程检验和成品入库来源。 | 报工关联的产出单头。 |
| 26 | `wm_product_produce_line` | `line_id`；`record_id`、`material_stock_id`、`item_id`、`quantity_produce`、`batch_id`、`batch_code` | `record_id → wm_product_produce`；产出记录应生成库存流水；待检状态可触发 IPQC。 | 一次产出的产品、批次和数量。 |
| 27 | `product_sn` | `sn_id`；`sn_code`、`product_id`、`work_order_id`、`barcode_id`、`status` | `sn_code` 唯一；关联工单和产品；用于产品级追溯与条码绑定。 | 单件产品序列号。 |

### 4.6 仓储、批次与库存单据（10 张）

| # | 表名 | 主键与核心字段 | 规范关系与约束 | 职责 |
|---:|---|---|---|---|
| 28 | `warehouse` | `warehouse_id`；`warehouse_code`、`warehouse_name`、`warehouse_type`、`status` | `warehouse_code` 唯一；类型包括原料、在制、成品、报废等。 | 仓库主数据。 |
| 29 | `storage_zone` | `zone_id`；`zone_code`、`zone_name`、`warehouse_id`、`area_flag`、`frozen_flag` | `warehouse_id → warehouse`；同仓库库区编码唯一。 | 仓库内库区。 |
| 30 | `storage_bin` | `bin_id`；`bin_code`、`bin_name`、`zone_id`、`max_loa`、坐标、`product_mixing`、`batch_mixing`、`frozen_flag` | `zone_id → storage_zone`；同库区库位编码唯一。 | 最小库存存放位置。 |
| 31 | `inventory_batch` | `batch_id`；`material_id`、`warehouse_id`、`location_id`、`batch_no`、`available_qty`、`locked_qty`、`quality_status`、`expire_date`、`status` | 关联物料、仓库和库位；物料+仓库+批号组合唯一；可用数不得小于锁定数。 | 旧批次维度库存与质量状态锚点。 |
| 32 | `wm_material_stock` | `material_stock_id`；`item_id`、`batch_id`、`workorder_id`、`vendor_id`、`warehouse_id`、`location_id`、`area_id`、`quantity_onhand`、`quantity_reserved`、日期字段、`frozen_flag` | 关联物料、批次、仓库、库区、库位；余额必须由库存流水汇总或事务同步维护。 | 当前库位级可用库存余额。 |
| 33 | `wm_transaction` | `transaction_id`；`transaction_type`、`item_id`、`batch_id`、`warehouse_id`、`location_id`、`area_id`、`source_doc_type`、`source_doc_id`、`material_stock_id`、`transaction_flag`、`transaction_quantity`、`transaction_date` | 每次库存增减都写入；`transaction_flag` 表示增减方向；来源单据与来源行可追溯。 | 库存变动不可替代流水。 |
| 34 | `wm_item_recpt` | `recpt_id`；`recpt_code`、`recpt_name`、`iqc_id`、`vendor_id`、`warehouse_id`、`location_id`、`area_id`、`recpt_date`、`status` | 可关联 IQC 和供应商；入库单号唯一；完成后生成批次、库存余额和流水。 | 原料/物料入库单头。 |
| 35 | `wm_item_recpt_line` | `line_id`；`recpt_id`、`item_id`、`quantity_recived`、`batch_id`、`warehouse_id`、`location_id`、`area_id`、生产/效期、`iqc_check` | `recpt_id → wm_item_recpt`；数量大于 0；批次、库位和 IQC 状态在此冻结。 | 原料/物料入库单行。 |
| 36 | `wm_issue_header` | `issue_id`；`issue_code`、`issue_name`、`workstation_id`、`workorder_id`、`task_id`、`required_time`、`issue_date`、`status` | 关联工单、任务和工位；领料单号唯一；完成后生成负向库存流水。 | 面向生产的领料单头。 |
| 37 | `wm_issue_line` | `line_id`；`issue_id`、`item_id`、`quantity_issued`、`batch_id`、`batch_code` | `issue_id → wm_issue_header`；批次必须处于可领用质量状态；数量不超过可用库存。 | 领料物料与批次明细。 |

### 4.7 质量管理（6 张）

| # | 表名 | 主键与核心字段 | 规范关系与约束 | 职责 |
|---:|---|---|---|---|
| 38 | `qc_template` | `template_id`；`template_code`、`template_name`、`qc_types`、`enable_flag` | 模板编码唯一；可适用于 IQC/IPQC/OQC/RQC。 | 检验模板主表。 |
| 39 | `qc_template_index` | `record_id`；`template_id`、`qc_tool`、`check_method`、`stander_val`、`threshold_min`、`threshold_max`、`unit_of_measure` | `template_id → qc_template`；每行描述一个检验指标及容差。 | 模板检验项目明细。 |
| 40 | `qc_iqc` | `iqc_id`；`iqc_code`、`template_id`、来源单据、`vendor_id`、`item_id`、收货/抽检/合格/不合格数量、`check_result`、`status` | 关联模板、供应商、物料和来料单据；判定结果影响来料入库可用性。 | 来料检验单。 |
| 41 | `qc_ipqc` | `ipqc_id`；`ipqc_code`、`ipqc_type`、`template_id`、`workorder_id`、`task_id`、`workstation_id`、`process_id`、`item_id`、检验数量和结果、`status` | 关联工单、任务、工位、工序和产出/报工来源；不合格可触发安灯或返工。 | 过程检验单。 |
| 42 | `qc_oqc` | `oqc_id`；`oqc_code`、`template_id`、来源销售单、`client_id`、`item_id`、`batch_code`、发货/抽检/不合格数量、`check_result`、`status` | 关联出货单、客户和批次；未检验合格的成品不得出库。 | 出货检验单。 |
| 43 | `qc_rqc` | `rqc_id`；`rqc_code`、`template_id`、来源退料单、`rqc_type`、`item_id`、`batch_id`、检验数量、`check_result`、`status` | 关联生产退料或销售退货；判定后决定再入库、报废或返工。 | 退料检验单。 |

### 4.8 设备与安灯（2 张）

| # | 表名 | 主键与核心字段 | 规范关系与约束 | 职责 |
|---:|---|---|---|---|
| 44 | `dv_machinery` | `machinery_id`；`machinery_code`、`machinery_name`、`machinery_type_id`、`workshop_id`、最近保养/点检时间、`status` | 关联设备类型和车间；设备编码唯一；`REPAIR/STOP` 状态应影响排产与安灯分析。 | 设备台账。 |
| 45 | `pro_andon_record` | `record_id`；`workstation_id`、`user_id`、`workorder_id`、`process_id`、`andon_reason`、`andon_level`、处理人、`handle_time`、`status` | 关联现场工位、工单、工序、发起和处理人员；`attr1` 可保存桥接的 `andon_event_id`。 | 现场安灯异常及闭环处理记录。 |

## 5. AI / Agent 核心表（17 张）

### 5.1 Agent、模型与提示词（6 张）

| # | 表名 | 主键与核心字段 | 规范关系与约束 | 职责 |
|---:|---|---|---|---|
| 46 | `agent_profile` | `agent_id`；`agent_code`、`agent_name`、`agent_type`、`owner_dept_id`、`description`、`status` | `agent_code` 唯一；类型可为计划、齐套、生产、质量、设备、安灯、追溯、报表。 | Agent 定义与启停控制。 |
| 47 | `llm_provider` | `provider_id`；`provider_code`、`provider_name`、`api_base_url`、`auth_type`、`secret_ref`、`status` | 提供商编码唯一；只保存密钥引用，禁止保存明文 API Key。 | 大模型供应商配置。 |
| 48 | `llm_model` | `model_id`；`provider_id`、`model_code`、`model_name`、`model_type`、`context_window`、价格、`status` | `provider_id → llm_provider`；同提供商模型编码唯一；模型类型包括 CHAT、EMBEDDING、RERANK、VISION。 | 可调用模型目录。 |
| 49 | `agent_model_config` | `model_config_id`；`agent_id`、`model_id`、`purpose`、`temperature`、`top_p`、`max_tokens`、`config_json`、`status` | `agent_id → agent_profile`，`model_id → llm_model`；同 Agent+用途只允许一个启用配置。 | 指定 Agent 在对话、摘要、工具推理中的模型参数。 |
| 50 | `agent_prompt_template` | `prompt_id`；`prompt_code`、`prompt_name`、`prompt_type`、`description` | 提示词编码唯一；类型包括 SYSTEM、TASK、TOOL、RAG、EVAL。 | 可复用提示词模板。 |
| 51 | `agent_prompt_version` | `prompt_version_id`；`prompt_id`、`agent_id`、`version_no`、`prompt_content`、`status`、`created_by` | 关联模板、可选 Agent 和创建人；同模板版本号唯一；生产调用仅允许已发布版本。 | 提示词内容与版本治理。 |

### 5.2 Agent 工具与权限（3 张）

| # | 表名 | 主键与核心字段 | 规范关系与约束 | 职责 |
|---:|---|---|---|---|
| 52 | `agent_connector` | `connector_id`；`connector_code`、`connector_name`、`connector_type`、`auth_type`、`endpoint`、`secret_ref`、`status` | 连接器编码唯一；类型包括 MES_DB、MES_API、ERP_API、REPORT、IOT、VECTOR_STORE。 | 外部或内部能力的安全连接配置。 |
| 53 | `agent_tool` | `tool_id`；`connector_id`、`tool_code`、`tool_name`、`tool_type`、`risk_level`、输入/输出 JSON Schema、`status` | `connector_id → agent_connector`；工具编码唯一；写操作必须标记 `MEDIUM/HIGH` 风险。 | Agent 可调用的标准化工具。 |
| 54 | `agent_tool_permission` | `permission_id`；`agent_id`、`tool_id`、`allow_scope`、`require_approval`、`status` | 关联 Agent 与工具；同 Agent+工具唯一；默认最小权限，写操作必须设置审批。 | Agent 工具白名单与数据范围控制。 |

### 5.3 会话、上下文与执行链（8 张）

| # | 表名 | 主键与核心字段 | 规范关系与约束 | 职责 |
|---:|---|---|---|---|
| 55 | `agent_session` | `session_id`；`agent_id`、`user_id`、`session_title`、`session_status`、`start_time`、`end_time` | 关联 Agent 和用户；只能向有权限的用户展示；关闭会话不再写入消息。 | 一次用户与 Agent 的会话。 |
| 56 | `agent_message` | `message_id`；`session_id`、`sender_type`、`sender_id`、`message_content`、`token_count`、`send_time` | `session_id → agent_session`；发送方为 USER、AGENT、SYSTEM、TOOL；按时间顺序不可修改。 | 会话消息和工具回显。 |
| 57 | `agent_context_binding` | `binding_id`；`session_id`、`biz_object_type`、`biz_object_id`、`binding_reason`、`created_at` | `session_id → agent_session`；业务对象可为 `work_order`、`product_sn`、`device`、`andon_event` 等。 | 将当前对话固定到 MES 业务对象。 |
| 58 | `agent_task` | `task_id`；`session_id`、`agent_id`、`task_type`、`task_title`、`task_status`、`priority`、`biz_object_type`、`biz_object_id`、完成时间 | 关联会话和 Agent；类型包括 CHAT、ANALYSIS、TRACE、PLAN、DIAGNOSE、REPORT、AUTO_EVENT。 | 一次可跟踪的 Agent 工作请求。 |
| 59 | `agent_plan` | `plan_id`；`task_id`、`plan_status`、`plan_summary`、`generated_time` | `task_id → agent_task`；任务可有多个历史计划，但同一时刻仅一个执行计划。 | Agent 对任务生成的计划。 |
| 60 | `agent_plan_step` | `step_id`；`plan_id`、`step_seq`、`step_type`、`step_desc`、`step_status` | `plan_id → agent_plan`；同计划步骤序号唯一；类型包括检索、工具、审批、总结。 | 可解释的计划步骤。 |
| 61 | `agent_action` | `action_id`；`step_id`、`action_type`、`biz_object_type`、`biz_object_id`、`action_status`、起止时间 | `step_id → agent_plan_step`；动作类型包括查询、工具调用、生成建议、创建工单、更新状态。 | 计划步骤的实际执行动作。 |
| 62 | `agent_tool_call` | `tool_call_id`；`action_id`、`tool_id`、`request_payload`、`response_payload`、`call_status`、`error_message`、`latency_ms`、`called_at` | `action_id → agent_action`，`tool_id → agent_tool`；请求/响应需脱敏；高风险调用前校验权限与审批。 | 每次工具调用的审计与可复现记录。 |

## 6. 状态与事务规则

### 6.1 工单到报工

1. 创建工单时冻结产品、BOM、工艺路线和计划数量。
2. 创建生产任务与派工时，派工数量之和不得超过任务数量。
3. 执行 `pro_feedback` 时，在同一事务中更新报工状态、累计 `production_task.completed_qty`，并写入投料/产出或其后续处理记录。
4. 只有所有任务完成且质量放行条件满足时，工单才能完工。

### 6.2 库存与质量

1. 入库、领料、投料、产出、退料都必须同步更新 `wm_material_stock` 并新增 `wm_transaction`。
2. `inventory_batch.quality_status` 或批次的质量状态为待检、不合格、冻结时，禁止领料和销售出库。
3. IQC 对来料入库负责；IPQC 对生产产出/报工负责；OQC 对销售出库负责；RQC 对退料负责。
4. 所有数量字段使用 `DECIMAL/DOUBLE` 时，业务层必须统一单位和精度，禁止浮点字符串比较。

### 6.3 AI/Agent 安全

1. Agent 查询和操作必须通过 `agent_tool_permission` 校验，不允许模型直接拼接任意 SQL。
2. Agent 写操作必须配置高风险工具，并在动作链中保留 `agent_task → plan → step → action → tool_call`。
3. `agent_context_binding` 只允许绑定当前用户有数据权限的 MES 对象。
4. 模型密钥只使用 `secret_ref` 指向安全配置；日志、消息和工具结果不得保存明文密钥、JWT 或敏感个人信息。

## 7. 表数核对

| 模块 | 表数 | 编号范围 |
|---|---:|---|
| 用户与工厂资源 | 4 | 1–4 |
| 主数据 | 6 | 5–10 |
| 工艺路线 | 5 | 11–15 |
| 订单与派工 | 6 | 16–21 |
| 生产执行 | 6 | 22–27 |
| 仓储与库存 | 10 | 28–37 |
| 质量管理 | 6 | 38–43 |
| 设备与安灯 | 2 | 44–45 |
| AI / Agent | 17 | 46–62 |
| **合计** | **62** | **1–62** |

## 8. 现有实现边界

- 当前生产报工执行接口已实现“报工完成 + 生产任务合格数累计”的核心动作；投料、产出单据在页面中作为关联子单据维护。
- 当前质量待检由来料、产出、报工、出货和退料来源动态汇总，不依赖独立待办表。
- 当前 SN 查询已实现基础的“SN—产品—工单—状态”追溯；由投料批次反向定位受影响成品的完整链路可在后续扩展。
- 当前 AI 聊天原型使用 `ai_chat_session` 与 `ai_chat_message`；正式 Agent 实施应统一采用本设计第 5 节的 `agent_session`、`agent_message` 和任务执行链，避免两套会话事实源长期并存。

## 9. 逐表字段字典

本节补充第 4、5 节的表级说明。为避免重复，以下约定适用于字段清单：

- **审计 A**：`created_at DATETIME(3)`、`updated_at DATETIME(3)`、`created_by BIGINT`、`updated_by BIGINT`、`is_deleted TINYINT(1)`、`version INT`；用于基础业务表。
- **审计 B**：`remark VARCHAR(500)`、`attr1 VARCHAR(64)`、`attr2 VARCHAR(255)`、`attr3 INT`、`attr4 INT`、`create_by VARCHAR(64)`、`create_time DATETIME(3)`、`update_by VARCHAR(64)`、`update_time DATETIME(3)`；用于兼容业务表。
- 下列每行列出该表的全部**业务字段**及其物理类型；上述公共审计字段按实际表结构附带。`PK` 为主键，`FK` 为实际或规范逻辑外键。

### 9.1 用户、工厂与主数据

| 表 | 字段结构（字段名：类型；`!` 表示 NOT NULL） | 主键、索引与关联 |
|---|---|---|
| `sys_user` | `user_id BIGINT UNSIGNED PK`；`username VARCHAR(64)!`；`password_hash VARCHAR(255)!`；`employee_no VARCHAR(64)`；`real_name VARCHAR(64)!`；`dept_id BIGINT UNSIGNED`；`phone VARCHAR(32)`；`email VARCHAR(128)`；`status VARCHAR(32)! DEFAULT 'ENABLED'`；`last_login_at DATETIME(3)`；审计 A | `username` 唯一；`dept_id → sys_department.dept_id`。 |
| `workshop` | `workshop_id BIGINT UNSIGNED PK`；`workshop_code VARCHAR(64)!`；`workshop_name VARCHAR(100)!`；`status VARCHAR(32)! DEFAULT 'ENABLED'`；`created_at DATETIME(3)!`；`updated_at DATETIME(3)!` | `workshop_code` 唯一。 |
| `production_line` | `line_id BIGINT UNSIGNED PK`；`workshop_id BIGINT UNSIGNED!`；`line_code VARCHAR(64)!`；`line_name VARCHAR(100)!`；`rated_capacity DECIMAL(18,4)`；`capacity_unit VARCHAR(32)`；`status VARCHAR(32)! DEFAULT 'ENABLED'`；时间戳 | `FK workshop_id`；`workshop_id + line_code` 唯一。 |
| `workstation` | `station_id BIGINT UNSIGNED PK`；`line_id BIGINT UNSIGNED!`；`station_code VARCHAR(64)!`；`station_name VARCHAR(100)!`；`station_type VARCHAR(32)`；`status VARCHAR(32)! DEFAULT 'ENABLED'`；时间戳 | `FK line_id`；`line_id + station_code` 唯一。 |
| `md_item_type` | `item_type_id BIGINT UNSIGNED PK`；`item_type_code VARCHAR(64)!`；`item_type_name VARCHAR(255)!`；`parent_type_id BIGINT UNSIGNED! DEFAULT 0`；`ancestors VARCHAR(255)! DEFAULT '0'`；`item_or_product VARCHAR(20)! DEFAULT 'ITEM'`；`order_num INT! DEFAULT 1`；`enable_flag CHAR(1)! DEFAULT 'Y'`；审计 B | `item_type_code` 唯一；`parent_type_id` 自关联。 |
| `md_unit_measure` | `measure_id BIGINT UNSIGNED PK`；`measure_code VARCHAR(64)!`；`measure_name VARCHAR(255)!`；`change_rate DOUBLE(12,4)`；`enable_flag CHAR(1)! DEFAULT 'Y'`；审计 B | `measure_code` 唯一。 |
| `md_item` | `item_id BIGINT UNSIGNED PK`；`item_code VARCHAR(64)!`；`item_name VARCHAR(255)!`；`specification VARCHAR(500)`；`unit_of_measure VARCHAR(64)! DEFAULT 'PCS'`；`unit_name VARCHAR(64)`；`item_or_product VARCHAR(20)!`；`item_type_id BIGINT UNSIGNED! DEFAULT 0`；`item_type_code VARCHAR(64)! DEFAULT ''`；`item_type_name VARCHAR(255)! DEFAULT ''`；`enable_flag CHAR(1)!`；`safe_stock_flag CHAR(1)!`；`min_stock DOUBLE(12,4)!`；`max_stock DOUBLE(12,4)!`；`high_value CHAR(1)!`；`batch_flag CHAR(1)!`；审计 B | `item_code` 唯一；`FK item_type_id`；`attr1/attr2` 可保存旧产品/物料映射。 |
| `md_product_bom` | `bom_id BIGINT UNSIGNED PK`；`item_id BIGINT UNSIGNED!`；`bom_item_id BIGINT UNSIGNED!`；`bom_item_code VARCHAR(64)!`；`bom_item_name VARCHAR(255)!`；`bom_item_spec VARCHAR(500)`；`unit_of_measure VARCHAR(64)!`；`item_or_product VARCHAR(20)!`；`quantity DOUBLE(12,4)!`；`enable_flag CHAR(1)!`；审计 B | `FK item_id,bom_item_id → md_item`；父子项组合唯一。 |
| `md_vendor` | `vendor_id BIGINT UNSIGNED PK`；`vendor_code VARCHAR(64)!`；`vendor_name VARCHAR(255)!`；`vendor_nick VARCHAR(255)`；`enable_flag CHAR(1)!`；联系人、电话、地址；审计 B | `vendor_code` 唯一。 |
| `md_client` | `client_id BIGINT UNSIGNED PK`；`client_code VARCHAR(64)!`；`client_name VARCHAR(255)!`；`client_nick VARCHAR(255)`；`enable_flag CHAR(1)!`；联系人、电话、地址；审计 B | `client_code` 唯一。 |

### 9.2 工艺、计划与生产执行

| 表 | 字段结构（字段名：类型；`!` 表示 NOT NULL） | 主键、索引与关联 |
|---|---|---|
| `pro_process` | `process_id BIGINT UNSIGNED PK`；`process_code VARCHAR(64)!`；`process_name VARCHAR(255)!`；`attention VARCHAR(1000)`；`enable_flag CHAR(1)! DEFAULT 'Y'`；审计 B | `process_code` 唯一。 |
| `pro_process_content` | `content_id BIGINT UNSIGNED PK`；`process_id BIGINT UNSIGNED!`；`order_num INT! DEFAULT 0`；`content_text VARCHAR(500)`；`device VARCHAR(255)`；`material VARCHAR(255)`；`doc_url VARCHAR(255)`；审计 B | `FK process_id`；工序内 `order_num` 唯一。 |
| `pro_route` | `route_id BIGINT UNSIGNED PK`；`route_code VARCHAR(64)!`；`route_name VARCHAR(255)!`；`route_desc VARCHAR(500)`；`enable_flag CHAR(1)!`；审计 B | `route_code` 唯一。 |
| `pro_route_process` | `record_id BIGINT UNSIGNED PK`；`route_id BIGINT UNSIGNED!`；`process_id BIGINT UNSIGNED!`；工序编码/名称快照；`order_num INT!`；`next_process_id BIGINT UNSIGNED! DEFAULT 0`；`link_type VARCHAR(64)! DEFAULT 'SS'`；`default_pre_time INT!`；`default_suf_time INT!`；`color_code CHAR(7)!`；`is_check CHAR(1)!`；审计 B | `FK route_id/process_id`；路线内顺序唯一。 |
| `pro_route_product` | `record_id BIGINT UNSIGNED PK`；`route_id BIGINT UNSIGNED!`；`item_id BIGINT UNSIGNED!`；物料编码/名称/规格快照；`unit_of_measure VARCHAR(64)!`；`quantity INT! DEFAULT 1`；`production_time DOUBLE(12,2)!`；`time_unit_type VARCHAR(64)! DEFAULT 'MINUTE'`；审计 B | `FK route_id,item_id`；路线产品组合唯一。 |
| `customer_order` | `order_id BIGINT UNSIGNED PK`；`order_no VARCHAR(64)!`；`customer_name VARCHAR(128)!`；`order_date DATE!`；`delivery_date DATE!`；`status VARCHAR(32)! DEFAULT 'CREATED'`；`remark VARCHAR(500)`；审计 A | `order_no` 唯一。 |
| `customer_order_item` | `order_item_id BIGINT UNSIGNED PK`；`order_id BIGINT UNSIGNED!`；`product_id BIGINT UNSIGNED!`；`order_qty DECIMAL(18,4)!`；`technical_requirement VARCHAR(1000)` | `FK order_id → customer_order`；`FK product_id → product`。 |
| `work_order` | `work_order_id BIGINT UNSIGNED PK`；`work_order_no VARCHAR(64)!`；`order_id BIGINT`；`order_item_id BIGINT`；`product_id BIGINT!`；`bom_id BIGINT`；`route_id BIGINT`；`item_id BIGINT`；`plan_qty DECIMAL(18,4)!`；`completed_qty DECIMAL(18,4)! DEFAULT 0`；`defect_qty DECIMAL(18,4)! DEFAULT 0`；计划/实际起止 `DATETIME(3)`；`priority VARCHAR(32)!`；`status VARCHAR(32)!`；审计 A | 工单号唯一；关联订单、BOM、路线、统一物料。 |
| `work_order_bom` | `line_id BIGINT UNSIGNED PK`；`work_order_id BIGINT UNSIGNED!`；`material_id BIGINT UNSIGNED!`；物料编码/名称/规格快照；`unit_code VARCHAR(32)`；`item_or_product VARCHAR(20)!`；`quantity DECIMAL(18,4)!`；`remark VARCHAR(500)`；`created_at DATETIME(3)!`；`updated_at DATETIME(3)!` | `FK work_order_id,material_id`；工单物料组合唯一。 |
| `production_task` | `task_id BIGINT UNSIGNED PK`；`task_no VARCHAR(64)!`；`work_order_id BIGINT!`；`line_id BIGINT!`；`shift_id BIGINT`；`task_date DATE!`；`task_qty DECIMAL(18,4)!`；`completed_qty DECIMAL(18,4)!`；`status VARCHAR(32)!`；`start_time DATETIME(3)`；`end_time DATETIME(3)`；审计字段 | 关联工单、产线、班次；任务号唯一。 |
| `dispatch_task` | `dispatch_id BIGINT UNSIGNED PK`；`dispatch_no VARCHAR(64)!`；`task_id BIGINT!`；`work_order_id BIGINT!`；`route_step_id BIGINT`；`step_id BIGINT!`；`station_id BIGINT`；`operator_id BIGINT`；`planned_qty DECIMAL(18,4)!`；`completed_qty DECIMAL(18,4)!`；计划/实际起止时间；`status VARCHAR(32)!` | 关联任务、工单、工序、工位、人员；派工号唯一。 |
| `pro_feedback` | `record_id BIGINT UNSIGNED PK`；`feedback_type VARCHAR(64)!`；`feedback_code VARCHAR(64)`；工位、工单、路线、工序、任务的 ID/编码/名称快照；`item_id BIGINT!`；物料快照；`quantity DOUBLE(14,2)`；`quantity_feedback/qualified/unquanlified/uncheck DOUBLE(14,2)`；人员、渠道、`feedback_time DATETIME(3)`；`status VARCHAR(64)! DEFAULT 'PREPARE'`；审计 B | 关联工单任务与工序；执行后更新任务数量。 |
| `wm_item_consume` | `record_id BIGINT UNSIGNED PK`；工单、任务、工位、工序的 ID/编码/名称；`feedback_id BIGINT`；`consume_date DATETIME(3)`；`status VARCHAR(64)!`；审计 B | `feedback_id → pro_feedback`。 |
| `wm_item_consume_line` | `line_id BIGINT UNSIGNED PK`；`record_id BIGINT`；`material_stock_id BIGINT`；`item_id BIGINT!`；物料快照；单位快照；`quantity_consume DOUBLE(12,2)!`；`batch_id BIGINT`；`batch_code VARCHAR(255)`；审计 B | `FK record_id`；库存、批次逻辑外键。 |
| `wm_product_produce` | `record_id BIGINT UNSIGNED PK`；工单、任务、工位、工序 ID/编码/名称；`feedback_id BIGINT`；`produce_date DATETIME(3)`；`status VARCHAR(64)!`；审计 B | `feedback_id → pro_feedback`。 |
| `wm_product_produce_line` | `line_id BIGINT UNSIGNED PK`；`record_id BIGINT`；`material_stock_id BIGINT`；`item_id BIGINT!`；物料/单位快照；`quantity_produce DOUBLE(12,2)!`；`batch_id BIGINT`；`batch_code VARCHAR(255)`；审计 B | `FK record_id`；生成过程检验来源。 |
| `product_sn` | `sn_id BIGINT UNSIGNED PK`；`sn_code VARCHAR(128)!`；`product_id BIGINT!`；`work_order_id BIGINT!`；`barcode_id BIGINT`；`status VARCHAR(32)!`；时间戳 | `sn_code` 唯一；关联产品、工单、条码记录。 |

### 9.3 仓储、质量、设备与安灯

| 表 | 字段结构（字段名：类型；`!` 表示 NOT NULL） | 主键、索引与关联 |
|---|---|---|
| `warehouse` | `warehouse_id BIGINT UNSIGNED PK`；`warehouse_code VARCHAR(64)!`；`warehouse_name VARCHAR(128)!`；`warehouse_type VARCHAR(32)!`；`status VARCHAR(32)!` | 仓库编码唯一。 |
| `storage_zone` | `zone_id BIGINT UNSIGNED PK`；`zone_code VARCHAR(64)!`；`zone_name VARCHAR(255)!`；`warehouse_id BIGINT!`；`area DOUBLE`；`area_flag CHAR(1)!`；`frozen_flag CHAR(1)!`；审计 B | `FK warehouse_id`；仓库内编码唯一。 |
| `storage_bin` | `bin_id BIGINT UNSIGNED PK`；`bin_code VARCHAR(64)!`；`bin_name VARCHAR(255)!`；`zone_id BIGINT!`；`legacy_location_id BIGINT`；`area DOUBLE`；`max_loa DOUBLE`；三维坐标 `INT`；`enable_flag/frozen_flag/product_mixing/batch_mixing CHAR(1)`；审计 B | `FK zone_id`；库区内编码唯一。 |
| `inventory_batch` | `batch_id BIGINT UNSIGNED PK`；`material_id BIGINT!`；`warehouse_id BIGINT!`；`location_id BIGINT`；`batch_no VARCHAR(64)!`；`supplier_batch_no VARCHAR(64)`；`available_qty DECIMAL(18,4)!`；`locked_qty DECIMAL(18,4)!`；`quality_status VARCHAR(32)!`；`received_at DATETIME(3)`；`expire_date DATE`；`status VARCHAR(32)!`；时间戳 | 关联物料、仓库、库位；批号维度库存。 |
| `wm_material_stock` | `material_stock_id BIGINT UNSIGNED PK`；`item_type_id`、`item_id BIGINT!`、物料快照；`batch_id`、`workorder_id`、`vendor_id`、`client_id`；`warehouse_id BIGINT!`、`location_id`、`area_id`；`package_id`；`quantity_onhand DOUBLE!`；`quantity_reserved DOUBLE!`；生产/入库/效期；`frozen_flag CHAR(1)!`；审计 B | 物料+批次+库位为库存余额维度。 |
| `wm_transaction` | `transaction_id BIGINT UNSIGNED PK`；`transaction_type VARCHAR(64)!`；`item_id BIGINT!`；物料快照；批次、仓库、库区、库位、包装 ID/快照；`source_doc_type/id/code/line_id`；`material_stock_id`；`transaction_flag INT! DEFAULT 1`；`transaction_quantity DOUBLE!`；`transaction_date DATETIME`；`related_transaction_id BIGINT`；审计字段 | 来源单据与库存余额的流水记录。 |
| `wm_item_recpt` | `recpt_id BIGINT UNSIGNED PK`；`recpt_code VARCHAR(64)!`；`recpt_name VARCHAR(255)!`；`iqc_id`；通知/采购单字段；供应商、仓库、库区、库位 ID/快照；`recpt_date DATETIME`；`status VARCHAR(64)!`；审计字段 | 入库单号唯一。 |
| `wm_item_recpt_line` | `line_id BIGINT UNSIGNED PK`；`recpt_id BIGINT!`；`notice_line_id`；`item_id BIGINT!`；物料/单位快照；`quantity_recived DOUBLE!`；`batch_id`；仓库、库区、库位；生产/效期/批号；`iqc_check CHAR(1)`；`iqc_id BIGINT`；备注与时间 | `FK recpt_id`；入库数量大于零。 |
| `wm_issue_header` | `issue_id BIGINT UNSIGNED PK`；`issue_code VARCHAR(64)!`；`issue_name VARCHAR(255)!`；工位、工单、任务、客户 ID/快照；`required_time DATETIME`；`issue_date DATETIME`；`status VARCHAR(64)!`；备注与时间 | 领料单号唯一。 |
| `wm_issue_line` | `line_id BIGINT UNSIGNED PK`；`issue_id BIGINT!`；`item_id BIGINT!`；物料/单位快照；`quantity_issued DOUBLE!`；`batch_id`；`batch_code VARCHAR(255)`；备注与时间 | `FK issue_id`；领料数不超过可用数。 |
| `qc_template` | `template_id BIGINT UNSIGNED PK`；`template_code VARCHAR(64)!`；`template_name VARCHAR(255)!`；`qc_types VARCHAR(255)!`；`enable_flag CHAR(1)!`；审计 B | 模板编码唯一。 |
| `qc_template_index` | `record_id BIGINT UNSIGNED PK`；`template_id BIGINT!`；`qc_tool VARCHAR(255)`；`check_method VARCHAR(500)`；`stander_val DOUBLE(12,4)`；`unit_of_measure VARCHAR(64)`；`threshold_max/min DOUBLE(12,4)`；`doc_url VARCHAR(255)`；审计 B | `FK template_id`。 |
| `qc_iqc` | `iqc_id BIGINT UNSIGNED PK`；`iqc_code VARCHAR(64)!`；`iqc_name VARCHAR(500)!`；`template_id BIGINT!`；来源单据字段；供应商字段；`item_id BIGINT!`、物料快照；抽检规则、收货/检验/合格/不合格数量；缺陷率与数量；`check_result`、日期、检验员、`status`；审计 B | 关联模板、供应商、物料、来料来源。 |
| `qc_ipqc` | `ipqc_id BIGINT UNSIGNED PK`；`ipqc_code VARCHAR(64)!`；`ipqc_name VARCHAR(255)`；`ipqc_type VARCHAR(64)!`；`template_id BIGINT!`；来源单据；工单、任务、工位、工序、物料 ID/快照；检验及不良数量、缺陷率/数量、`check_result`、检验日期/人员、`status`；审计 B | 关联模板和生产执行来源。 |
| `qc_oqc` | `oqc_id BIGINT UNSIGNED PK`；`oqc_code VARCHAR(64)!`；`oqc_name VARCHAR(500)`；`template_id BIGINT!`；来源销售单；客户、批次、物料 ID/快照；发货/抽检/不良/合格数量、缺陷率/数量、`check_result`、出货和检验日期、人员、`status`；审计 B | 关联模板、客户、销售出库来源。 |
| `qc_rqc` | `rqc_id BIGINT UNSIGNED PK`；`rqc_code VARCHAR(64)!`；`rqc_name VARCHAR(500)`；`template_id BIGINT!`；来源退料单；`rqc_type`；物料/批次 ID 与快照；抽检/不良/合格数量、`check_result`、检验日期、人员、`status`；审计 B | 关联模板、退料来源。 |
| `dv_machinery` | `machinery_id BIGINT UNSIGNED PK`；`machinery_code VARCHAR(64)!`；`machinery_name VARCHAR(255)!`；品牌、规格；`machinery_type_id BIGINT!` 与类型快照；`workshop_id BIGINT!` 与车间快照；`last_mainten_time DATETIME(3)`；`last_check_time DATETIME(3)`；`status VARCHAR(64)! DEFAULT 'STOP'`；审计 B | 设备编码唯一；关联设备类型和车间。 |
| `pro_andon_record` | `record_id BIGINT UNSIGNED PK`；工位、用户、工单、工序 ID/编码/名称快照；`andon_reason VARCHAR(500)!`；`andon_level VARCHAR(64) DEFAULT 'LEVEL3'`；`handle_time DATETIME(3)`；处理人字段；`status VARCHAR(64)! DEFAULT 'ACTIVE'`；`attr1` 保存桥接事件 ID；审计 B | 关联现场资源、工单、工序与处理人员。 |

### 9.4 AI / Agent 字段结构

| 表 | 字段结构（字段名：类型；`!` 表示 NOT NULL） | 主键、索引与关联 |
|---|---|---|
| `agent_profile` | `agent_id BIGINT UNSIGNED PK`；`agent_code VARCHAR(64)!`；`agent_name VARCHAR(128)!`；`agent_type VARCHAR(64)!`；`owner_dept_id BIGINT`；`description VARCHAR(1000)`；`status VARCHAR(32)! DEFAULT 'DRAFT'`；审计 A | `agent_code` 唯一；`owner_dept_id → sys_department`。 |
| `llm_provider` | `provider_id BIGINT UNSIGNED PK`；`provider_code VARCHAR(64)!`；`provider_name VARCHAR(128)!`；`api_base_url VARCHAR(500)`；`auth_type VARCHAR(32)! DEFAULT 'API_KEY'`；`secret_ref VARCHAR(255)`；`status VARCHAR(32)!` | 提供商编码唯一；不保存明文密钥。 |
| `llm_model` | `model_id BIGINT UNSIGNED PK`；`provider_id BIGINT!`；`model_code/name VARCHAR(128)!`；`model_type VARCHAR(32)!`；`context_window INT`；输入/输出单价 `DECIMAL(18,8)`；`status VARCHAR(32)!` | `FK provider_id`；提供商内模型编码唯一。 |
| `agent_model_config` | `model_config_id BIGINT UNSIGNED PK`；`agent_id BIGINT!`；`model_id BIGINT!`；`purpose VARCHAR(32)! DEFAULT 'CHAT'`；`temperature DECIMAL(4,3)`；`top_p DECIMAL(4,3)`；`max_tokens INT`；`config_json JSON`；`status VARCHAR(32)!` | `FK agent_id,model_id`；Agent+purpose 唯一启用。 |
| `agent_prompt_template` | `prompt_id BIGINT UNSIGNED PK`；`prompt_code VARCHAR(64)!`；`prompt_name VARCHAR(128)!`；`prompt_type VARCHAR(32)!`；`description VARCHAR(500)`；状态与审计字段 | 提示词编码唯一。 |
| `agent_prompt_version` | `prompt_version_id BIGINT UNSIGNED PK`；`prompt_id BIGINT!`；`agent_id BIGINT`；`version_no VARCHAR(32)!`；`prompt_content MEDIUMTEXT!`；`status VARCHAR(32)!`；`created_at DATETIME(3)!`；`created_by BIGINT` | `FK prompt_id,agent_id,created_by`；模板版本唯一。 |
| `agent_connector` | `connector_id BIGINT UNSIGNED PK`；`connector_code VARCHAR(64)!`；`connector_name VARCHAR(128)!`；`connector_type VARCHAR(64)!`；`auth_type VARCHAR(32)!`；`endpoint VARCHAR(500)`；`secret_ref VARCHAR(255)`；`status VARCHAR(32)!` | 连接器编码唯一。 |
| `agent_tool` | `tool_id BIGINT UNSIGNED PK`；`connector_id BIGINT!`；`tool_code VARCHAR(64)!`；`tool_name VARCHAR(128)!`；`tool_type VARCHAR(64)!`；`risk_level VARCHAR(32)!`；`input_schema_json JSON`；`output_schema_json JSON`；`status VARCHAR(32)!` | `FK connector_id`；工具编码唯一。 |
| `agent_tool_permission` | `permission_id BIGINT UNSIGNED PK`；`agent_id BIGINT!`；`tool_id BIGINT!`；`allow_scope VARCHAR(255)! DEFAULT '*'`；`require_approval TINYINT(1)! DEFAULT 0`；`status VARCHAR(32)!` | `FK agent_id,tool_id`；Agent+工具唯一。 |
| `agent_session` | `session_id BIGINT UNSIGNED PK`；`agent_id BIGINT!`；`user_id BIGINT!`；`session_title VARCHAR(255)`；`session_status VARCHAR(32)! DEFAULT 'OPEN'`；`start_time DATETIME(3)!`；`end_time DATETIME(3)` | `FK agent_id,user_id`；用户会话索引。 |
| `agent_message` | `message_id BIGINT UNSIGNED PK`；`session_id BIGINT!`；`sender_type VARCHAR(32)!`；`sender_id BIGINT`；`message_content MEDIUMTEXT!`；`token_count INT`；`send_time DATETIME(3)!` | `FK session_id`；按会话时间建立索引。 |
| `agent_context_binding` | `binding_id BIGINT UNSIGNED PK`；`session_id BIGINT!`；`biz_object_type VARCHAR(64)!`；`biz_object_id BIGINT!`；`binding_reason VARCHAR(500)`；`created_at DATETIME(3)!` | `FK session_id`；对象类型+对象 ID 逻辑外键。 |
| `agent_task` | `task_id BIGINT UNSIGNED PK`；`session_id BIGINT`；`agent_id BIGINT!`；`task_type VARCHAR(64)!`；`task_title VARCHAR(255)!`；`task_status VARCHAR(32)! DEFAULT 'CREATED'`；`priority VARCHAR(32)! DEFAULT 'NORMAL'`；业务对象类型/ID；`created_at`、`finished_at` | `FK session_id,agent_id`。 |
| `agent_plan` | `plan_id BIGINT UNSIGNED PK`；`task_id BIGINT!`；`plan_status VARCHAR(32)! DEFAULT 'CREATED'`；`plan_summary VARCHAR(1000)`；`generated_time DATETIME(3)!` | `FK task_id`。 |
| `agent_plan_step` | `step_id BIGINT UNSIGNED PK`；`plan_id BIGINT!`；`step_seq INT!`；`step_type VARCHAR(64)!`；`step_desc VARCHAR(1000)!`；`step_status VARCHAR(32)! DEFAULT 'CREATED'` | `FK plan_id`；计划内步骤序号唯一。 |
| `agent_action` | `action_id BIGINT UNSIGNED PK`；`step_id BIGINT!`；`action_type VARCHAR(64)!`；业务对象类型/ID；`action_status VARCHAR(32)! DEFAULT 'CREATED'`；`created_at DATETIME(3)!`；`finished_at DATETIME(3)` | `FK step_id`。 |
| `agent_tool_call` | `tool_call_id BIGINT UNSIGNED PK`；`action_id BIGINT!`；`tool_id BIGINT!`；`request_payload JSON`；`response_payload JSON`；`call_status VARCHAR(32)! DEFAULT 'PENDING'`；`error_message VARCHAR(1000)`；`latency_ms INT`；`called_at DATETIME(3)!` | `FK action_id,tool_id`；调用日志需脱敏保存。 |
