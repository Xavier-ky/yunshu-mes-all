<template>
  <TabCrudPanel
    eyebrow="BARCODE"
    title="条码应用"
    subtitle="维护条码类型、规则和标签模板，是产品追溯的入口。"
    default-tab="types"
    :tabs="tabs"
  />
</template>

<script setup>
import TabCrudPanel from "@/components/TabCrudPanel.vue";
import {
  fetchBarcodeTypes,
  createBarcodeType,
  updateBarcodeType,
  deleteBarcodeType,
  fetchBarcodeRules,
  createBarcodeRule,
  updateBarcodeRule,
  deleteBarcodeRule,
  fetchBarcodeTemplates,
  createBarcodeTemplate,
  updateBarcodeTemplate,
  deleteBarcodeTemplate,
} from "@/api/domain";

const tabs = [
  {
    key: "types",
    label: "条码类型",
    itemLabel: "条码类型",
    idKey: "typeId",
    searchPlaceholder: "类型编码 / 名称",
    fetchList: (p) => fetchBarcodeTypes(p),
    create: createBarcodeType,
    update: updateBarcodeType,
    del: deleteBarcodeType,
    columns: [
      { key: "typeCode", label: "类型编码", mono: true },
      { key: "typeName", label: "类型名称" },
      { key: "status", label: "状态", type: "status" },
    ],
    formFields: [
      { key: "typeCode", label: "类型编码", required: true, placeholder: "如 PRODUCT / MATERIAL" },
      { key: "typeName", label: "类型名称", required: true, placeholder: "如 产品码" },
    ],
  },
  {
    key: "rules",
    label: "条码规则",
    itemLabel: "条码规则",
    idKey: "ruleId",
    searchPlaceholder: "规则编码 / 名称",
    fetchList: (p) => fetchBarcodeRules(p),
    create: createBarcodeRule,
    update: updateBarcodeRule,
    del: deleteBarcodeRule,
    columns: [
      { key: "ruleCode", label: "规则编码", mono: true },
      { key: "ruleName", label: "规则名称" },
      { key: "typeName", label: "条码类型" },
      { key: "status", label: "状态", type: "status" },
    ],
    formFields: [
      { key: "ruleCode", label: "规则编码", required: true, placeholder: "如 RULE-001" },
      { key: "ruleName", label: "规则名称", required: true, placeholder: "如 产品码规则" },
      { key: "typeId", label: "条码类型ID", placeholder: "关联 barcode_type" },
    ],
  },
  {
    key: "templates",
    label: "标签模板",
    itemLabel: "标签模板",
    idKey: "templateId",
    searchPlaceholder: "模板编码 / 名称",
    fetchList: (p) => fetchBarcodeTemplates(p),
    create: createBarcodeTemplate,
    update: updateBarcodeTemplate,
    del: deleteBarcodeTemplate,
    columns: [
      { key: "templateCode", label: "模板编码", mono: true },
      { key: "templateName", label: "模板名称" },
      { key: "status", label: "状态", type: "status" },
    ],
    formFields: [
      { key: "templateCode", label: "模板编码", required: true, placeholder: "如 TPL-001" },
      { key: "templateName", label: "模板名称", required: true, placeholder: "如 产品标签模板" },
    ],
  },
];
</script>
