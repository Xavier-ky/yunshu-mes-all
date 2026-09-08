# IPQC 判定、模板与质量门禁规则

> 权威来源：`quality/compat/controller/QcIpqcController.java`、`quality/compat/QcTemplateResolver.java`、`quality/compat/QcFinishWriteback.java`、`planning/workflow/WorkOrderLifecycleService.java`  
> 数据库来源：`V15__qc_ktg_quality.sql`；适用范围：黄金流程第 8 步（过程质检）；版本 `0.1`；生效日期 `2026-07-15`。

## 检验单、模板和明细

IPQC 单据表为 `qc_ipqc`，主键 `ipqc_id`，通过 `workorder_id` 或 `workorder_code` 回连工单；检验项目明细存储在 `qc_ipqc_line`。创建接口为 `POST /api/mes/qc/ipqc`，更新/完结接口为 `PUT /api/mes/qc/ipqc`。

创建时系统必须解析 `template_id`：优先匹配 `qc_template_product.item_id`，再匹配 `item_code`，最后选择启用且适用 PQC 的默认 `qc_template`。模板确定后，系统从 `qc_template_index` 生成 `qc_ipqc_line`。因此 Agent 不能把某产品的固定检验项目当作通用事实；应使用已发布的模板快照或实时模板工具确认。

## 完结与工单生命周期

只有当 IPQC 请求状态被更新为 `FINISHED` 时，系统才执行质量回写和生命周期推进。按 `check_result` 判定：

- `ACCEPT`：工单推进到 `QC_PASSED`。
- 任何非 `ACCEPT` 的结果：工单推进到 `QC_FAILED`。

IPQC 完结还会把报工待检量 `pro_feedback.quantity_uncheck` 清零，并在有关联产出单时更新/拆分 `wm_product_produce_line` 的合格和不合格数量及 `quality_status`。混合合格/不合格时，系统会拆出不合格产出行；所有产出行不再处于待检状态时，`wm_product_produce` 才更新为 `FINISHED`。

## 质量门禁与异常处理

`QC_PASSED` 是成品入库的后端门禁；`QC_FAILED` 不可直接入库。质量不通过后的返工、复检或处置结论必须以实际质量单和生命周期快照为准，Agent 只能提出待审批建议，不得绕过 `WorkOrderLifecycleService.validateProductRecpt`。

## 下游连接

只有工单真实处于 `QC_PASSED`（或已经 `COMPLETED`）时，仓库才能按工单创建并执行成品入库单。具体本次检验结论、缺陷数量和模板版本属于实时业务事实，应通过 IPQC 查询接口读取。
