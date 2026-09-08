import { defineStore } from "pinia";
import {
  addVisitedTab,
  closeVisitedTab,
  createHomeTab,
  createTabFromRoute,
  sanitizeVisitedTabs,
} from "./tags-view-model.js";

export const TAGS_STORAGE_KEY = "mes_visited_tabs";

function getStorage() {
  return typeof sessionStorage === "undefined" ? null : sessionStorage;
}

export const useTagsViewStore = defineStore("tags-view", {
  state: () => ({
    tabs: [createHomeTab()],
    restored: false,
  }),
  actions: {
    persist() {
      try {
        getStorage()?.setItem(TAGS_STORAGE_KEY, JSON.stringify(this.tabs));
      } catch {
        // Storage can be unavailable in private browsing; tabs still work in memory.
      }
    },
    restore(resolve, canAccess) {
      let saved = [];
      try {
        saved = JSON.parse(getStorage()?.getItem(TAGS_STORAGE_KEY) || "[]");
      } catch {
        saved = [];
      }
      this.tabs = sanitizeVisitedTabs(saved, resolve, canAccess);
      this.restored = true;
      this.persist();
    },
    addRoute(route) {
      const tab = createTabFromRoute(route);
      const nextTabs = addVisitedTab(this.tabs, tab);
      if (nextTabs !== this.tabs) {
        this.tabs = nextTabs;
        this.persist();
      }
    },
    closeTab(key, activeKey) {
      const result = closeVisitedTab(this.tabs, key, activeKey);
      if (result.tabs !== this.tabs) {
        this.tabs = result.tabs;
        this.persist();
      }
      return result.nextPath;
    },
    closeOthers(activeKey) {
      const activeTab = this.tabs.find((tab) => tab.key === activeKey);
      this.tabs = [
        createHomeTab(),
        ...(activeTab && !activeTab.affix ? [activeTab] : []),
      ];
      this.persist();
    },
    closeAll(activeKey) {
      this.tabs = [createHomeTab()];
      this.persist();
      return activeKey === "/app" ? null : "/app";
    },
    clearSession() {
      this.tabs = [createHomeTab()];
      this.restored = false;
      try {
        getStorage()?.removeItem(TAGS_STORAGE_KEY);
      } catch {
        // Ignore unavailable storage; in-memory state has already been reset.
      }
    },
  },
});
