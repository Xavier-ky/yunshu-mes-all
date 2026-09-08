/**
 * 侧边栏导航 — 10 大板块，统一二级结构（板块 → 页面）
 */

export const navYunshuAi = {
  id: "yunshu-ai",
  label: "云枢小智",
  icon: "yunshu",
  items: [{ path: "/app/yunshu-ai", label: "云枢小智" }],
};

export const navOverview = {
  id: "overview",
  label: "系统总览",
  icon: "dashboard",
  items: [
    { path: "/app/line-monitor", label: "产线监控" },
    { path: "/app", label: "生产大屏" },
  ],
};

export const navPlanning = {
  id: "planning",
  label: "计划调度",
  icon: "workorders",
  items: [
    { path: "/app/planning/orders", label: "订单中心" },
    { path: "/app/planning/work-orders", label: "工单中心" },
    { path: "/app/planning/scheduling", label: "排产派工" },
    { path: "/app/planning/progress", label: "生产进度" },
  ],
};

export const navInventory = {
  id: "inventory",
  label: "仓储管理",
  icon: "warehouse",
  items: [
    { path: "/app/inventory/wmstock", label: "库存现有量" },
    { path: "/app/inventory/inbound", label: "入库作业" },
    { path: "/app/inventory/outbound", label: "出库作业" },
    { path: "/app/inventory/barcode", label: "条码管理" },
    { path: "/app/inventory/package", label: "装箱管理" },
  ],
};

const productionItemsFull = [
  { path: "/app/factory/calendar", label: "排班日历" },
  { path: "/app/pro/feedback", label: "生产报工" },
  { path: "/app/pro/route", label: "工艺路线" },
  { path: "/app/factory/model", label: "工厂建模" },
  { path: "/app/master-data/mditem", label: "物料产品" },
  { path: "/app/my-work", label: "我的工位" },
];

export const navProductionMgmt = {
  id: "production-mgmt",
  label: "生产管理",
  icon: "factory",
  items: productionItemsFull,
};

export const navProductionMgmtSupervisor = {
  id: "production-mgmt",
  label: "生产管理",
  icon: "factory",
  items: productionItemsFull.filter((item) => item.path !== "/app/my-work"),
};

export const navProductionMgmtOperator = {
  id: "production-mgmt",
  label: "生产管理",
  icon: "factory",
  items: [{ path: "/app/my-work", label: "我的工位" }],
};

export const navQuality = {
  id: "quality",
  label: "质量管理",
  icon: "quality",
  items: [
    { path: "/app/quality", label: "质量分析中心" },
    { path: "/app/quality/workbench", label: "检验工作台" },
  ],
};

export const navAndon = {
  id: "andon",
  label: "安灯中心",
  icon: "andon",
  items: [
    { path: "/app/andon", label: "安灯看板" },
    { path: "/app/andon/launch", label: "发起安灯" },
  ],
};

export const navAndonOperator = {
  id: "andon",
  label: "安灯中心",
  icon: "andon",
  items: [
    { path: "/app/andon/launch", label: "发起安灯" },
    { path: "/app/andon", label: "安灯看板" },
  ],
};

export const navEquipmentFull = {
  id: "equipment",
  label: "设备管理",
  icon: "equipment",
  items: [
    { path: "/app/equipment/workbench", label: "设备工作台" },
    { path: "/app/equipment/machinery", label: "设备台账" },
    { path: "/app/equipment/check-maint", label: "点检保养" },
    { path: "/app/equipment/repairs", label: "维修中心" },
    { path: "/app/equipment/analytics", label: "设备分析" },
  ],
};

export const navEquipmentSupervisor = {
  id: "equipment",
  label: "设备管理",
  icon: "equipment",
  items: [
    { path: "/app/equipment/machinery", label: "设备台账" },
    { path: "/app/equipment/check-maint", label: "点检保养" },
    { path: "/app/equipment/analytics", label: "设备分析" },
  ],
};

export const navSystem = {
  id: "system",
  label: "系统管理",
  icon: "system",
  items: [
    { path: "/app/system/access", label: "权限与组织" },
    { path: "/app/system/settings", label: "系统配置" },
    { path: "/app/system/comms", label: "通知消息" },
    { path: "/app/system/audit", label: "安全审计" },
  ],
};

export const navAnalyticsFull = {
  id: "analytics",
  label: "分析集成",
  icon: "data",
  items: [
    { path: "/app/analytics/report-designer", label: "报表设计" },
    { path: "/app/analytics/print", label: "打印中心" },
    { path: "/app/analytics/trace", label: "追溯中心" },
    { path: "/app/analytics/integration", label: "集成中心" },
  ],
};

export const navAnalyticsStandard = {
  id: "analytics",
  label: "分析集成",
  icon: "data",
  items: [
    { path: "/app/analytics/report-designer", label: "报表设计" },
    { path: "/app/analytics/trace", label: "追溯中心" },
  ],
};

export const navAnalyticsTrace = {
  id: "analytics",
  label: "分析集成",
  icon: "data",
  items: [{ path: "/app/analytics/trace", label: "追溯中心" }],
};

export const navPlanningProgress = {
  id: "planning",
  label: "计划调度",
  icon: "workorders",
  items: [{ path: "/app/planning/progress", label: "生产进度" }],
};

export const navInventoryReadonly = {
  id: "inventory",
  label: "仓储管理",
  icon: "warehouse",
  items: [
    { path: "/app/inventory/wmstock", label: "库存现有量" },
    { path: "/app/inventory/barcode", label: "条码管理" },
  ],
};

export const navProductionOperator = {
  id: "production-mgmt",
  label: "生产管理",
  icon: "factory",
  items: [
    { path: "/app/my-work", label: "我的工位" },
    { path: "/app/pro/feedback", label: "生产报工" },
  ],
};

/** 从侧栏菜单项收集可访问路径（精确到菜单 path，避免 /app/planning 误放行全部计划页） */
export function collectRoutePrefixes(navModules, extraPrefixes = []) {
  const paths = new Set(extraPrefixes);
  const add = (raw) => {
    const base = String(raw || "").split("?")[0].replace(/\/+$/, "") || "/app";
    paths.add(base);
  };
  for (const mod of navModules || []) {
    for (const item of mod.items || []) {
      add(item.path);
      for (const child of item.children || []) {
        add(child.path);
      }
    }
  }
  return [...paths];
}

/** 所有角色共用的导航底座：云枢小智 + 系统总览（与超级管理员一致） */
export const navRoleBase = [navYunshuAi, navOverview];

export const superAdminNav = [
  navYunshuAi,
  navOverview,
  navPlanning,
  navInventory,
  navProductionMgmt,
  navQuality,
  navAndon,
  navEquipmentFull,
  navSystem,
  navAnalyticsFull,
];

export const navManager = [
  ...navRoleBase,
  navPlanning,
  navInventory,
  navProductionMgmtSupervisor,
  navQuality,
  navAndon,
  navEquipmentFull,
  navAnalyticsFull,
  navSystem,
];

export const navProdSupervisor = [
  ...navRoleBase,
  navPlanning,
  navInventory,
  navProductionMgmtSupervisor,
  navQuality,
  navAndon,
  navEquipmentSupervisor,
  navAnalyticsStandard,
];

export const navWarehouse = [
  ...navRoleBase,
  navInventory,
  navPlanningProgress,
  navAnalyticsTrace,
  navAndon,
];

export const navQualityRole = [
  ...navRoleBase,
  navQuality,
  navInventoryReadonly,
  navPlanningProgress,
  navAnalyticsTrace,
  navAndon,
];

export const navEquipment = [
  ...navRoleBase,
  navEquipmentFull,
  navPlanningProgress,
  navAndon,
];

export const navOperator = [
  ...navRoleBase,
  navProductionOperator,
  navAndonOperator,
];
