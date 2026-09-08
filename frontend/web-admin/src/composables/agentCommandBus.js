/**
 * Agent 命令总线 —— Agent 与其他模块之间的解耦层。
 *
 * 设计目标：Agent 页面下发指令（navigate / tool / notify），各业务模块通过
 * registerTool / onCommand 自愿接入。总线本身不 import 任何业务模块或 router，
 * 保持单向依赖：业务模块依赖总线，总线不依赖业务模块。
 *
 * 预留接口（供后续 Agent 接入大模型后自动执行指令）：
 *   - registerTool(name, fn)：模块向 Agent 暴露一个可执行工具
 *   - listTools()：列出已注册工具，供 Agent 上报给大模型作为可用能力
 *   - dispatch({type, ...})：Agent 下发一条指令
 *       type="navigate"  打开某页面（path）
 *       type="tool"      调用已注册工具（name, args）
 *       type="notify"    发通知（message, level）
 *   - onCommand(fn)：订阅指令流（Agent 页面用它接管 navigate 等）
 */
const tools = new Map();
const listeners = new Set();

export const agentCommandBus = {
  registerTool(name, fn) {
    tools.set(name, fn);
    return () => tools.delete(name);
  },

  unregisterTool(name) {
    tools.delete(name);
  },

  listTools() {
    return Array.from(tools.keys());
  },

  onCommand(fn) {
    listeners.add(fn);
    return () => listeners.delete(fn);
  },

  dispatch(command) {
    if (!command || !command.type) return undefined;
    if (command.type === "tool" && tools.has(command.name)) {
      try {
        return tools.get(command.name)(command.args || {});
      } catch (e) {
        console.error("[agentCommandBus] tool 执行失败:", command.name, e);
        return undefined;
      }
    }
    listeners.forEach((fn) => {
      try {
        fn(command);
      } catch (e) {
        console.error("[agentCommandBus] 指令订阅者异常:", e);
      }
    });
    return undefined;
  },
};
