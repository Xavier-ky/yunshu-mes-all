import { createRouter, createWebHistory } from "vue-router";
import NewAdminLayout from "@/layouts/NewAdminLayout.vue";
import LoginView from "@/views/login/LoginView.vue";
import PortalView from "@/views/portal/PortalView.vue";
import AgentCenterRoute from "@/views/agent/AgentCenterRoute.vue";
import DashboardHome from "@/views/new-dashboard/DashboardHome.vue";
import WorkshopManage from "@/views/new-dashboard/WorkshopManage.vue";
import LineManage from "@/views/new-dashboard/LineManage.vue";
import ProductManage from "@/views/new-dashboard/ProductManage.vue";
import BomManage from "@/views/new-dashboard/BomManage.vue";
import ProcessRouteManage from "@/views/new-dashboard/ProcessRouteManage.vue";
import MaterialManage from "@/views/new-dashboard/MaterialManage.vue";
import OrderManage from "@/views/new-dashboard/OrderManage.vue";
import WorkstationManage from "@/views/new-dashboard/WorkstationManage.vue";
import ShiftManage from "@/views/new-dashboard/ShiftManage.vue";
import LineMonitor from "@/views/new-dashboard/LineMonitor.vue";
import QualityManage from "@/views/new-dashboard/QualityManage.vue";
import OperatorWork from "@/views/new-dashboard/OperatorWork.vue";
import BarcodeManage from "@/views/new-dashboard/BarcodeManage.vue";
import ProductionExec from "@/views/new-dashboard/ProductionExec.vue";
import AgentHome from "@/views/yunshu-ai/AgentHome.vue";
import {
  canRoleAccessPath,
  resolveRoleCodeFromAuth,
  resolveDefaultHomePath,
} from "@/constants/role-access";
import { hasClientSession } from "@/utils/client-session";
import { getAuthUser, hasAuthToken } from "@/utils/auth-session";

const legacyRedirects = [
  { path: "/dashboard/workbench", redirect: "/app" },
  { path: "/system/users", redirect: "/app/system/access?tab=users" },
  { path: "/system/roles", redirect: "/app/system/access?tab=roles" },
  { path: "/system/permissions", redirect: "/app/system/access?tab=roles" },
  { path: "/system/logs", redirect: "/app/system/audit?tab=operlog" },
  { path: "/factory/workshops", redirect: "/app/factory/workshops" },
  { path: "/master-data/products", redirect: "/app/master-data/mditem" },
  { path: "/master-data/materials", redirect: "/app/master-data/mditem" },
  { path: "/process/routes", redirect: "/app/process/routes" },
  { path: "/barcode/rules", redirect: "/app/inventory/barcode" },
  { path: "/barcode", redirect: "/app/inventory/barcode" },
  { path: "/planning/work-orders", redirect: "/app/planning/work-orders" },
  { path: "/work-orders", redirect: "/app/planning/work-orders" },
  { path: "/inventory/batches", redirect: "/app/inventory/wmstock?mode=batch" },
  { path: "/production/tasks", redirect: "/app/pro/feedback" },
  { path: "/quality/tasks", redirect: "/app/quality/analytics" },
  { path: "/andon/events", redirect: "/app/andon" },
  { path: "/equipment/devices", redirect: "/app/equipment/machinery" },
  { path: "/equipment/maintenance", redirect: "/app/equipment/check-maint" },
  { path: "/traceability/product", redirect: "/app/analytics/trace?tab=workorder" },
  { path: "/reporting/output", redirect: "/app/analytics/report-designer?drawer=archive" },
  { path: "/integration/systems", redirect: "/app/analytics/integration?tab=systems" },
  { path: "/app/reports", redirect: "/app/analytics/report-designer?drawer=archive" },
  { path: "/app/trace", redirect: "/app/analytics/trace?tab=workorder" },
  { path: "/app/integration", redirect: "/app/analytics/integration?tab=systems" },
];

const routes = [
  {
    path: "/",
    name: "portal",
    component: PortalView,
    meta: { title: "首页" },
  },
  {
    path: "/login",
    name: "login",
    component: LoginView,
    meta: { title: "登录" },
  },
  {
    path: "/client",
    component: () => import("@/views/client/ClientLayout.vue"),
    meta: { title: "采购客户端", clientPortal: true },
    children: [
      {
        path: "",
        redirect: "/client/mall",
      },
      {
        path: "mall",
        name: "client-mall",
        component: () => import("@/views/client/ClientMallView.vue"),
        meta: { title: "采购商城", clientPortal: true },
      },
      {
        path: "orders",
        name: "client-orders",
        component: () => import("@/views/client/ClientOrderHistoryView.vue"),
        meta: { title: "我的订单", clientPortal: true },
      },
    ],
  },
  {
    path: "/big-screen",
    redirect: "/app",
  },
  {
    path: "/agent",
    name: "agent-center",
    component: AgentCenterRoute,
    meta: { title: "Agent 中心", allowedRoles: ["MANAGER", "TESTER"] },
  },
  ...legacyRedirects,
  {
    path: "/app",
    component: NewAdminLayout,
    children: [
      {
        path: "",
        name: "new-dashboard",
        component: () => import("@/views/new-dashboard/BigScreenView.vue"),
        meta: { title: "生产大屏", fullBleed: true, hideTabs: true, group: "监控" },
      },
      { path: "home", name: "dashboard-home", component: DashboardHome, meta: { title: "工作台", group: "监控" } },
      // 云枢小智的两条已接入工作流均已在后端明确允许生产主管：
      // 智能排产使用 PROD_SUPERVISOR 的订单/排产权限，质量分析仅读取
      // PROD_SUPERVISOR 可见的质量快照。这里同步前端声明，避免任何
      // 依赖 route.meta 的菜单、标签页或后续权限守卫误判为无权访问。
      { path: "yunshu-ai", name: "yunshu-ai", component: AgentHome, meta: { title: "云枢小智", fullBleed: true, allowedRoles: ["MANAGER", "PROD_SUPERVISOR", "TESTER"] } },
      { path: "line-monitor", name: "new-line-monitor", component: LineMonitor, meta: { title: "产线监控", fullBleed: true, allowedRoles: ["PROD_SUPERVISOR", "EQUIPMENT_MAINTAINER", "MANAGER"] } },
      { path: "work-orders", redirect: "/app/planning/work-orders" },
      { path: "planning/orders", name: "new-orders", component: () => import("@/yunshu-ui/planning/orders/index.vue"), meta: { title: "订单中心", yunshuUi: true, allowedRoles: ["PROD_SUPERVISOR", "MANAGER"] } },
      { path: "planning/work-orders", name: "new-work-orders", component: () => import("@/yunshu-ui/pro/workorder/index.vue"), meta: { title: "工单中心", yunshuUi: true, allowedRoles: ["PROD_SUPERVISOR", "MANAGER"] } },
      { path: "planning/scheduling", name: "new-scheduling", component: () => import("@/yunshu-ui/pro/schedule/index.vue"), meta: { title: "排产派工", yunshuUi: true, allowedRoles: ["PROD_SUPERVISOR", "MANAGER"] } },
      { path: "planning/scheduling/ganttedit", name: "planning-gantt-edit", component: () => import("@/yunshu-ui/pro/schedule/ganttedit.vue"), meta: { title: "甘特排产", yunshuUi: true, allowedRoles: ["PROD_SUPERVISOR", "MANAGER"] } },
      { path: "planning/progress", name: "new-planning-progress", component: () => import("@/yunshu-ui/pro/schedule/progress.vue"), meta: { title: "生产进度", yunshuUi: true, allowedRoles: ["PROD_SUPERVISOR", "MANAGER"] } },
      { path: "planning/tasks", redirect: (to) => ({ path: "/app/planning/scheduling", query: to.query }) },
      { path: "planning/dispatch", redirect: (to) => ({ path: "/app/planning/scheduling", query: to.query }) },
      { path: "planning/kitting", redirect: (to) => ({ path: "/app/planning/work-orders", query: to.query }) },
      { path: "production", redirect: "/app/pro/feedback" },
      { path: "pro/feedback", name: "new-pro-feedback", component: () => import("@/yunshu-ui/pro/feedback/index.vue"), meta: { title: "生产报工", yunshuUi: true, allowedRoles: ["PROD_SUPERVISOR", "MANAGER"] } },
      { path: "pro/route", name: "new-pro-route-hub", component: () => import("@/yunshu-ui/pro/route-hub/index.vue"), meta: { title: "工艺路线", yunshuUi: true, allowedRoles: ["PROD_SUPERVISOR", "MANAGER"] } },
      { path: "pro/process", redirect: "/app/pro/route?mode=process" },
      { path: "pro/proroute", redirect: "/app/pro/route?mode=proroute" },
      { path: "factory/model", name: "new-factory-model", component: () => import("@/yunshu-ui/md/factory-model/index.vue"), meta: { title: "工厂建模", yunshuUi: true, allowedRoles: ["PROD_SUPERVISOR", "MANAGER"] } },
      { path: "factory/workshops", redirect: "/app/factory/model?mode=workshop" },
      { path: "factory/lines", redirect: "/app/factory/model?mode=line" },
      { path: "factory/workstations", redirect: "/app/factory/model?mode=workstation" },
      { path: "factory/shifts", redirect: "/app/factory/calendar?mode=plan" },
      { path: "factory/cal-teams", redirect: "/app/factory/calendar?mode=team" },
      { path: "factory/holiday", redirect: "/app/factory/calendar?mode=holiday" },
      { path: "factory/calendar", name: "new-cal-calendar", component: () => import("@/yunshu-ui/cal/calendar/index.vue"), meta: { title: "排班日历", yunshuUi: true, allowedRoles: ["PROD_SUPERVISOR", "MANAGER"] } },
      { path: "master-data/mditem", name: "new-mditem", component: () => import("@/yunshu-ui/md/hub/index.vue"), meta: { title: "物料产品", yunshuUi: true, allowedRoles: ["PROD_SUPERVISOR", "MANAGER"] } },
      { path: "master-data/mditem/form/:itemId?", name: "new-mditem-form", component: () => import("@/yunshu-ui/md/mditem/form.vue"), meta: { title: "物料详情", yunshuUi: true, hidden: true, allowedRoles: ["PROD_SUPERVISOR", "MANAGER"] } },
      { path: "master-data/boms", redirect: "/app/master-data/mditem?mode=bom" },
      { path: "master-data/itemtype", redirect: "/app/master-data/mditem?mode=type" },
      { path: "master-data/unitmeasure", redirect: "/app/master-data/mditem?mode=unit" },
      { path: "master-data/clients", redirect: "/app/master-data/mditem?mode=client" },
      { path: "master-data/vendors", redirect: "/app/master-data/mditem?mode=vendor" },
      { path: "master-data/products", name: "new-products", component: ProductManage, meta: { title: "产品管理(旧)", allowedRoles: ["PROD_SUPERVISOR", "MANAGER"], hidden: true } },
      { path: "master-data/materials", name: "new-materials", component: MaterialManage, meta: { title: "物料管理(旧)", allowedRoles: ["PROD_SUPERVISOR", "MANAGER"], hidden: true } },
      { path: "process/routes", name: "new-routes", component: ProcessRouteManage, meta: { title: "工艺路线(旧)", allowedRoles: ["PROD_SUPERVISOR", "MANAGER"], hidden: true } },
      { path: "barcode", redirect: "/app/inventory/barcode" },
      { path: "production-exec", name: "new-production-exec", component: ProductionExec, meta: { title: "生产执行(旧)", allowedRoles: ["PROD_SUPERVISOR", "MANAGER"], hidden: true } },
      { path: "equipment/devices", redirect: "/app/equipment/machinery" },
      { path: "equipment/maintenance", redirect: "/app/equipment/check-maint" },
      { path: "equipment", redirect: "/app/equipment/workbench" },
      {
        path: "equipment/workbench",
        name: "dv-workbench",
        component: () => import("@/yunshu-ui/pro/dv/workbench/index.vue"),
        meta: { title: "设备工作台", yunshuUi: true, allowedRoles: ["EQUIPMENT_MAINTAINER", "PROD_SUPERVISOR", "MANAGER", "TESTER"] },
        children: [
          { path: "exec/check", name: "dv-wb-check", component: () => import("@/yunshu-ui/pro/dv/workbench/exec/check.vue"), meta: { title: "点检执行", yunshuUi: true, hidden: true, allowedRoles: ["EQUIPMENT_MAINTAINER", "PROD_SUPERVISOR", "MANAGER", "TESTER"] } },
          { path: "exec/mainten", name: "dv-wb-mainten", component: () => import("@/yunshu-ui/pro/dv/workbench/exec/mainten.vue"), meta: { title: "保养执行", yunshuUi: true, hidden: true, allowedRoles: ["EQUIPMENT_MAINTAINER", "PROD_SUPERVISOR", "MANAGER", "TESTER"] } },
          { path: "exec/repair", name: "dv-wb-repair", component: () => import("@/yunshu-ui/pro/dv/workbench/exec/repair.vue"), meta: { title: "维修执行", yunshuUi: true, hidden: true, allowedRoles: ["EQUIPMENT_MAINTAINER", "PROD_SUPERVISOR", "MANAGER", "TESTER"] } },
        ],
      },
      { path: "equipment/machinery", name: "dv-machinery", component: () => import("@/yunshu-ui/pro/dv/machinery/index.vue"), meta: { title: "设备台账", yunshuUi: true, allowedRoles: ["EQUIPMENT_MAINTAINER", "PROD_SUPERVISOR", "MANAGER", "TESTER"] } },
      { path: "equipment/check-maint", name: "dv-check-maint", component: () => import("@/yunshu-ui/pro/dv/check-maint/index.vue"), meta: { title: "点检保养", yunshuUi: true, allowedRoles: ["EQUIPMENT_MAINTAINER", "PROD_SUPERVISOR", "MANAGER", "TESTER"] } },
      { path: "equipment/repairs", name: "dv-repairs", component: () => import("@/yunshu-ui/pro/dv/repairs/index.vue"), meta: { title: "维修中心", yunshuUi: true, allowedRoles: ["EQUIPMENT_MAINTAINER", "PROD_SUPERVISOR", "MANAGER", "TESTER"] } },
      { path: "equipment/analytics", name: "dv-analytics", component: () => import("@/yunshu-ui/pro/dv/analytics/index.vue"), meta: { title: "设备分析", yunshuUi: true, allowedRoles: ["EQUIPMENT_MAINTAINER", "PROD_SUPERVISOR", "MANAGER", "TESTER"] } },
      { path: "equipment/machinery-type", redirect: (to) => ({ path: "/app/equipment/machinery", query: { panel: "type", ...to.query } }) },
      { path: "equipment/subjects", redirect: (to) => ({ path: "/app/equipment/check-maint", query: { tab: "subjects", ...to.query } }) },
      { path: "equipment/check-plans", redirect: (to) => ({ path: "/app/equipment/check-maint", query: { tab: "plans", ...to.query } }) },
      { path: "equipment/check-records", redirect: (to) => ({ path: "/app/equipment/check-maint", query: { tab: "records", type: "CHECK", ...to.query } }) },
      { path: "equipment/maintenance-records", redirect: (to) => ({ path: "/app/equipment/check-maint", query: { tab: "records", type: "MAINTEN", ...to.query } }) },
      { path: "equipment/maintenance-tasks", redirect: (to) => ({ path: "/app/equipment/workbench", query: { queue: "mainten", ...to.query } }) },
      { path: "equipment/manufacturers", redirect: (to) => ({ path: "/app/equipment/machinery", query: { drawer: "manufacturers", ...to.query } }) },
      { path: "equipment/fault-causes", redirect: (to) => ({ path: "/app/equipment/repairs", query: { drawer: "fault-causes", ...to.query } }) },
      { path: "equipment/oee", redirect: "/app/equipment/analytics" },
      { path: "quality", redirect: "/app/quality/analytics" },
      { path: "quality/analytics", name: "qc-analytics", component: () => import("@/yunshu-ui/qc/analytics/index.vue"), meta: { title: "质量分析中心", yunshuUi: true, allowedRoles: ["QUALITY_INSPECTOR", "PROD_SUPERVISOR", "MANAGER", "TESTER"] } },
      {
        path: "quality/workbench",
        name: "qc-workbench",
        component: () => import("@/yunshu-ui/qc/workbench/index.vue"),
        meta: { title: "检验工作台", yunshuUi: true, allowedRoles: ["QUALITY_INSPECTOR", "PROD_SUPERVISOR", "MANAGER", "TESTER"] },
        children: [
          { path: "exec/iqc", name: "qc-wb-iqc", component: () => import("@/yunshu-ui/qc/pendinginspect/iqc.vue"), meta: { title: "来料检验", yunshuUi: true, hidden: true, allowedRoles: ["QUALITY_INSPECTOR", "PROD_SUPERVISOR", "MANAGER", "TESTER"] } },
          { path: "exec/pqc", name: "qc-wb-pqc", component: () => import("@/yunshu-ui/qc/pendinginspect/pqc.vue"), meta: { title: "过程检验", yunshuUi: true, hidden: true, allowedRoles: ["QUALITY_INSPECTOR", "PROD_SUPERVISOR", "MANAGER", "TESTER"] } },
          { path: "exec/rqc", name: "qc-wb-rqc", component: () => import("@/yunshu-ui/qc/pendinginspect/rqc.vue"), meta: { title: "退料检验", yunshuUi: true, hidden: true, allowedRoles: ["QUALITY_INSPECTOR", "PROD_SUPERVISOR", "MANAGER", "TESTER"] } },
          { path: "exec/oqc", name: "qc-wb-oqc", component: () => import("@/yunshu-ui/qc/pendinginspect/oqc.vue"), meta: { title: "出货检验", yunshuUi: true, hidden: true, allowedRoles: ["QUALITY_INSPECTOR", "PROD_SUPERVISOR", "MANAGER", "TESTER"] } },
        ],
      },
      { path: "quality/tasks", redirect: "/app/quality/analytics" },
      { path: "quality/pending", redirect: "/app/quality/workbench" },
      { path: "quality/pending/add/iqc", redirect: (to) => ({ path: "/app/quality/workbench/exec/iqc", query: to.query }) },
      { path: "quality/pending/add/pqc", redirect: (to) => ({ path: "/app/quality/workbench/exec/pqc", query: to.query }) },
      { path: "quality/pending/add/rqc", redirect: (to) => ({ path: "/app/quality/workbench/exec/rqc", query: to.query }) },
      { path: "quality/pending/add/oqc", redirect: (to) => ({ path: "/app/quality/workbench/exec/oqc", query: to.query }) },
      { path: "quality/iqc", redirect: (to) => ({ path: "/app/quality/analytics", query: { drawer: "records", tab: "iqc", ...to.query } }) },
      { path: "quality/ipqc", redirect: (to) => ({ path: "/app/quality/analytics", query: { drawer: "records", tab: "ipqc", ...to.query } }) },
      { path: "quality/oqc", redirect: (to) => ({ path: "/app/quality/analytics", query: { drawer: "records", tab: "oqc", ...to.query } }) },
      { path: "quality/rqc", redirect: (to) => ({ path: "/app/quality/analytics", query: { drawer: "records", tab: "rqc", ...to.query } }) },
      { path: "quality/qctemplate", redirect: (to) => ({ path: "/app/quality/analytics", query: { drawer: "config", tab: "template", ...to.query } }) },
      { path: "quality/qcindex", redirect: (to) => ({ path: "/app/quality/analytics", query: { drawer: "config", tab: "index", ...to.query } }) },
      { path: "quality/qcdefect", redirect: (to) => ({ path: "/app/quality/analytics", query: { drawer: "config", tab: "defect", ...to.query } }) },
      { path: "quality/batchtrace", redirect: "/app/analytics/trace?tab=batch" },
      { path: "quality/tasks-legacy", name: "new-quality-tasks", component: QualityManage, meta: { title: "质检管理(旧)", allowedRoles: ["QUALITY_INSPECTOR", "PROD_SUPERVISOR", "MANAGER"], hidden: true } },
      { path: "analytics/print", name: "analytics-print-hub", component: () => import("@/yunshu-ui/analytics/hub/print/index.vue"), meta: { title: "打印中心", yunshuUi: true, fullBleed: true, allowedRoles: ["MANAGER", "TESTER"] } },
      { path: "analytics/insights", redirect: (to) => ({ path: "/app/analytics/report-designer", query: { drawer: "archive", ...to.query } }) },
      { path: "analytics/trace", name: "analytics-trace-hub", component: () => import("@/yunshu-ui/analytics/hub/trace/index.vue"), meta: { title: "追溯中心", yunshuUi: true, allowedRoles: ["QUALITY_INSPECTOR", "PROD_SUPERVISOR", "MANAGER", "TESTER", "WAREHOUSE_CLERK"] } },
      { path: "analytics/integration", name: "analytics-integration-hub", component: () => import("@/yunshu-ui/analytics/hub/integration/index.vue"), meta: { title: "集成中心", yunshuUi: true, allowedRoles: ["MANAGER", "TESTER"] } },
      { path: "analytics/reports", redirect: (to) => ({ path: "/app/analytics/report-designer", query: { drawer: "archive", ...to.query } }) },
      { path: "analytics/report-designer", name: "analytics-report-designer", component: () => import("@/yunshu-ui/analytics/report/designer/index.vue"), meta: { title: "报表设计", yunshuUi: true, fullBleed: true, allowedRoles: ["PROD_SUPERVISOR", "MANAGER", "TESTER"] } },
      { path: "analytics/charts", redirect: "/app/analytics/report-designer" },
      { path: "analytics/product-trace", redirect: (to) => ({ path: "/app/analytics/trace", query: { tab: "product", ...to.query } }) },
      { path: "analytics/batch-trace", redirect: (to) => ({ path: "/app/analytics/trace", query: { tab: "batch", ...to.query } }) },
      { path: "analytics/print-templates", redirect: (to) => ({ path: "/app/analytics/print", query: { ...to.query } }) },
      { path: "analytics/print-design", redirect: (to) => ({ path: "/app/analytics/print", query: { templateId: to.query.templateId, paperType: to.query.paperType, ...to.query } }) },
      { path: "analytics/print-clients", redirect: (to) => ({ path: "/app/analytics/print", query: { drawer: "clients", ...to.query } }) },
      { path: "analytics/integration/systems", redirect: (to) => ({ path: "/app/analytics/integration", query: { tab: "systems", ...to.query } }) },
      { path: "analytics/integration/endpoints", redirect: (to) => ({ path: "/app/analytics/integration", query: { tab: "endpoints", ...to.query } }) },
      { path: "analytics/integration/sync-logs", redirect: (to) => ({ path: "/app/analytics/integration", query: { tab: "logs", ...to.query } }) },
      { path: "reports", redirect: "/app/analytics/report-designer?drawer=archive" },
      { path: "trace", redirect: "/app/analytics/trace?tab=workorder" },
      { path: "integration", redirect: "/app/analytics/integration?tab=systems" },
      { path: "my-work", name: "new-mywork", component: () => import("@/yunshu-ui/pro/operator/index.vue"), meta: { title: "我的工位", yunshuUi: true, allowedRoles: ["LINE_OPERATOR", "TESTER"] } },
      { path: "inventory/wmstock", name: "new-inventory-wmstock", component: () => import("@/yunshu-ui/pro/wm/wmstock/index.vue"), meta: { title: "库存现有量", yunshuUi: true, allowedRoles: ["WAREHOUSE_CLERK", "PROD_SUPERVISOR", "MANAGER"] } },
      { path: "inventory/inbound", name: "new-inventory-inbound", component: () => import("@/yunshu-ui/pro/wm/inbound/index.vue"), meta: { title: "入库作业", yunshuUi: true, allowedRoles: ["WAREHOUSE_CLERK", "PROD_SUPERVISOR", "MANAGER"] } },
      { path: "inventory/outbound", name: "new-inventory-outbound", component: () => import("@/yunshu-ui/pro/wm/outbound/index.vue"), meta: { title: "出库作业", yunshuUi: true, allowedRoles: ["WAREHOUSE_CLERK", "PROD_SUPERVISOR", "MANAGER"] } },
      { path: "inventory/warehouses", redirect: "/app/inventory/wmstock?mode=warehouse" },
      { path: "inventory/locations", name: "new-inventory-locations", component: () => import("@/yunshu-ui/pro/wm/location/index.vue"), meta: { title: "库区设置", yunshuUi: true, hidden: true, allowedRoles: ["WAREHOUSE_CLERK", "PROD_SUPERVISOR", "MANAGER"] } },
      { path: "inventory/areas", name: "new-inventory-areas", component: () => import("@/yunshu-ui/pro/wm/area/index.vue"), meta: { title: "库位设置", yunshuUi: true, hidden: true, allowedRoles: ["WAREHOUSE_CLERK", "PROD_SUPERVISOR", "MANAGER"] } },
      { path: "inventory/wm-batch", redirect: "/app/inventory/wmstock?mode=batch" },
      { path: "inventory/batches", redirect: "/app/inventory/wmstock?mode=batch" },
      { path: "inventory/item-recpt", redirect: "/app/inventory/inbound?tab=item-recpt" },
      { path: "inventory/product-recpt", redirect: "/app/inventory/inbound?tab=product-recpt" },
      { path: "inventory/rt-issue", redirect: "/app/inventory/inbound?tab=rt-issue" },
      { path: "inventory/rt-sales", redirect: "/app/inventory/inbound?tab=rt-sales" },
      { path: "inventory/issue", redirect: "/app/inventory/outbound?tab=issue" },
      { path: "inventory/product-sales", redirect: "/app/inventory/outbound?tab=product-sales" },
      { path: "inventory/rt-vendor", redirect: "/app/inventory/outbound?tab=rt-vendor" },
      { path: "inventory/product-sales/form", name: "new-inventory-product-sales-form", component: () => import("@/yunshu-ui/pro/wm/productsales/form.vue"), meta: { title: "销售出库单", yunshuUi: true, hidden: true, allowedRoles: ["WAREHOUSE_CLERK", "PROD_SUPERVISOR", "MANAGER"] } },
      { path: "inventory/barcode", name: "new-inventory-barcode", component: () => import("@/yunshu-ui/pro/wm/barcode/index.vue"), meta: { title: "条码清单", yunshuUi: true, allowedRoles: ["WAREHOUSE_CLERK", "PROD_SUPERVISOR", "MANAGER"] } },
      { path: "inventory/package", name: "new-inventory-package", component: () => import("@/yunshu-ui/pro/wm/package/index.vue"), meta: { title: "装箱管理", yunshuUi: true, allowedRoles: ["WAREHOUSE_CLERK", "PROD_SUPERVISOR", "MANAGER"] } },
      { path: "inventory/requisitions", redirect: "/app/inventory/outbound?tab=issue" },
      { path: "inventory/issues", redirect: "/app/inventory/outbound?tab=issue" },
      { path: "inventory/returns", redirect: "/app/inventory/inbound?tab=rt-issue" },
      { path: "andon", name: "new-andon-board", component: () => import("@/yunshu-ui/pro/andon/hub/board/index.vue"), meta: { title: "安灯看板", yunshuUi: true, group: "事件", allowedRoles: ["MANAGER", "PROD_SUPERVISOR", "WAREHOUSE_CLERK", "QUALITY_INSPECTOR", "EQUIPMENT_MAINTAINER", "LINE_OPERATOR", "TESTER"] } },
      { path: "andon/launch", name: "new-andon-launch", component: () => import("@/yunshu-ui/pro/andon/hub/launch/index.vue"), meta: { title: "发起安灯", yunshuUi: true, allowedRoles: ["MANAGER", "PROD_SUPERVISOR", "WAREHOUSE_CLERK", "QUALITY_INSPECTOR", "EQUIPMENT_MAINTAINER", "LINE_OPERATOR", "TESTER"] } },
      { path: "andon/handle", redirect: (to) => ({ path: "/app/andon", query: { view: "handle", ...to.query } }) },
      { path: "andon/records", redirect: (to) => ({ path: "/app/andon", query: { view: "handle", ...to.query } }) },
      { path: "andon/types", redirect: "/app/andon" },
      { path: "andon/reasons", redirect: "/app/andon" },
      { path: "andon/tasks", redirect: (to) => ({ path: "/app/andon", query: { view: "handle", tab: "tasks", ...to.query } }) },
      { path: "andon/analytics", redirect: "/app/andon" },
      { path: "system/access", name: "sys-access", component: () => import("@/yunshu-ui/system/hub/access/index.vue"), meta: { title: "权限与组织", yunshuUi: true, allowedRoles: ["MANAGER", "TESTER"] } },
      { path: "system/settings", name: "sys-settings", component: () => import("@/yunshu-ui/system/hub/settings/index.vue"), meta: { title: "系统配置", yunshuUi: true, allowedRoles: ["MANAGER", "TESTER"] } },
      { path: "system/comms", name: "sys-comms", component: () => import("@/yunshu-ui/system/hub/comms/index.vue"), meta: { title: "通知消息", yunshuUi: true, allowedRoles: ["MANAGER", "TESTER"] } },
      { path: "system/audit", name: "sys-audit", component: () => import("@/yunshu-ui/system/hub/audit/index.vue"), meta: { title: "安全审计", yunshuUi: true, allowedRoles: ["MANAGER", "TESTER"] } },
      { path: "system/users", redirect: (to) => ({ path: "/app/system/access", query: { tab: "users", ...to.query } }) },
      { path: "system/roles", redirect: (to) => ({ path: "/app/system/access", query: { tab: "roles", ...to.query } }) },
      { path: "system/departments", redirect: (to) => ({ path: "/app/system/access", query: { tab: "departments", ...to.query } }) },
      { path: "system/posts", redirect: (to) => ({ path: "/app/system/access", query: { tab: "posts", ...to.query } }) },
      { path: "system/dict", redirect: (to) => ({ path: "/app/system/settings", query: { tab: "dict", ...to.query } }) },
      { path: "system/config", redirect: (to) => ({ path: "/app/system/settings", query: { tab: "config", ...to.query } }) },
      { path: "system/autocode", redirect: (to) => ({ path: "/app/system/settings", query: { tab: "autocode", ...to.query } }) },
      { path: "system/notices", redirect: (to) => ({ path: "/app/system/comms", query: { tab: "notices", ...to.query } }) },
      { path: "system/message", redirect: (to) => ({ path: "/app/system/comms", query: { tab: "message", ...to.query } }) },
      { path: "system/operlog", redirect: (to) => ({ path: "/app/system/audit", query: { tab: "operlog", ...to.query } }) },
      { path: "system/logininfor", redirect: (to) => ({ path: "/app/system/audit", query: { tab: "logininfor", ...to.query } }) },
      { path: "system/users-legacy", name: "sys-users", component: () => import("@/yunshu-ui/system/user/index.vue"), meta: { title: "用户管理", yunshuUi: true, hidden: true, allowedRoles: ["MANAGER", "TESTER"] } },
      { path: "system/user-auth/role/:userId", name: "sys-user-auth-role", component: () => import("@/yunshu-ui/system/user/authRole.vue"), meta: { title: "分配角色", yunshuUi: true, hidden: true, allowedRoles: ["MANAGER", "TESTER"] } },
      { path: "system/profile", name: "sys-profile", component: () => import("@/yunshu-ui/system/user/profile/index.vue"), meta: { title: "个人中心", yunshuUi: true, hidden: true, allowedRoles: ["MANAGER", "TESTER"] } },
      { path: "system/roles-legacy", name: "sys-roles", component: () => import("@/yunshu-ui/system/role/index.vue"), meta: { title: "角色管理", yunshuUi: true, hidden: true, allowedRoles: ["MANAGER", "TESTER"] } },
      { path: "system/role-auth/user/:roleId", name: "sys-role-auth-user", component: () => import("@/yunshu-ui/system/role/authUser.vue"), meta: { title: "分配用户", yunshuUi: true, hidden: true, allowedRoles: ["MANAGER", "TESTER"] } },
      { path: "system/departments-legacy", name: "sys-departments", component: () => import("@/yunshu-ui/system/dept/index.vue"), meta: { title: "部门管理", yunshuUi: true, hidden: true, allowedRoles: ["MANAGER", "TESTER"] } },
      { path: "system/posts-legacy", name: "sys-posts", component: () => import("@/yunshu-ui/system/post/index.vue"), meta: { title: "岗位管理", yunshuUi: true, hidden: true, allowedRoles: ["MANAGER", "TESTER"] } },
      { path: "system/menus", name: "sys-menus", component: () => import("@/yunshu-ui/system/menu/index.vue"), meta: { title: "菜单管理", yunshuUi: true, hidden: true, allowedRoles: ["MANAGER", "TESTER"] } },
      { path: "system/dict-legacy", name: "sys-dict", component: () => import("@/yunshu-ui/system/dict/index.vue"), meta: { title: "字典管理", yunshuUi: true, hidden: true, allowedRoles: ["MANAGER", "TESTER"] } },
      { path: "system/dict-data/:dictId", redirect: (to) => ({ path: "/app/system/settings", query: { tab: "dict", dictId: to.params.dictId } }) },
      { path: "system/config-legacy", name: "sys-config", component: () => import("@/yunshu-ui/system/config/index.vue"), meta: { title: "参数设置", yunshuUi: true, hidden: true, allowedRoles: ["MANAGER", "TESTER"] } },
      { path: "system/notices-legacy", name: "sys-notices", component: () => import("@/yunshu-ui/system/notice/index.vue"), meta: { title: "通知公告", yunshuUi: true, hidden: true, allowedRoles: ["MANAGER", "TESTER"] } },
      { path: "system/autocode-legacy", name: "sys-autocode", component: () => import("@/yunshu-ui/system/autocode/index.vue"), meta: { title: "编码规则", yunshuUi: true, hidden: true, allowedRoles: ["MANAGER", "TESTER"] } },
      { path: "system/autocode-part/:ruleId", redirect: (to) => ({ path: "/app/system/settings", query: { tab: "autocode", ruleId: to.params.ruleId } }) },
      { path: "system/message-legacy", name: "sys-message", component: () => import("@/yunshu-ui/system/message/index.vue"), meta: { title: "消息管理", yunshuUi: true, hidden: true, allowedRoles: ["MANAGER", "TESTER"] } },
      { path: "system/operlog-legacy", name: "sys-operlog", component: () => import("@/yunshu-ui/monitor/operlog/index.vue"), meta: { title: "操作日志", yunshuUi: true, hidden: true, allowedRoles: ["MANAGER", "TESTER"] } },
      { path: "system/logininfor-legacy", name: "sys-logininfor", component: () => import("@/yunshu-ui/monitor/logininfor/index.vue"), meta: { title: "登录日志", yunshuUi: true, hidden: true, allowedRoles: ["MANAGER", "TESTER"] } },
      { path: "system/logs", redirect: "/app/system/audit?tab=operlog" },
      { path: "system/permissions", redirect: "/app/system/access?tab=roles" },
      { path: "system", redirect: "/app/system/access?tab=users" },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

const PUBLIC_PATHS = new Set(["/", "/login"]);

router.beforeEach((to) => {
  if (PUBLIC_PATHS.has(to.path)) {
    return true;
  }
  if (to.matched.some((r) => r.meta.clientPortal)) {
    if (!hasClientSession()) {
      return { path: "/login" };
    }
    return true;
  }
  if (!hasAuthToken()) {
    return { path: "/login", query: { redirect: to.fullPath } };
  }
  const user = getAuthUser() || {};
  const roleCode = resolveRoleCodeFromAuth(user);
  if (!canRoleAccessPath(roleCode, to.path)) {
    return { path: resolveDefaultHomePath(roleCode) };
  }
  return true;
});

router.afterEach((to) => {
  document.title = `${to.meta.title || "工作台"} - 云枢智造 MES`;
});

export default router;
