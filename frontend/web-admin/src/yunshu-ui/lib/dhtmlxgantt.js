import "dhtmlx-gantt/codebase/dhtmlxgantt.css";
import "dhtmlx-gantt/codebase/dhtmlxgantt.js";

function resolveGantt() {
  if (typeof window === "undefined") {
    throw new Error("dhtmlx-gantt can only run in the browser");
  }
  const g = window.gantt || window.dhtmlxgantt?.gantt;
  if (!g) {
    throw new Error("dhtmlx-gantt failed to attach to window");
  }
  return g;
}

/** Proxy keeps existing `gantt.xxx()` call sites; UMD has no ESM named exports in Vite dev. */
export const gantt = new Proxy(
  {},
  {
    get(_target, prop) {
      const api = resolveGantt();
      const value = api[prop];
      return typeof value === "function" ? value.bind(api) : value;
    },
    set(_target, prop, value) {
      resolveGantt()[prop] = value;
      return true;
    },
  },
);

export default gantt;
