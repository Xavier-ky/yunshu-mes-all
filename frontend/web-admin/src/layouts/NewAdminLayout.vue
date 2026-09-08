<template>
  <div class="tc-shell">
    <header class="tc-header">
      <div class="tc-header-left">
        <RouterLink to="/app" class="tc-logo">
          <img src="/images/yun-logo.svg" alt="" class="tc-logo-img" />
          <span class="tc-logo-text">云枢智造</span>
        </RouterLink>
        <span class="tc-header-divider"></span>
        <span class="tc-header-console">
          <LayoutGrid :size="17" :stroke-width="1.6" />
          <span class="tc-header-title">控制台</span>
        </span>
      </div>

      <div class="tc-header-search">
        <div class="tc-search-wrap">
          <Search :size="16" :stroke-width="1.6" class="tc-search-icon" />
          <input
            v-model="searchQuery"
            type="text"
            class="tc-search-input"
            placeholder="搜索功能、工单与物料"
            @keydown.enter="onSearch"
          />
        </div>
      </div>

      <div class="tc-header-right">
        <template v-if="isSuperAdmin">
          <RouterLink
            v-for="link in headerLinks"
            :key="link.path"
            :to="link.path"
            class="tc-header-link"
            active-class=""
            exact-active-class="router-link-active"
            >{{ link.label }}</RouterLink
          >
        </template>
        <template v-else>
          <RouterLink
            to="/app/planning/work-orders"
            class="tc-header-link"
            active-class=""
            exact-active-class="router-link-active"
            >工单中心</RouterLink
          >
          <RouterLink
            to="/app/andon"
            class="tc-header-link"
            active-class=""
            exact-active-class="router-link-active"
            >安灯中心</RouterLink
          >
        </template>
        <div class="tc-header-tools">
          <RouterLink
            v-for="tool in headerTools"
            :key="tool.path"
            :to="tool.path"
            class="tc-header-tool"
            :class="`tc-header-tool--${tool.tone}`"
            :title="tool.title"
          >
            <component :is="tool.icon" :size="16" :stroke-width="2" />
            <span>{{ tool.label }}</span>
          </RouterLink>
        </div>
        <span class="tc-header-divider"></span>
        <div class="tc-user-menu">
          <RouterLink
            to="/app/system/profile"
            class="tc-avatar-ring"
            title="个人中心"
          >
            <img
              v-if="showCustomAvatar"
              src="/images/my-logo.jpg"
              alt=""
              class="tc-user-avatar-img"
            />
            <span v-else class="tc-user-avatar">{{ userInitial }}</span>
          </RouterLink>
          <div class="tc-user-meta">
            <strong>{{ app.user.name }}</strong>
            <small>{{
              isSuperAdmin ? "超级管理员" : app.user.role || "主账号"
            }}</small>
          </div>
          <button class="tc-logout-btn" @click="logout" title="退出登录">
            <LogOut :size="15" :stroke-width="1.6" />
          </button>
        </div>
      </div>
    </header>

    <div class="tc-body">
      <!-- Single sidebar with accordion sub-nav -->
      <aside class="tc-sidebar" :class="{ collapsed: sidebarCollapsed }">
        <div class="tc-sidebar-head">
          <h2 v-show="!sidebarCollapsed">{{ sidebarTitle }}</h2>
        </div>

        <nav class="tc-sidebar-nav">
          <div v-for="mod in navModules" :key="mod.id" class="tc-nav-group">
            <!-- Single leaf: direct link -->
            <RouterLink
              v-if="isLeafGroup(mod)"
              :to="mod.items[0].path"
              class="tc-nav-item"
              :class="{ 'tc-nav-item-yunshu': mod.icon === 'yunshu' }"
              active-class=""
              exact-active-class="router-link-active"
              :title="mod.label"
            >
              <img
                v-if="mod.icon === 'yunshu'"
                src="/images/open-a-i.svg"
                alt=""
                class="tc-nav-icon tc-nav-icon-img"
              />
              <component
                v-else
                :is="icons[mod.icon]"
                :size="18"
                :stroke-width="1.6"
                class="tc-nav-icon"
              />
              <span v-show="!sidebarCollapsed" class="tc-nav-label">{{
                mod.label
              }}</span>
            </RouterLink>

            <!-- Group with children: click to expand sub-nav -->
            <template v-else>
              <button
                type="button"
                class="tc-nav-item tc-nav-toggle"
                :class="{
                  active: isGroupActive(mod),
                  expanded: isExpanded(mod.id),
                }"
                :title="mod.label"
                @click="toggleGroup(mod)"
              >
                <img
                  v-if="mod.icon === 'yunshu'"
                  src="/images/open-a-i.svg"
                  alt=""
                  class="tc-nav-icon tc-nav-icon-img"
                />
                <component
                  v-else
                  :is="icons[mod.icon]"
                  :size="18"
                  :stroke-width="1.6"
                  class="tc-nav-icon"
                />
                <span v-show="!sidebarCollapsed" class="tc-nav-label">{{
                  mod.label
                }}</span>
                <ChevronDown
                  v-show="!sidebarCollapsed"
                  :size="15"
                  :stroke-width="1.6"
                  class="tc-nav-chevron"
                  :class="{ open: isExpanded(mod.id) }"
                />
              </button>

              <div
                v-show="isExpanded(mod.id) && !sidebarCollapsed"
                class="tc-nav-sub"
              >
                <div
                  v-for="item in mod.items"
                  :key="item.path"
                  class="tc-nav-sub-group"
                >
                  <RouterLink
                    v-if="!item.children"
                    :to="item.path"
                    class="tc-nav-sub-item"
                    active-class=""
                    exact-active-class="router-link-active"
                    >{{ item.label }}</RouterLink
                  >
                  <template v-else>
                    <button
                      type="button"
                      class="tc-nav-sub-item tc-nav-sub-toggle"
                      :class="{
                        expanded: isSubExpanded(mod.id, item.path),
                        active: isSubGroupActive(item),
                      }"
                      @click="toggleSubGroup(mod.id, item.path)"
                    >
                      <span class="tc-nav-sub-label">{{ item.label }}</span>
                      <ChevronDown
                        :size="13"
                        :stroke-width="1.6"
                        class="tc-nav-sub-chevron"
                        :class="{ open: isSubExpanded(mod.id, item.path) }"
                      />
                    </button>
                    <div
                      v-show="isSubExpanded(mod.id, item.path)"
                      class="tc-nav-sub-children"
                    >
                      <RouterLink
                        v-for="child in item.children"
                        :key="child.path"
                        :to="child.path"
                        class="tc-nav-sub-item tc-nav-sub-child"
                        active-class=""
                        exact-active-class="router-link-active"
                        >{{ child.label }}</RouterLink
                      >
                    </div>
                  </template>
                </div>
              </div>
            </template>
          </div>
        </nav>

        <div class="tc-sidebar-foot">
          <button
            class="tc-collapse-btn"
            @click="sidebarCollapsed = !sidebarCollapsed"
          >
            <ChevronLeft
              v-if="!sidebarCollapsed"
              :size="14"
              :stroke-width="1.6"
            />
            <ChevronRight v-else :size="14" :stroke-width="1.6" />
            <span v-show="!sidebarCollapsed">收起</span>
          </button>
        </div>
      </aside>

      <!-- Flyout sub-panel when sidebar collapsed -->
      <aside
        v-if="sidebarCollapsed && flyoutModule"
        class="tc-flyout"
        @mouseleave="flyoutModule = null"
      >
        <div class="tc-flyout-head">{{ flyoutModule.label }}</div>
        <div
          v-for="item in flyoutModule.items"
          :key="item.path"
          class="tc-flyout-group"
        >
          <RouterLink
            v-if="!item.children"
            :to="item.path"
            class="tc-flyout-item"
            active-class=""
            exact-active-class="router-link-active"
            @click="flyoutModule = null"
            >{{ item.label }}</RouterLink
          >
          <template v-else>
            <button
              type="button"
              class="tc-flyout-item tc-flyout-toggle"
              :class="{
                expanded: isSubExpanded(flyoutModule.id, item.path),
                active: isSubGroupActive(item),
              }"
              @click="toggleSubGroup(flyoutModule.id, item.path)"
            >
              <span>{{ item.label }}</span>
              <ChevronDown
                :size="13"
                :stroke-width="1.6"
                class="tc-nav-sub-chevron"
                :class="{ open: isSubExpanded(flyoutModule.id, item.path) }"
              />
            </button>
            <div
              v-show="isSubExpanded(flyoutModule.id, item.path)"
              class="tc-flyout-children"
            >
              <RouterLink
                v-for="child in item.children"
                :key="child.path"
                :to="child.path"
                class="tc-flyout-item tc-flyout-child"
                active-class=""
                exact-active-class="router-link-active"
                @click="flyoutModule = null"
                >{{ child.label }}</RouterLink
              >
            </div>
          </template>
        </div>
      </aside>

      <main class="tc-main">
        <VisitedTabsBar v-if="route.path !== '/app/yunshu-ai' && !route.meta.hideTabs" />
        <div
          v-if="
            !route.meta.fullBleed &&
            !route.meta.pageHero &&
            !route.meta.yunshuUi
          "
          class="tc-page-head"
        >
          <h1 class="tc-page-title">{{ route.meta.title || "工作台" }}</h1>
        </div>
        <section
          class="tc-content"
          :class="{
            'tc-content--bleed': route.meta.fullBleed,
            'tc-content--hero': route.meta.pageHero,
            'tc-content--yunshu-ui': route.meta.yunshuUi,
          }"
        >
          <RouterView v-slot="{ Component }">
            <YunshuUiHost v-if="route.meta.yunshuUi && Component">
              <component :is="Component" />
            </YunshuUiHost>
            <component v-else :is="Component" />
          </RouterView>
        </section>
      </main>
    </div>
    <SpiritCompanion />
  </div>
</template>

<script setup>
import { getAuthUser } from "@/utils/auth-context";
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAppStore } from "@/stores/app";
import { useTagsViewStore } from "@/stores/tags-view";
import {
  superAdminHeaderLinks,
  isSuperAdminUser,
} from "@/constants/super-admin";
import { superAdminNav } from "@/constants/nav-modules";
import {
  getNavForRole,
  canRoleAccessPath,
  resolveRoleCodeFromAuth,
} from "@/constants/role-access";
import VisitedTabsBar from "@/components/layout/VisitedTabsBar.vue";
import YunshuUiHost from "@/components/yunshu-ui/YunshuUiHost.vue";
import SpiritCompanion from "@/components/SpiritCompanion.vue";
import {
  LayoutDashboard,
  MonitorDown,
  BarChart3,
  Search,
  Bell,
  Settings,
  Users,
  ShieldCheck,
  ScrollText,
  ClipboardList,
  Factory,
  Package,
  LogOut,
  ChevronLeft,
  ChevronRight,
  ChevronDown,
  Boxes,
  Shield,
  Wrench,
  Database,
  AlertTriangle,
  UserCog,
  HardHat,
  LayoutGrid,
  CalendarDays,
  FileSpreadsheet,
  Printer,
} from "lucide-vue-next";

const headerTools = [
  {
    path: "/app/factory/calendar",
    label: "日历",
    title: "排班日历",
    icon: CalendarDays,
    tone: "calendar",
  },
  {
    path: "/app/analytics/report-designer",
    label: "报表",
    title: "报表设计",
    icon: FileSpreadsheet,
    tone: "report",
  },
  {
    path: "/app/analytics/print",
    label: "打印",
    title: "打印中心",
    icon: Printer,
    tone: "print",
  },
];

const route = useRoute();
const router = useRouter();
const app = useAppStore();
const tagsView = useTagsViewStore();
const sidebarCollapsed = ref(false);
const searchQuery = ref("");
const expandedGroups = ref(new Set());
const expandedSubGroups = ref(new Set());
const flyoutModule = ref(null);

const icons = {
  dashboard: LayoutDashboard,
  monitor: MonitorDown,
  workorders: ClipboardList,
  factory: Factory,
  package: Package,
  reports: BarChart3,
  trace: Search,
  andon: Bell,
  settings: Settings,
  users: Users,
  roles: ShieldCheck,
  logs: ScrollText,
  warehouse: Boxes,
  quality: Shield,
  equipment: Wrench,
  data: Database,
  event: AlertTriangle,
  system: UserCog,
  operator: HardHat,
};

const authSnapshot = computed(
  () =>
    app.authUser || getAuthUser() || {},
);

const isSuperAdmin = computed(() =>
  isSuperAdminUser({ ...authSnapshot.value, role: app.user.role }),
);

const navModules = computed(() => {
  if (isSuperAdmin.value) return superAdminNav;
  const roleCode = resolveRoleCodeFromAuth(authSnapshot.value);
  return getNavForRole(roleCode);
});

const headerLinks = computed(() =>
  isSuperAdmin.value ? superAdminHeaderLinks : [],
);

const sidebarTitle = computed(() => {
  if (isSuperAdmin.value) return "超级管理员";
  const role = app.user.role || "";
  if (role.includes("管理") || role === "MANAGER") return "系统中心";
  return "MES 控制台";
});

const userInitial = computed(() =>
  (app.user.name || "U").charAt(0).toUpperCase(),
);

const showCustomAvatar = computed(() => isSuperAdmin.value);

function groupPaths(mod) {
  const paths = [];
  for (const item of mod.items) {
    paths.push(item.path);
    if (item.children) paths.push(...item.children.map((c) => c.path));
  }
  return paths;
}

function isPathInModule(path, mod) {
  return mod.items.some((item) => {
    if (path === item.path) return true;
    if (item.path === "/app") return false;
    if (item.path.startsWith("/app/") && path.startsWith(item.path + "/"))
      return true;
    return (item.children || []).some(
      (c) => path === c.path || path.startsWith(c.path + "/"),
    );
  });
}

function isLeafGroup(mod) {
  if (mod.items.length !== 1) return false;
  return !mod.items[0].children;
}

function subGroupKey(modId, itemPath) {
  return `${modId}::${itemPath}`;
}

function isSubExpanded(modId, itemPath) {
  return expandedSubGroups.value.has(subGroupKey(modId, itemPath));
}

function toggleSubGroup(modId, itemPath) {
  const key = subGroupKey(modId, itemPath);
  const next = new Set(expandedSubGroups.value);
  if (next.has(key)) {
    next.delete(key);
  } else {
    next.add(key);
  }
  expandedSubGroups.value = next;
}

function isSubGroupActive(item) {
  const path = route.path;
  if (path === item.path || path.startsWith(item.path + "/")) return true;
  return (item.children || []).some(
    (c) => path === c.path || path.startsWith(c.path + "/"),
  );
}

function isGroupActive(mod) {
  return isPathInModule(route.path, mod);
}

function isExpanded(id) {
  return expandedGroups.value.has(id);
}

function toggleGroup(mod) {
  if (sidebarCollapsed.value) {
    flyoutModule.value = flyoutModule.value?.id === mod.id ? null : mod;
    return;
  }
  const next = new Set(expandedGroups.value);
  if (next.has(mod.id)) {
    next.delete(mod.id);
  } else {
    next.add(mod.id);
  }
  expandedGroups.value = next;
}

function syncExpandedFromRoute() {
  const path = route.path;
  const nextGroups = new Set(expandedGroups.value);
  const nextSubs = new Set(expandedSubGroups.value);
  for (const mod of navModules.value) {
    if (!isLeafGroup(mod) && isPathInModule(path, mod)) {
      nextGroups.add(mod.id);
      for (const item of mod.items) {
        if (item.children?.length && isSubGroupActive(item)) {
          nextSubs.add(subGroupKey(mod.id, item.path));
        }
      }
    }
  }
  expandedGroups.value = nextGroups;
  expandedSubGroups.value = nextSubs;
}

function canAccessRoute(targetRoute) {
  if (isSuperAdmin.value) return true;
  const roleCode = resolveRoleCodeFromAuth(authSnapshot.value);
  return canRoleAccessPath(roleCode, targetRoute.path);
}

if (!tagsView.restored) {
  tagsView.restore(router.resolve, canAccessRoute);
}

watch(() => route.path, syncExpandedFromRoute, { immediate: true });
watch(
  () => route.fullPath,
  () => tagsView.addRoute(route),
  { immediate: true },
);

function onSearch() {
  const q = searchQuery.value.trim().toLowerCase();
  if (!q) return;
  for (const mod of navModules.value) {
    for (const item of mod.items) {
      if (item.label.toLowerCase().includes(q)) {
        expandedGroups.value = new Set([...expandedGroups.value, mod.id]);
        router.push(item.path);
        return;
      }
      for (const child of item.children || []) {
        if (child.label.toLowerCase().includes(q)) {
          expandedGroups.value = new Set([...expandedGroups.value, mod.id]);
          expandedSubGroups.value = new Set([
            ...expandedSubGroups.value,
            subGroupKey(mod.id, item.path),
          ]);
          router.push(child.path);
          return;
        }
      }
    }
  }
}

function logout() {
  tagsView.clearSession();
  app.logout();
  router.push("/login");
}
</script>

<style scoped>
/* ===== Tencent Cloud Console Tokens ===== */
.tc-shell {
  --tc-header-h: 58px;
  --tc-sidebar-w: 208px;
  --tc-sidebar-collapsed-w: 52px;
  --tc-bg-header: #000000;
  --tc-bg-sidebar: #1e1e1e;
  --tc-bg-page: #f2f2f2;
  --mes-watercolor-bg:
    radial-gradient(circle at 100% 0%,
      transparent 0 110px,
      rgba(120, 170, 230, 0.018) 160px,
      rgba(120, 170, 230, 0.055) 215px,
      rgba(120, 170, 230, 0.09) 265px,
      rgba(120, 170, 230, 0.055) 315px,
      rgba(120, 170, 230, 0.018) 370px,
      transparent 420px),
    radial-gradient(ellipse 46% 38% at 90% 2%, rgba(244, 180, 128, 0.075), transparent 62%),
    radial-gradient(ellipse 40% 34% at 62% 14%, rgba(236, 150, 158, 0.065), transparent 60%),
    radial-gradient(ellipse 38% 32% at 22% 8%, rgba(240, 206, 128, 0.055), transparent 62%),
    radial-gradient(ellipse 48% 40% at 4% 42%, rgba(140, 186, 228, 0.095), transparent 64%),
    radial-gradient(ellipse 44% 36% at 48% 52%, rgba(168, 168, 224, 0.075), transparent 62%),
    radial-gradient(ellipse 40% 34% at 88% 58%, rgba(234, 160, 150, 0.05), transparent 62%),
    radial-gradient(ellipse 46% 38% at 68% 96%, rgba(120, 188, 210, 0.085), transparent 62%),
    radial-gradient(ellipse 50% 42% at 12% 100%, rgba(108, 156, 224, 0.11), transparent 64%),
    radial-gradient(ellipse 42% 34% at 38% 78%, rgba(186, 208, 238, 0.09), transparent 62%),
    linear-gradient(160deg, #fbf7f2 0%, #f6f4f2 32%, #f1f3f6 64%, #ecf1f8 100%);
  --tc-bg-card: #ffffff;
  --tc-blue: #0052d9;
  --tc-text-on-dark: #ffffff;
  --tc-text-on-dark-2: rgba(255, 255, 255, 0.72);
  --tc-text-on-dark-3: rgba(255, 255, 255, 0.42);
  --tc-text-body: #000000;
  --tc-text-secondary: #666666;
  --tc-text-label: #888888;
  --tc-border-dark: rgba(255, 255, 255, 0.06);
  --tc-hover-dark: rgba(255, 255, 255, 0.06);
  --tc-active-dark: rgba(255, 255, 255, 0.1);
  --tc-font-nav:
    "钉钉进步体", "DingTalk JinBuTi", "Microsoft YaHei", sans-serif;
  --tc-font-ui:
    "PingFang SC", "Microsoft YaHei", -apple-system, BlinkMacSystemFont,
    sans-serif;

  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  font-family: var(--tc-font-ui);
  font-size: 13px;
  -webkit-font-smoothing: antialiased;
  color: var(--tc-text-body);
  background: var(--tc-bg-page);
}

/* ===== Header ===== */
.tc-header {
  display: flex;
  align-items: center;
  height: var(--tc-header-h);
  padding: 0 16px;
  background: var(--tc-bg-header);
  flex-shrink: 0;
  z-index: 200;
  gap: 20px;
  position: relative;
}

.tc-header::after {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 1px;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.06) 12%,
    rgba(160, 200, 255, 0.28) 50%,
    rgba(255, 255, 255, 0.06) 88%,
    transparent 100%
  );
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.04);
  pointer-events: none;
}

.tc-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.tc-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: var(--tc-text-on-dark);
}

.tc-logo-img {
  height: 36px;
  width: auto;
  display: block;
  filter: brightness(0) invert(1);
  opacity: 0.95;
}

.tc-logo-text {
  font-size: 25px;
  font-weight: 400;
  color: #ffffff;
  letter-spacing: 0.5px;
  font-family: var(--tc-font-nav);
  line-height: 1;
}

.tc-header-divider {
  width: 1px;
  height: 18px;
  background: rgba(255, 255, 255, 0.12);
}

.tc-header-console {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--tc-text-on-dark-2);
}

.tc-header-console svg {
  opacity: 0.8;
}

.tc-header-title {
  font-size: 15px;
  color: var(--tc-text-on-dark-2);
  font-weight: 400;
}

.tc-header-search {
  flex: 1;
  max-width: 560px;
  margin: 0 auto;
}

.tc-search-wrap {
  position: relative;
  display: flex;
  align-items: center;
  height: 36px;
  background: #1a1a1a;
  border: 1px solid #2a2a2a;
  border-radius: 2px;
  transition:
    border-color 0.15s,
    background 0.15s;
}

.tc-search-wrap:focus-within {
  background: #222222;
  border-color: #3a3a3a;
}

.tc-search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: rgba(255, 255, 255, 0.35);
  pointer-events: none;
}

.tc-search-input {
  width: 100%;
  height: 100%;
  padding: 0 12px 0 32px;
  background: transparent !important;
  border: none !important;
  border-radius: 0;
  color: rgba(255, 255, 255, 0.88) !important;
  font-size: 14px;
  font-family: var(--tc-font-ui);
  outline: none;
  box-shadow: none !important;
}

.tc-search-input::placeholder {
  color: rgba(255, 255, 255, 0.35);
}

.tc-search-input:focus {
  background: transparent !important;
  box-shadow: none !important;
}

.tc-header-right {
  display: flex;
  align-items: center;
  gap: 0;
  flex-shrink: 0;
}

.tc-header-link {
  padding: 0 10px;
  height: var(--tc-header-h);
  line-height: var(--tc-header-h);
  font-size: 14px;
  color: var(--tc-text-on-dark-2);
  text-decoration: none;
  transition: color 0.15s;
  white-space: nowrap;
}

.tc-header-link:hover {
  color: var(--tc-text-on-dark);
}

.tc-header-link.router-link-active {
  color: var(--tc-text-on-dark);
}

.tc-header-tools {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 10px 0 6px;
}

.tc-header-tool {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 30px;
  padding: 0 11px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.82);
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
  text-decoration: none;
  white-space: nowrap;
  transition:
    background 0.15s,
    border-color 0.15s,
    color 0.15s,
    transform 0.15s;
}

.tc-header-tool:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.24);
  transform: translateY(-1px);
}

.tc-header-tool.router-link-active {
  color: #fff;
  background: rgba(255, 255, 255, 0.14);
  border-color: rgba(255, 255, 255, 0.28);
}

.tc-header-tool--calendar {
  border-color: rgba(56, 189, 248, 0.35);
  background: rgba(56, 189, 248, 0.12);
  color: #bae6fd;
}

.tc-header-tool--calendar:hover,
.tc-header-tool--calendar.router-link-active {
  background: rgba(56, 189, 248, 0.22);
  border-color: rgba(56, 189, 248, 0.5);
  color: #e0f2fe;
}

.tc-header-tool--report {
  border-color: rgba(52, 211, 153, 0.35);
  background: rgba(52, 211, 153, 0.12);
  color: #a7f3d0;
}

.tc-header-tool--report:hover,
.tc-header-tool--report.router-link-active {
  background: rgba(52, 211, 153, 0.22);
  border-color: rgba(52, 211, 153, 0.5);
  color: #d1fae5;
}

.tc-header-tool--print {
  border-color: rgba(251, 191, 36, 0.35);
  background: rgba(251, 191, 36, 0.12);
  color: #fde68a;
}

.tc-header-tool--print:hover,
.tc-header-tool--print.router-link-active {
  background: rgba(251, 191, 36, 0.22);
  border-color: rgba(251, 191, 36, 0.5);
  color: #fef3c7;
}

.tc-user-menu {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-left: 8px;
}

.tc-avatar-ring {
  position: relative;
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  border-radius: 50%;
  display: grid;
  place-items: center;
}

.tc-avatar-ring::before {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: conic-gradient(
    from 0deg,
    rgba(255, 255, 255, 0.06),
    rgba(255, 255, 255, 0.55),
    rgba(255, 255, 255, 0.15),
    rgba(255, 255, 255, 0.45),
    rgba(255, 255, 255, 0.06)
  );
  animation: tcRingFlow 2.6s linear infinite;
}

.tc-avatar-ring::after {
  content: "";
  position: absolute;
  inset: 2px;
  border-radius: 50%;
  background: #000000;
  z-index: 0;
}

@keyframes tcRingFlow {
  to {
    transform: rotate(360deg);
  }
}

.tc-user-avatar {
  position: relative;
  z-index: 1;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #333333;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  display: grid;
  place-items: center;
}

.tc-user-avatar-img {
  position: relative;
  z-index: 1;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
}

.tc-user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.tc-user-meta strong {
  font-size: 14px;
  font-weight: 400;
  color: var(--tc-text-on-dark);
}

.tc-user-meta small {
  font-size: 13px;
  color: var(--tc-text-on-dark-3);
}

.tc-logout-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  color: var(--tc-text-on-dark-3);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.tc-logout-btn:hover {
  color: var(--tc-text-on-dark);
}

/* ===== Body ===== */
.tc-body {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  position: relative;
}

/* ===== Single Sidebar ===== */
.tc-sidebar {
  position: relative;
  width: var(--tc-sidebar-w);
  flex-shrink: 0;
  background: var(--tc-bg-sidebar);
  display: flex;
  flex-direction: column;
  transition: width 0.2s ease;
  overflow: hidden;
  z-index: 100;
  font-family: var(--tc-font-nav);
  border-right: 1px solid rgba(255, 255, 255, 0.07);
  box-shadow: inset -1px 0 0 rgba(255, 255, 255, 0.03);
}

.tc-sidebar::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(
    90deg,
    rgba(120, 180, 255, 0.08) 0%,
    rgba(180, 220, 255, 0.42) 22%,
    rgba(255, 255, 255, 0.55) 50%,
    rgba(180, 220, 255, 0.42) 78%,
    rgba(120, 180, 255, 0.08) 100%
  );
  box-shadow:
    0 0 6px rgba(140, 190, 255, 0.18),
    0 1px 0 rgba(255, 255, 255, 0.06);
  pointer-events: none;
  z-index: 2;
}

.tc-nav-icon-img {
  width: 22px;
  height: 22px;
  object-fit: contain;
  filter: brightness(0) invert(1);
  opacity: 0.9;
  flex-shrink: 0;
}

.tc-nav-item.router-link-active .tc-nav-icon-img,
.tc-nav-item.active .tc-nav-icon-img {
  opacity: 1;
}

.tc-nav-item-yunshu {
  transform: translateX(-2px);
}

.tc-sidebar.collapsed {
  width: var(--tc-sidebar-collapsed-w);
}

.tc-sidebar-head {
  height: 44px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.tc-sidebar-head h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 400;
  color: var(--tc-text-on-dark);
  white-space: nowrap;
}

.tc-sidebar-nav {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 4px 0;
}

.tc-sidebar-nav::-webkit-scrollbar {
  width: 4px;
}

.tc-sidebar-nav::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
}

.tc-nav-group {
  margin-bottom: 0;
}

.tc-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  height: 46px;
  padding: 0 16px;
  color: var(--tc-text-on-dark-2);
  font-size: 18px;
  font-weight: 400;
  text-decoration: none;
  border: none;
  background: transparent;
  cursor: pointer;
  font-family: var(--tc-font-nav);
  text-align: left;
  transition:
    background 0.15s,
    color 0.15s;
  white-space: nowrap;
  box-sizing: border-box;
}

.tc-nav-item:hover {
  background: var(--tc-hover-dark);
  color: var(--tc-text-on-dark);
}

.tc-nav-item.router-link-active,
.tc-nav-item.active {
  background: var(--tc-active-dark);
  color: var(--tc-text-on-dark);
  font-weight: 400;
}

.tc-nav-toggle.expanded:not(.active) {
  color: var(--tc-text-on-dark);
  background: rgba(255, 255, 255, 0.04);
}

.tc-nav-icon {
  flex-shrink: 0;
  opacity: 0.85;
}

.tc-nav-item.router-link-active .tc-nav-icon,
.tc-nav-item.active .tc-nav-icon {
  opacity: 1;
}

.tc-nav-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tc-nav-chevron {
  flex-shrink: 0;
  opacity: 0.5;
  transition: transform 0.2s;
}

.tc-nav-chevron.open {
  transform: rotate(180deg);
}

/* Sub-nav (accordion dropdown) — 与侧栏同色，不再叠加深黑底 */
.tc-nav-sub {
  background: transparent;
  padding: 2px 0 6px;
}

.tc-nav-sub-group {
  display: block;
}

.tc-nav-sub-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  border: none;
  background: transparent;
  cursor: pointer;
  font-family: var(--tc-font-nav);
  text-align: left;
}

.tc-nav-sub-toggle.expanded,
.tc-nav-sub-toggle.active {
  color: var(--tc-text-on-dark);
  background: rgba(255, 255, 255, 0.05);
}

.tc-nav-sub-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tc-nav-sub-chevron {
  flex-shrink: 0;
  opacity: 0.45;
  transition: transform 0.2s;
}

.tc-nav-sub-chevron.open {
  transform: rotate(180deg);
}

.tc-nav-sub-children {
  overflow: hidden;
}

.tc-nav-sub-item {
  display: block;
  height: 40px;
  line-height: 40px;
  padding: 0 16px 0 42px;
  color: rgba(255, 255, 255, 0.62);
  font-size: 16px;
  font-family: var(--tc-font-nav);
  text-decoration: none;
  transition:
    color 0.15s,
    background 0.15s;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tc-nav-sub-item:hover {
  color: var(--tc-text-on-dark);
  background: var(--tc-hover-dark);
}

.tc-nav-sub-item.router-link-active {
  color: var(--tc-text-on-dark);
  background: rgba(255, 255, 255, 0.08);
  font-weight: 400;
}

.tc-nav-sub-child {
  padding-left: 54px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
}

.tc-sidebar-foot {
  padding: 8px 12px;
  flex-shrink: 0;
}

.tc-collapse-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  width: 100%;
  height: 34px;
  padding: 0 4px;
  border: none;
  background: transparent;
  color: var(--tc-text-on-dark-3);
  font-size: 15px;
  font-family: var(--tc-font-nav);
  cursor: pointer;
}

.tc-collapse-btn:hover {
  color: var(--tc-text-on-dark-2);
}

/* Flyout when collapsed */
.tc-flyout {
  position: absolute;
  left: var(--tc-sidebar-collapsed-w);
  top: 0;
  bottom: 0;
  width: 180px;
  background: var(--tc-bg-sidebar);
  border-right: 1px solid rgba(255, 255, 255, 0.07);
  z-index: 99;
  overflow-y: auto;
  box-shadow: 4px 0 12px rgba(0, 0, 0, 0.15);
}

.tc-flyout::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(
    90deg,
    rgba(120, 180, 255, 0.08) 0%,
    rgba(180, 220, 255, 0.42) 22%,
    rgba(255, 255, 255, 0.55) 50%,
    rgba(180, 220, 255, 0.42) 78%,
    rgba(120, 180, 255, 0.08) 100%
  );
  box-shadow:
    0 0 6px rgba(140, 190, 255, 0.18),
    0 1px 0 rgba(255, 255, 255, 0.06);
  pointer-events: none;
}

.tc-flyout-head {
  height: 46px;
  line-height: 46px;
  padding: 0 16px;
  font-size: 17px;
  font-weight: 500;
  color: var(--tc-text-on-dark);
  border-bottom: 1px solid var(--tc-border-dark);
}

.tc-flyout-group {
  display: block;
}

.tc-flyout-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  border: none;
  background: transparent;
  cursor: pointer;
  font-family: var(--tc-font-nav);
  text-align: left;
  color: var(--tc-text-on-dark-2);
}

.tc-flyout-toggle.expanded,
.tc-flyout-toggle.active {
  color: var(--tc-text-on-dark);
  background: var(--tc-hover-dark);
}

.tc-flyout-children {
  overflow: hidden;
}

.tc-flyout-item {
  display: block;
  height: 40px;
  line-height: 40px;
  padding: 0 16px;
  color: var(--tc-text-on-dark-2);
  font-size: 16px;
  text-decoration: none;
}

.tc-flyout-item:hover {
  background: var(--tc-hover-dark);
  color: var(--tc-text-on-dark);
}

.tc-flyout-item.router-link-active {
  background: rgba(255, 255, 255, 0.1);
  color: #ffffff;
}

.tc-flyout-child {
  padding-left: 28px;
  font-size: 13px;
}

/* ===== Main Content ===== */
.tc-main:not(:has(.chatgpt-page)) {
  background: var(--mes-watercolor-bg);
}

.tc-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.tc-page-head {
  padding: 16px 20px 0;
  flex-shrink: 0;
}

.tc-page-title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: var(--tc-text-body);
  line-height: 1.5;
}

.tc-content {
  flex: 1;
  padding: 12px 20px 20px;
  overflow-y: auto;
  min-width: 0;
}

.tc-content--hero {
  padding-top: 0;
  padding-left: 0;
  padding-right: 0;
}

.tc-content--yunshu-ui {
  padding: 0;
  overflow: hidden;
  background: var(--mes-watercolor-bg);
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.tc-content:not(:has(> .chatgpt-page)) {
  background: var(--mes-watercolor-bg);
}

.tc-content--yunshu-ui > :deep(*) {
  flex: 1;
  min-height: 0;
  min-width: 0;
}

.tc-content--bleed {
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.tc-content--bleed > :deep(*) {
  flex: 1;
  min-height: 0;
  height: 100%;
}

/* Content deep overrides — scoped to content area only (skip yunshu-ui pages) */
.tc-content:not(.tc-content--yunshu-ui) :deep(table) {
  min-width: 0;
  width: 100%;
  border-collapse: collapse;
}
.tc-content:not(.tc-content--yunshu-ui) :deep(th) {
  color: var(--tc-text-label);
  background: #fafafa;
  font-size: 13px;
  font-weight: 400;
  padding: 10px 16px;
  border-bottom: 1px solid #e7e7e7;
  text-align: left;
}
.tc-content:not(.tc-content--yunshu-ui) :deep(td) {
  color: var(--tc-text-body);
  font-size: 13px;
  padding: 10px 16px;
  border-bottom: 1px solid #f0f0f0;
}
.tc-content:not(.tc-content--yunshu-ui) :deep(tbody tr:hover) {
  background: #fafafa;
}
.tc-content:not(.tc-content--yunshu-ui) :deep(input),
.tc-content:not(.tc-content--yunshu-ui) :deep(select),
.tc-content:not(.tc-content--yunshu-ui) :deep(textarea) {
  background: #fff;
  color: var(--tc-text-body);
  border: 1px solid #dcdcdc;
  border-radius: 0;
  font-size: 13px;
  font-family: var(--tc-font-ui);
}

/* 云枢小智输入框：无内框，保持干净 */
.tc-content--bleed :deep(.composer-card textarea) {
  border: none;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  padding: 0;
  font-size: 16px;
}

.tc-content--bleed :deep(.composer-card .composer-body) {
  min-height: 64px;
  padding: 20px 28px 0;
}

.tc-content--bleed :deep(.composer-card .composer-toolbar) {
  padding: 6px 20px 12px;
}

.tc-content--bleed :deep(.composer-card textarea:focus) {
  border: none;
  box-shadow: none;
  outline: none;
}

/* 仅作用于原生表单控件；Element Plus 输入框自带聚焦边框，再叠阴影会重影 */
.tc-content :deep(input:focus),
.tc-content :deep(select:focus) {
  border-color: var(--tc-blue);
  outline: none;
  box-shadow: 0 0 0 2px rgba(0, 82, 217, 0.1);
}

.tc-content :deep(.el-input__wrapper input:focus),
.tc-content :deep(.el-textarea__inner:focus),
.tc-content :deep(.el-select__wrapper input:focus),
.tc-content :deep(.el-range-editor input:focus),
.tc-content :deep(.el-input-number input:focus),
.tc-content :deep(.yunshu-ui-root input:focus),
.tc-content :deep(.yunshu-ui-root select:focus),
.tc-content :deep(.yunshu-ui-root textarea:focus) {
  border-color: transparent;
  outline: none;
  box-shadow: none !important;
}

.tc-content :deep(a) {
  color: var(--tc-blue);
}
</style>
