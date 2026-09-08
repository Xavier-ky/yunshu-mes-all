<template>
  <TabCrudPanel
    eyebrow="PRODUCTION"
    title="生产执行"
    subtitle="管理产品 SN、生产报工和完工记录，是生产追溯的核心数据。"
    default-tab="sns"
    :tabs="tabs"
    :status-options="statusOptions"
  />
</template>

<script setup>
import TabCrudPanel from "@/components/TabCrudPanel.vue";
import {
  fetchProductSns,
  createProductSn,
  updateProductSn,
  deleteProductSn,
  fetchProductionReports,
  createProductionReport,
  updateProductionReport,
  deleteProductionReport,
  fetchCompletions,
  createCompletion,
  updateCompletion,
  deleteCompletion,
} from "@/api/domain";

const statusOptions = [
  { value: "CREATED", label: "已创建" },
  { value: "PRODUCING", label: "生产中" },
  { value: "COMPLETED", label: "已完成" },
];

const tabs = [
  {
    key: "sns",
    label: "产品 SN",
    itemLabel: "产品 SN",
    idKey: "snId",
    searchPlaceholder: "产品SN / 工单号",
    fetchList: (p) => fetchProductSns(p),
    create: createProductSn,
    update: updateProductSn,
    del: deleteProductSn,
    columns: [
      { key: "snCode", label: "产品SN", mono: true },
      { key: "productName", label: "产品" },
      { key: "workOrderNo", label: "工单号" },
      { key: "status", label: "状态", type: "status" },
    ],
    formFields: [
      { key: "snCode", label: "产品SN", required: true, placeholder: "如 FAN-20240101-0001" },
      { key: "productId", label: "产品ID", required: true },
      { key: "workOrderId", label: "工单ID", required: true },
    ],
  },
  {
    key: "reports",
    label: "生产报工",
    itemLabel: "生产报工",
    idKey: "reportId",
    showStatusFilter: false,
    searchPlaceholder: "报工号 / 工单号",
    fetchList: (p) => fetchProductionReports(p),
    create: createProductionReport,
    update: updateProductionReport,
    del: deleteProductionReport,
    columns: [
      { key: "reportNo", label: "报工号", mono: true },
      { key: "workOrderNo", label: "工单号" },
      { key: "stepName", label: "工序" },
      { key: "reportType", label: "类型" },
      { key: "goodQty", label: "良品数" },
      { key: "defectQty", label: "不良数" },
    ],
    formFields: [
      { key: "reportNo", label: "报工号", required: true, placeholder: "如 RPT-001" },
      { key: "workOrderId", label: "工单ID", required: true },
      { key: "stepId", label: "工序ID", required: true },
      { key: "operatorId", label: "操作人ID", required: true },
      { key: "goodQty", label: "良品数" },
      { key: "defectQty", label: "不良数" },
    ],
  },
  {
    key: "completions",
    label: "完工记录",
    itemLabel: "完工记录",
    idKey: "completionId",
    showStatusFilter: false,
    searchPlaceholder: "完工单号 / 工单号",
    fetchList: (p) => fetchCompletions(p),
    create: createCompletion,
    update: updateCompletion,
    del: deleteCompletion,
    columns: [
      { key: "completionNo", label: "完工单号", mono: true },
      { key: "workOrderNo", label: "工单号" },
      { key: "completedQty", label: "完工数量" },
      { key: "defectQty", label: "不良数量" },
      { key: "status", label: "状态", type: "status" },
    ],
    formFields: [
      { key: "completionNo", label: "完工单号", required: true, placeholder: "如 CMP-001" },
      { key: "workOrderId", label: "工单ID", required: true },
      { key: "completedQty", label: "完工数量" },
      { key: "defectQty", label: "不良数量" },
    ],
  },
];
</script>
