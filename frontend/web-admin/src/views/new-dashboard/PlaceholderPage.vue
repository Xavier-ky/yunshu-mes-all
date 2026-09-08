<template>
  <div class="placeholder-page">
    <div class="ph-header">
      <h2>{{ route.meta.title }}</h2>
      <p>{{ description }}</p>
    </div>
    <div class="ph-empty">
      <div class="ph-empty-inner">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#d1d5db" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="9" y1="21" x2="9" y2="9"/></svg>
        <p>此模块即将上线</p>
        <small>当前页面为框架占位，功能内容将在后续迭代中交付。</small>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();

const descriptions = {
  "计划工单": "订单接入、生产工单创建、齐套分析与派工任务下达。",
  "仓储物料": "仓库管理、库存批次、领料发料与库存流水跟踪。",
  "生产执行": "产线任务、工位作业、SN绑定与实时报工。",
  "质量管理": "质检任务、检验记录、不良品管理与质量放行。",
  "设备管理": "设备台账、点检保养、故障报修与OEE分析。",
  "安灯异常": "异常事件发起、通知派发、处理关闭与统计分析。",
  "报表分析": "产量、质量、OEE、能耗与产品追溯报表。",
  "产品追溯": "产品SN串联物料批次、工序记录与质检数据全链路追溯。",
  "系统设置": "用户管理、角色权限、部门组织与系统参数配置。",
};

const description = computed(() => descriptions[route.meta.title] || "功能模块建设中。");
</script>

<style scoped>
.placeholder-page { display: grid; gap: 20px; }
.ph-header h2 { margin: 0; font-size: 22px; font-weight: 700; }
.ph-header p { margin: 4px 0 0; color: #6b7280; font-size: 14px; }
.ph-empty { background: rgba(255,255,255,0.45); backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px); border: 1px solid rgba(255,255,255,0.5); box-shadow: 0 0 0 1px rgba(255,255,255,0.5), 0 0 24px rgba(180,190,205,0.12); border-radius: 0; min-height: 360px; display: grid; place-items: center; }
.ph-empty-inner { text-align: center; }
.ph-empty-inner p { margin: 16px 0 4px; font-size: 15px; color: #9ca3af; font-weight: 500; }
.ph-empty-inner small { font-size: 13px; color: #d1d5db; }
</style>
