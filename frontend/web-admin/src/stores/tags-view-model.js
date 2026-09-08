export const HOME_TAB_PATH = "/app";
export const YUNSHU_AI_PATH = "/app/yunshu-ai";

export function createHomeTab() {
  return {
    key: HOME_TAB_PATH,
    path: HOME_TAB_PATH,
    fullPath: HOME_TAB_PATH,
    name: "new-dashboard",
    title: "生产大屏",
    affix: true,
  };
}

export function createTabFromRoute(route) {
  if (!route?.name || !route.path?.startsWith("/app") || route.path === YUNSHU_AI_PATH) {
    return null;
  }
  if (route.path === HOME_TAB_PATH) {
    return createHomeTab();
  }

  const fullPath = route.fullPath || route.path;
  return {
    key: fullPath,
    path: route.path,
    fullPath,
    name: String(route.name),
    title: String(route.meta?.title || "未命名页面"),
    affix: route.path === HOME_TAB_PATH,
  };
}

export function addVisitedTab(tabs, tab) {
  if (!tab || tabs.some((item) => item.key === tab.key)) {
    return tabs;
  }
  return [...tabs, tab];
}

export function closeVisitedTab(tabs, key, activeKey) {
  const index = tabs.findIndex((item) => item.key === key);
  if (index < 0 || tabs[index].affix) {
    return { tabs, nextPath: null };
  }

  const nextTabs = tabs.filter((item) => item.key !== key);
  if (key !== activeKey) {
    return { tabs: nextTabs, nextPath: null };
  }

  const neighbor = nextTabs[Math.max(0, index - 1)] || nextTabs[0];
  return { tabs: nextTabs, nextPath: neighbor?.fullPath || HOME_TAB_PATH };
}

export function sanitizeVisitedTabs(savedTabs, resolve, canAccess = () => true) {
  const tabs = [createHomeTab()];
  for (const saved of Array.isArray(savedTabs) ? savedTabs : []) {
    if (!saved?.fullPath || saved.fullPath === HOME_TAB_PATH) continue;
    const route = resolve(saved.fullPath);
    if (!route?.matched?.length || !canAccess(route)) continue;
    const tab = createTabFromRoute(route);
    if (tab && !tabs.some((item) => item.key === tab.key)) tabs.push(tab);
  }
  return tabs;
}
