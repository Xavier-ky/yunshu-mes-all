package com.yunshu.mes.agent.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * Agent 工具注册表 —— Agent 调用其他业务模块能力的统一入口。
 *
 * 解耦设计：业务模块在启动时向本注册表 register 自身能力（如「查询工单」「关闭安灯」），
 * Agent（经大模型 function calling）下发 tool 指令时，由本注册表派发到对应模块，
 * Agent 不直接依赖任何业务模块的类。高风险写操作可在注册时自行加审批校验。
 *
 * 与前端 agentCommandBus 对应：前端处理 navigate/notify，后端处理 tool（业务数据操作）。
 */
@Component
public class AgentToolRegistry {

    private final Map<String, Function<Map<String, Object>, String>> tools = new LinkedHashMap<>();

    public synchronized void register(String name, Function<Map<String, Object>, String> handler) {
        tools.put(name, handler);
    }

    public synchronized void unregister(String name) {
        tools.remove(name);
    }

    public synchronized List<String> listTools() {
        return new ArrayList<>(tools.keySet());
    }

    public synchronized String execute(String name, Map<String, Object> args) {
        Function<Map<String, Object>, String> handler = tools.get(name);
        if (handler == null) {
            return "工具未注册: " + name;
        }
        try {
            return handler.apply(args == null ? Map.of() : args);
        } catch (Exception e) {
            return "工具执行失败: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }
}
