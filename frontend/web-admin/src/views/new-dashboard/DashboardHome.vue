<template>
  <div class="dash-root">
    <!-- ====== MANAGER ====== -->
    <template v-if="role==='MANAGER'">
      <div class="hero-banner" :style="{backgroundImage:`url(${banners[bannerIdx].image})`}">
        <div class="hero-overlay"></div>
        <div class="hero-content">
          <h2>{{ banners[bannerIdx].title }}</h2>
          <p>{{ banners[bannerIdx].desc }}</p>
          <div class="hero-dots"><span v-for="(s,i) in banners" :key="i" class="hero-dot" :class="{on:bannerIdx===i}" @click="bannerIdx=i"></span></div>
        </div>
      </div>
      <div class="welcome-row"><h2>{{ app.user.name }}，欢迎回来</h2><span class="dash-clock">{{ now }}</span></div>
      <div class="dash-kpi-row">
        <div class="dash-kpi"><span class="kl">今日在线用户</span><strong class="kv kpi-blue">{{ users.length }}</strong></div>
        <div class="dash-kpi"><span class="kl">工单总数</span><strong class="kv">{{ workOrders.length }}</strong></div>
        <div class="dash-kpi"><span class="kl">生产中</span><strong class="kv kpi-blue">{{ workOrders.filter(w=>w.status==='RUNNING').length }}</strong></div>
        <div class="dash-kpi"><span class="kl">设备总数</span><strong class="kv">{{ devices.length }}</strong></div>
        <div class="dash-kpi"><span class="kl">库存批次</span><strong class="kv">{{ batches.length }}</strong></div>
        <div class="dash-kpi"><span class="kl">待处理安灯</span><strong class="kv kpi-red">{{ andonEvents.filter(e=>e.status==='OPEN').length }}</strong></div>
      </div>
      <div class="dash-grid-2col">
        <div class="dash-card">
          <h3>待办工作</h3>
          <div class="card-stats"><div class="stat-item" v-for="a in mgrAlerts" :key="a.id"><span class="stat-dot" :class="a.level"></span><span class="stat-text">{{ a.text }}</span></div></div>
          <div v-if="!mgrAlerts.length" class="empty-hint">暂无待办事项</div>
        </div>
        <div class="dash-card">
          <h3>快捷操作</h3>
          <div class="quick-grid-2"><RouterLink v-for="q in mgrQuick" :key="q.path" :to="q.path" class="quick-card-2"><span class="qc-icon" :style="{background:q.color}">{{ q.icon }}</span><span class="qc-label">{{ q.label }}</span></RouterLink></div>
        </div>
      </div>
    </template>

    <!-- ====== PROD_SUPERVISOR ====== -->
    <template v-else-if="role==='PROD_SUPERVISOR'">
      <div class="hero-banner" style="background:linear-gradient(135deg,#1e3a5f 0%,#2563eb 50%,#1e40af 100%)">
        <div class="hero-content"><h2>生产驾驶舱</h2><p>实时掌控工单进度、物料齐套与产线状态</p></div>
      </div>
      <div class="welcome-row"><h2>{{ app.user.name }}，欢迎回来</h2><span class="dash-clock">{{ now }}</span></div>
      <div class="dash-kpi-row">
        <div class="dash-kpi"><span class="kl">工单总数</span><strong class="kv kpi-blue">{{ workOrders.length }}</strong></div>
        <div class="dash-kpi"><span class="kl">生产中</span><strong class="kv">{{ workOrders.filter(w=>w.status==='RUNNING').length }}</strong></div>
        <div class="dash-kpi"><span class="kl">已完成</span><strong class="kv kpi-green">{{ workOrders.filter(w=>w.status==='COMPLETED').length }}</strong></div>
        <div class="dash-kpi"><span class="kl">总计划量</span><strong class="kv">{{ workOrders.reduce((s,w)=>s+(Number(w.planQty)||0),0) }}</strong></div>
        <div class="dash-kpi"><span class="kl">欠料预警</span><strong class="kv kpi-red">{{ shortages.length }}</strong></div>
        <div class="dash-kpi"><span class="kl">待处理安灯</span><strong class="kv kpi-red">{{ andonEvents.filter(e=>e.status==='OPEN').length }}</strong></div>
      </div>
      <div class="dash-grid-2col">
        <div class="dash-card"><h3>待办事项</h3><div class="card-stats"><div class="stat-item" v-for="a in supAlerts" :key="a.id"><span class="stat-dot" :class="a.level"></span><span class="stat-text">{{ a.text }}</span></div></div><div v-if="!supAlerts.length" class="empty-hint">暂无待办</div></div>
        <div class="dash-card"><h3>快捷操作</h3><div class="quick-grid-2"><RouterLink v-for="q in supQuick" :key="q.path" :to="q.path" class="quick-card-2"><span class="qc-icon" :style="{background:q.color}">{{ q.icon }}</span><span class="qc-label">{{ q.label }}</span></RouterLink></div></div>
      </div>
      <WorkorderProgressStrip />
    </template>

    <!-- ====== WAREHOUSE_CLERK ====== -->
    <template v-else-if="role==='WAREHOUSE_CLERK'">
      <div class="hero-banner" style="background:linear-gradient(135deg,#0f4c2f 0%,#059669 50%,#065f46 100%)">
        <div class="hero-content"><h2>仓库工作台</h2><p>备料 · 领料 · 发料 · 退料 — 物料流转全闭环</p></div>
      </div>
      <div class="welcome-row"><h2>{{ app.user.name }}，欢迎回来</h2><span class="dash-clock">{{ now }}</span></div>
      <div class="dash-kpi-row">
        <div class="dash-kpi"><span class="kl">库存批次</span><strong class="kv kpi-blue">{{ batches.length }}</strong></div>
        <div class="dash-kpi"><span class="kl">总库存量</span><strong class="kv">{{ batches.reduce((s,b)=>s+(Number(b.availableQty)||0),0).toLocaleString() }}</strong></div>
        <div class="dash-kpi"><span class="kl">在库批次</span><strong class="kv kpi-green">{{ batches.filter(b=>b.status==='IN_STOCK').length }}</strong></div>
        <div class="dash-kpi"><span class="kl">欠料预警</span><strong class="kv kpi-red">{{ shortages.length }}</strong></div>
        <div class="dash-kpi"><span class="kl">锁定量</span><strong class="kv">{{ batches.reduce((s,b)=>s+(Number(b.lockedQty)||0),0) }}</strong></div>
        <div class="dash-kpi"><span class="kl">待处理安灯</span><strong class="kv kpi-red">{{ andonEvents.filter(e=>e.status==='OPEN').length }}</strong></div>
      </div>
      <div class="dash-grid-2col">
        <div class="dash-card"><h3>待办事项</h3><div class="card-stats"><div class="stat-item" v-for="a in whAlerts" :key="a.id"><span class="stat-dot" :class="a.level"></span><span class="stat-text">{{ a.text }}</span></div></div><div v-if="!whAlerts.length" class="empty-hint">暂无待办</div></div>
        <div class="dash-card"><h3>快捷操作</h3><div class="quick-grid-2"><RouterLink v-for="q in whQuick" :key="q.path" :to="q.path" class="quick-card-2"><span class="qc-icon" :style="{background:q.color}">{{ q.icon }}</span><span class="qc-label">{{ q.label }}</span></RouterLink></div></div>
      </div>
    </template>

    <!-- ====== QUALITY_INSPECTOR ====== -->
    <template v-else-if="role==='QUALITY_INSPECTOR'">
      <div class="hero-banner" style="background:linear-gradient(135deg,#5f1e7a 0%,#7c3aed 50%,#6d28d9 100%)">
        <div class="hero-content"><h2>质检工作台</h2><p>首件检验 · 过程巡检 · 成品放行 — 质量全链路保障</p></div>
      </div>
      <div class="welcome-row"><h2>{{ app.user.name }}，欢迎回来</h2><span class="dash-clock">{{ now }}</span></div>
      <div class="dash-kpi-row">
        <div class="dash-kpi"><span class="kl">待检验</span><strong class="kv kpi-blue">{{ qualityTasks.filter(t=>t.status==='PENDING').length }}</strong></div>
        <div class="dash-kpi"><span class="kl">检验中</span><strong class="kv">{{ qualityTasks.filter(t=>t.status==='IN_PROGRESS').length }}</strong></div>
        <div class="dash-kpi"><span class="kl">已完成</span><strong class="kv kpi-green">{{ qualityTasks.filter(t=>t.status==='COMPLETED').length }}</strong></div>
        <div class="dash-kpi"><span class="kl">质检总数</span><strong class="kv">{{ qualityTasks.length }}</strong></div>
        <div class="dash-kpi"><span class="kl">不良记录</span><strong class="kv kpi-red">{{ defectCount }}</strong></div>
        <div class="dash-kpi"><span class="kl">待处理安灯</span><strong class="kv kpi-red">{{ andonEvents.filter(e=>e.status==='OPEN').length }}</strong></div>
      </div>
      <div class="dash-grid-2col">
        <div class="dash-card"><h3>待办事项</h3><div class="card-stats"><div class="stat-item" v-for="a in qcAlerts" :key="a.id"><span class="stat-dot" :class="a.level"></span><span class="stat-text">{{ a.text }}</span></div></div><div v-if="!qcAlerts.length" class="empty-hint">暂无待办</div></div>
        <div class="dash-card"><h3>快捷操作</h3><div class="quick-grid-2"><RouterLink v-for="q in qcQuick" :key="q.path" :to="q.path" class="quick-card-2"><span class="qc-icon" :style="{background:q.color}">{{ q.icon }}</span><span class="qc-label">{{ q.label }}</span></RouterLink></div></div>
      </div>
    </template>

    <!-- ====== EQUIPMENT_MAINTAINER ====== -->
    <template v-else-if="role==='EQUIPMENT_MAINTAINER'">
      <div class="hero-banner" style="background:linear-gradient(135deg,#5f3a1e 0%,#d97706 50%,#b45309 100%)">
        <div class="hero-content"><h2>设备工作台</h2><p>点检 · 保养 · 报修 · 维修 — 设备全生命周期保障</p></div>
      </div>
      <div class="welcome-row"><h2>{{ app.user.name }}，欢迎回来</h2><span class="dash-clock">{{ now }}</span></div>
      <div class="dash-kpi-row">
        <div class="dash-kpi"><span class="kl">设备总数</span><strong class="kv kpi-blue">{{ devices.length }}</strong></div>
        <div class="dash-kpi"><span class="kl">运行中</span><strong class="kv kpi-green">{{ devices.filter(d=>d.status!=="FAULT"&&d.status!=="INACTIVE").length }}</strong></div>
        <div class="dash-kpi"><span class="kl">故障设备</span><strong class="kv kpi-red">{{ devices.filter(d=>d.status==='FAULT').length }}</strong></div>
        <div class="dash-kpi"><span class="kl">待保养</span><strong class="kv">{{ maintenanceDue }}</strong></div>
        <div class="dash-kpi"><span class="kl">待点检</span><strong class="kv kpi-blue">{{ inspections.length }}</strong></div>
        <div class="dash-kpi"><span class="kl">待处理安灯</span><strong class="kv kpi-red">{{ andonEvents.filter(e=>e.status==='OPEN').length }}</strong></div>
      </div>
      <div class="dash-grid-2col">
        <div class="dash-card"><h3>待办事项</h3><div class="card-stats"><div class="stat-item" v-for="a in eqAlerts" :key="a.id"><span class="stat-dot" :class="a.level"></span><span class="stat-text">{{ a.text }}</span></div></div><div v-if="!eqAlerts.length" class="empty-hint">暂无待办</div></div>
        <div class="dash-card"><h3>快捷操作</h3><div class="quick-grid-2"><RouterLink v-for="q in eqQuick" :key="q.path" :to="q.path" class="quick-card-2"><span class="qc-icon" :style="{background:q.color}">{{ q.icon }}</span><span class="qc-label">{{ q.label }}</span></RouterLink></div></div>
      </div>
    </template>

    <!-- ====== LINE_OPERATOR ====== -->
    <template v-else-if="role==='LINE_OPERATOR'">
      <div class="hero-banner" style="background:linear-gradient(135deg,#1a3a4a 0%,#0891b2 50%,#0e7490 100%)">
        <div class="hero-content"><h2>生产看板</h2><p>扫码报工 · 物料绑定 · 安灯呼叫 — 现场高效作业</p></div>
      </div>
      <div class="welcome-row"><h2>{{ app.user.name }}，欢迎回来</h2><span class="dash-clock">{{ now }}</span></div>
      <div class="dash-kpi-row">
        <div class="dash-kpi"><span class="kl">我的任务</span><strong class="kv kpi-blue">{{ myTasks.length }}</strong></div>
        <div class="dash-kpi"><span class="kl">进行中</span><strong class="kv">{{ myTasks.filter(t=>t.status==='RUNNING').length }}</strong></div>
        <div class="dash-kpi"><span class="kl">待报工</span><strong class="kv kpi-blue">{{ myTasks.filter(t=>t.status!=='COMPLETED').length }}</strong></div>
        <div class="dash-kpi"><span class="kl">已完成</span><strong class="kv kpi-green">{{ myTasks.filter(t=>t.status==='COMPLETED').length }}</strong></div>
        <div class="dash-kpi"><span class="kl">今日报工</span><strong class="kv">{{ todayReports }}</strong></div>
        <div class="dash-kpi"><span class="kl">安灯</span><strong class="kv kpi-red">{{ andonEvents.filter(e=>e.status==='OPEN').length }}</strong></div>
      </div>
      <div class="dash-grid-2col">
        <div class="dash-card"><h3>任务概览</h3>
          <div class="task-mini-list" v-if="myTasks.length">
            <div class="task-mini-row" v-for="t in myTasks.slice(0,5)" :key="t.dispatchId">
              <span class="tmn">{{ t.dispatchNo }}</span><span class="tms">{{ t.stepName }}</span><span class="tmt" :class="tagC(t.status)">{{ statusLabel(t.status) }}</span>
              <button class="btn-mini" @click="$router.push('/app/my-work')">报工</button>
            </div>
          </div>
          <div v-else class="empty-hint">暂无分配任务，请联系主管</div>
        </div>
        <div class="dash-card"><h3>快捷操作</h3><div class="quick-grid-2"><RouterLink v-for="q in opQuick" :key="q.path" :to="q.path" class="quick-card-2"><span class="qc-icon" :style="{background:q.color}">{{ q.icon }}</span><span class="qc-label">{{ q.label }}</span></RouterLink></div></div>
      </div>
    </template>

    <!-- ====== TESTER / 超级管理员 ====== -->
    <template v-else-if="role==='TESTER'">
      <div class="hero-banner" :style="{ backgroundImage: `url(${testerBanners[testerBannerIdx].image})` }">
        <div class="hero-overlay"></div>
        <div class="hero-content">
          <h2>{{ testerBanners[testerBannerIdx].title }}</h2>
          <p>{{ testerBanners[testerBannerIdx].desc }}</p>
          <div class="hero-dots">
            <span
              v-for="(s, i) in testerBanners"
              :key="i"
              class="hero-dot"
              :class="{ on: testerBannerIdx === i }"
              @click="testerBannerIdx = i"
            ></span>
          </div>
        </div>
      </div>
      <div class="welcome-row"><h2>{{ app.user.name }}，欢迎回来</h2><span class="dash-badge">SUPER ADMIN</span><span class="dash-clock">{{ now }}</span></div>
      <div class="dash-kpi-row">
        <div class="dash-kpi"><span class="kl">工单</span><strong class="kv kpi-blue">{{ workOrders.length }}</strong></div>
        <div class="dash-kpi"><span class="kl">设备</span><strong class="kv">{{ devices.length }}</strong></div>
        <div class="dash-kpi"><span class="kl">批次</span><strong class="kv kpi-green">{{ batches.length }}</strong></div>
        <div class="dash-kpi"><span class="kl">质检</span><strong class="kv">{{ qualityTasks.length }}</strong></div>
        <div class="dash-kpi"><span class="kl">欠料</span><strong class="kv kpi-red">{{ shortages.length }}</strong></div>
        <div class="dash-kpi"><span class="kl">安灯</span><strong class="kv kpi-red">{{ andonEvents.filter(e=>e.status==='OPEN').length }}</strong></div>
      </div>
      <div class="dash-grid-2col">
        <div class="dash-card"><h3>待办事项</h3><div class="card-stats"><div class="stat-item" v-for="a in tsAlerts" :key="a.id"><span class="stat-dot" :class="a.level"></span><span class="stat-text">{{ a.text }}</span></div></div><div v-if="!tsAlerts.length" class="empty-hint">暂无待办</div></div>
        <div class="dash-card"><h3>模块快捷入口</h3><div class="quick-grid-2"><RouterLink v-for="q in tsQuick" :key="q.path" :to="q.path" class="quick-card-2"><span class="qc-icon" :style="{background:q.color}">{{ q.icon }}</span><span class="qc-label">{{ q.label }}</span></RouterLink></div></div>
      </div>
    </template>

    <template v-else>
      <div class="empty-hint">未识别角色，请联系管理员</div>
    </template>
  </div>
</template>

<script setup>
import { getAuthUser } from "@/utils/auth-context";
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { request } from "@/api/request";
import { useAppStore } from "@/stores/app";
import { resolveRoleCodeFromAuth } from "@/constants/role-access";
import WorkorderProgressStrip from "@/yunshu-ui/pro/schedule/WorkorderProgressStrip.vue";

const app = useAppStore();
const now = ref("");
const bannerIdx = ref(0);
const testerBannerIdx = ref(0);
let clock;
let bannerTimer;
const banners = [
  { image: "https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=1200&q=80", title: "精密制造 · 智联未来", desc: "云枢智造 MES — 为每一台风扇建立数字档案，从订单到交付全链路数字化" },
  { image: "https://images.unsplash.com/photo-1677442136019-21780ecad995?w=1200&q=80", title: "Agent 功能已上线", desc: "AI 驱动的制造决策辅助，实时风险预警与知识库问答现已面向全部角色开放" },
];
const testerBanners = [
  { image: "https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=1200&q=80", title: "超级管理员总览", desc: "全系统页面 · 全模块操作 · 统一新控制台" },
  { image: "https://images.unsplash.com/photo-1677442136019-21780ecad995?w=1200&q=80", title: "Agent 功能已上线", desc: "AI 驱动的制造决策辅助，实时风险预警与知识库问答现已面向全部角色开放" },
];

const workOrders = ref([]), andonEvents = ref([]), shortages = ref([]), qualityTasks = ref([]);
const devices = ref([]), batches = ref([]), myTasks = ref([]), inspections = ref([]);
const users = ref([]), defects = ref([]), reports = ref([]);

onMounted(async () => {
  clock = setInterval(() => {
    now.value = new Date().toLocaleString("zh-CN", { hour: "2-digit", minute: "2-digit", second: "2-digit", year: "numeric", month: "2-digit", day: "2-digit", weekday: "short" });
  }, 1000);
  bannerTimer = setInterval(() => {
    bannerIdx.value = (bannerIdx.value + 1) % banners.length;
    testerBannerIdx.value = (testerBannerIdx.value + 1) % testerBanners.length;
  }, 5000);

  const fetchers = [
    (async () => { try { workOrders.value = (await request.get("/planning/work-orders"))?.data || []; } catch { workOrders.value = []; } })(),
    (async () => { try { andonEvents.value = (await request.get("/andon/events"))?.data || []; } catch { andonEvents.value = []; } })(),
    (async () => { try { shortages.value = (await request.get("/planning/material-shortages"))?.data || []; } catch { shortages.value = []; } })(),
    (async () => { try { const r = await request.get("/mes/qc/ipqc/list", { params: { pageNum: 1, pageSize: 200 } }); qualityTasks.value = (r?.rows || []).map(row => ({ status: row.status === "FINISHED" ? "COMPLETED" : row.status === "PREPARE" ? "PENDING" : "IN_PROGRESS" })); } catch { qualityTasks.value = []; } })(),
    (async () => { try { const r = await request.get("/mes/dv/machinery/list", { params: { pageNum: 1, pageSize: 200 } }); devices.value = (r?.rows || []).map(row => ({ status: row.status === "REPAIR" || row.status === "STOP" ? "FAULT" : "NORMAL" })); } catch { devices.value = []; } })(),
    (async () => { try { batches.value = (await request.get("/inventory/batches"))?.data || []; } catch { batches.value = []; } })(),
    (async () => { try { const r = await request.get("/mes/dv/checkrecord/list", { params: { pageNum: 1, pageSize: 50 } }); inspections.value = r?.rows || []; } catch { inspections.value = []; } })(),
    (async () => { try { users.value = (await request.get("/system/users"))?.data || []; } catch { users.value = []; } })(),
    (async () => { try { const r = await request.get("/mes/qc/defectrecord/list", { params: { pageNum: 1, pageSize: 200 } }); defects.value = r?.rows || []; } catch { defects.value = []; } })(),
    (async () => { try { reports.value = (await request.get("/production/reports"))?.data || []; } catch { reports.value = []; } })(),
  ];

  try {
    const u = getAuthUser() || {};
    fetchers.push((async () => {
      try {
        const r = await request.get("/planning/dispatch-tasks");
        myTasks.value = (r?.data || []).filter(t => String(t.operatorId) === String(u.userId));
      } catch { myTasks.value = []; }
    })());
  } catch { myTasks.value = []; }

  await Promise.allSettled(fetchers);
});

onBeforeUnmount(() => {
  clearInterval(clock);
  clearInterval(bannerTimer);
});

const role = computed(() => {
  const authUser = app.authUser || getAuthUser() || {};
  const code = resolveRoleCodeFromAuth(authUser);
  if (code) return code;
  const rn = app.user.role || "";
  if (rn.includes("测试") || rn.includes("TESTER")) return "TESTER";
  if (rn.includes("仓库") || rn.includes("WAREHOUSE")) return "WAREHOUSE_CLERK";
  if (rn.includes("质检") || rn.includes("QUALITY")) return "QUALITY_INSPECTOR";
  if (rn.includes("设备") || rn.includes("EQUIPMENT")) return "EQUIPMENT_MAINTAINER";
  if (rn.includes("操作") || rn.includes("LINE")) return "LINE_OPERATOR";
  if (rn.includes("生产主管") || rn.includes("PROD")) return "PROD_SUPERVISOR";
  if (rn.includes("管理") || rn.includes("MANAGER")) return "MANAGER";
  return "";
});

const defectCount = computed(() => defects.value.length);
const todayReports = computed(() => reports.value.length);
const maintenanceDue = computed(() => 0);

const statusLabel = (s) => s === "CREATED" ? "新建" : s === "RUNNING" ? "进行中" : s === "COMPLETED" ? "已完成" : s;
const tagC = (s) => s === "CREATED" ? "t-blue" : s === "RUNNING" ? "t-amber" : s === "COMPLETED" ? "t-green" : "";

// ---- Quick Links & Alerts per role ----
const mgrQuick = [
  { path: "/app/system/users", label: "用户管理", icon: "U", color: "#2563eb" },
  { path: "/app/system/roles", label: "角色管理", icon: "R", color: "#7c3aed" },
  { path: "/app/system/departments", label: "部门管理", icon: "D", color: "#059669" },
  { path: "/app/system/dict", label: "字典管理", icon: "P", color: "#d97706" },
  { path: "/app/system/operlog", label: "操作日志", icon: "L", color: "#dc2626" },
  { path: "/app/andon", label: "安灯中心", icon: "A", color: "#f59e0b" },
];

const supQuick = [
  { path: "/app/planning/orders", label: "订单中心", icon: "O", color: "#0052d9" },
  { path: "/app/planning/work-orders", label: "工单中心", icon: "W", color: "#0abf5b" },
  { path: "/app/planning/scheduling", label: "排产派工", icon: "S", color: "#ff9d00" },
  { path: "/app/factory/workshops", label: "车间管理", icon: "F", color: "#0891b2" },
  { path: "/app/line-monitor", label: "产线监控", icon: "L", color: "#2563eb" },
  { path: "/app/andon", label: "安灯中心", icon: "A", color: "#f59e0b" },
];

const whQuick = [
  { path: "/app/inventory/wmstock", label: "库存现有量", icon: "S", color: "#2563eb" },
  { path: "/app/inventory/inbound", label: "入库作业", icon: "I", color: "#059669" },
  { path: "/app/inventory/outbound", label: "出库作业", icon: "O", color: "#d97706" },
  { path: "/app/inventory/wmstock?mode=batch", label: "批次档案", icon: "B", color: "#7c3aed" },
  { path: "/app/inventory/barcode", label: "条码管理", icon: "C", color: "#dc2626" },
  { path: "/app/andon", label: "安灯中心", icon: "A", color: "#f59e0b" },
];

const qcQuick = [
  { path: "/app/quality/tasks", label: "质检管理", icon: "Q", color: "#7c3aed" },
  { path: "/app/andon", label: "安灯中心", icon: "A", color: "#f59e0b" },
];

const eqQuick = [
  { path: "/app/equipment/workbench", label: "设备工作台", icon: "W", color: "#d97706" },
  { path: "/app/equipment/machinery", label: "设备台账", icon: "D", color: "#2563eb" },
  { path: "/app/equipment/analytics", label: "设备分析", icon: "A", color: "#059669" },
  { path: "/app/andon", label: "安灯中心", icon: "N", color: "#f59e0b" },
];

const opQuick = [
  { path: "/app/my-work", label: "我的工位", icon: "W", color: "#2563eb" },
  { path: "/app/andon/launch", label: "发起安灯", icon: "A", color: "#f59e0b" },
];

const tsQuick = [
  { path: "/app/system/users", label: "用户管理", icon: "U", color: "#2563eb" },
  { path: "/app/system/roles", label: "角色管理", icon: "R", color: "#7c3aed" },
  { path: "/app/system/menus", label: "菜单管理", icon: "P", color: "#d97706" },
  { path: "/app/planning/orders", label: "订单中心", icon: "O", color: "#0052d9" },
  { path: "/app/planning/work-orders", label: "工单中心", icon: "W", color: "#0abf5b" },
  { path: "/app/planning/scheduling", label: "排产派工", icon: "S", color: "#ff9d00" },
  { path: "/app/quality/tasks", label: "质检管理", icon: "Q", color: "#7c3aed" },
  { path: "/app/inventory/wmstock", label: "库存现有量", icon: "B", color: "#d97706" },
  { path: "/app/equipment/workbench", label: "设备工作台", icon: "D", color: "#0891b2" },
  { path: "/app/andon", label: "安灯中心", icon: "A", color: "#f59e0b" },
  { path: "/app/barcode", label: "条码应用", icon: "B", color: "#64748b" },
  { path: "/app/production", label: "生产执行", icon: "P", color: "#0891b2" },
  { path: "/app/analytics/integration/systems", label: "接口集成", icon: "I", color: "#7c3aed" },
  { path: "/agent", label: "Agent中心", icon: "G", color: "#4f46e5" },
];

const mgrAlerts = computed(() => {
  const a = [];
  if (andonEvents.value.filter(e => e.status === "OPEN").length) a.push({ id: 1, text: `${andonEvents.value.filter(e => e.status === "OPEN").length} 条安灯事件待处理`, level: "warn" });
  if (shortages.value.length) a.push({ id: 2, text: `${shortages.value.length} 项物料欠料需关注`, level: "warn" });
  return a;
});
const supAlerts = computed(() => {
  const a = [];
  if (shortages.value.length) a.push({ id: 1, text: `${shortages.value.length} 项物料欠料需齐套分析`, level: "warn" });
  const running = workOrders.value.filter(w => w.status === "RUNNING");
  if (running.length) a.push({ id: 2, text: `${running.length} 张工单正在生产中`, level: "info" });
  return a;
});
const whAlerts = computed(() => {
  const a = [];
  if (shortages.value.length) a.push({ id: 1, text: `${shortages.value.length} 项物料库存不足需补货`, level: "warn" });
  const locked = batches.value.filter(b => (Number(b.lockedQty) || 0) > 0);
  if (locked.length) a.push({ id: 2, text: `${locked.length} 个批次已锁定，待确认发料`, level: "info" });
  return a;
});
const qcAlerts = computed(() => {
  const a = [];
  const pending = qualityTasks.value.filter(t => t.status === "PENDING");
  if (pending.length) a.push({ id: 1, text: `${pending.length} 项任务待检验`, level: "info" });
  if (defects.value.length) a.push({ id: 2, text: `${defects.value.length} 条不良记录待处理`, level: "warn" });
  return a;
});
const eqAlerts = computed(() => {
  const a = [];
  const fault = devices.value.filter(d => d.status === "FAULT");
  if (fault.length) a.push({ id: 1, text: `${fault.length} 台设备故障需维修`, level: "warn" });
  if (inspections.value.length) a.push({ id: 2, text: `${inspections.value.length} 条点检记录已完成`, level: "info" });
  return a;
});
const tsAlerts = computed(() => {
  const a = [];
  if (shortages.value.length) a.push({ id: 1, text: `${shortages.value.length} 项欠料`, level: "warn" });
  return a;
});
</script>

<style scoped>
.dash-root { display: grid; gap: 14px; }

/* Hero Banner */
.hero-banner { position: relative; overflow: hidden; height: 160px; background-size: cover; background-position: center; }
.hero-overlay { position: absolute; inset: 0; background: linear-gradient(90deg, rgba(0,0,0,.7) 0%, rgba(0,0,0,.35) 60%, rgba(0,0,0,.1) 100%); }
.hero-content { position: absolute; bottom: 24px; left: 24px; z-index: 1; }
.hero-content h2 { margin: 0; color: #fff; font-size: 22px; font-weight: 700; }
.hero-content p { margin: 4px 0 10px; color: rgba(255,255,255,.7); font-size: 13px; max-width: 520px; }
.hero-dots { display: flex; gap: 8px; }
.hero-dot { width: 24px; height: 3px; background: rgba(255,255,255,.3); cursor: pointer; transition: background .2s; }
.hero-dot.on { background: rgba(255,255,255,.9); }

/* Welcome */
.welcome-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 8px 0; flex-wrap: wrap; }
.welcome-row h2 { margin: 0; font-size: 20px; font-weight: 700; }
.dash-badge { font-size: 11px; font-weight: 600; letter-spacing: 0.06em; color: #fff; background: #111; border: 1px solid #333; padding: 3px 8px; }
.dash-clock { font-size: 13px; color: #999; font-family: "JetBrains Mono", monospace; margin-left: auto; }

/* KPI Row */
.dash-kpi-row { display: grid; grid-template-columns: repeat(6, 1fr); gap: 10px; padding: 14px; background: #fff; border: 1px solid #e0e0e0; }
.dash-kpi { display: flex; flex-direction: column; gap: 2px; padding: 8px 12px; border-right: 1px solid #eee; }
.dash-kpi:last-child { border-right: none; }
.kl { font-size: 11px; color: #999; }
.kv { font-size: 26px; font-weight: 700; }
.kpi-blue { color: #2563eb; }
.kpi-green { color: #059669; }
.kpi-red { color: #dc2626; }

/* 2-Column Grid */
.dash-grid-2col { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.dash-card { background: #fff; border: 1px solid #e0e0e0; padding: 18px; }
.dash-card h3 { margin: 0 0 14px; font-size: 15px; font-weight: 600; }
.card-stats { display: flex; flex-direction: column; gap: 6px; }
.stat-item { display: flex; align-items: center; gap: 10px; padding: 8px 10px; font-size: 13px; border-bottom: 1px solid #f0f0f0; }
.stat-item:last-child { border-bottom: none; }
.stat-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.stat-dot.warn { background: #f59e0b; }
.stat-dot.info { background: #3b82f6; }
.stat-text { flex: 1; }
.empty-hint { padding: 20px 0; text-align: center; color: #ccc; font-size: 13px; }

/* Quick Grid */
.quick-grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.quick-card-2 { display: flex; align-items: center; gap: 10px; padding: 16px; background: #f8f8f8; border: 1px solid #eee; font-size: 14px; font-weight: 500; text-decoration: none; color: #1a1a1a; transition: background .12s; }
.quick-card-2:hover { background: #eee; }
.qc-icon { width: 34px; height: 34px; border-radius: 0; color: #fff; display: grid; place-items: center; font-size: 15px; font-weight: 700; flex-shrink: 0; }
.qc-label { flex: 1; }

/* Task Mini List */
.task-mini-list { display: flex; flex-direction: column; }
.task-mini-row { display: flex; align-items: center; gap: 10px; padding: 8px 10px; border-bottom: 1px solid #f0f0f0; font-size: 13px; }
.task-mini-row:last-child { border-bottom: none; }
.tmn { font-family: "JetBrains Mono", monospace; font-size: 11px; color: #999; width: 80px; flex-shrink: 0; }
.tms { flex: 1; font-weight: 500; }
.tmt { font-size: 11px; font-weight: 600; padding: 2px 6px; }
.t-blue { color: #2563eb; }
.t-amber { color: #d97706; }
.t-green { color: #059669; }
.btn-mini { height: 26px; padding: 0 10px; border: 1px solid #1a1a1a; background: #fff; font-size: 11px; cursor: pointer; text-decoration: none; color: #1a1a1a; }
.btn-mini:hover { background: #1a1a1a; color: #fff; }
</style>
