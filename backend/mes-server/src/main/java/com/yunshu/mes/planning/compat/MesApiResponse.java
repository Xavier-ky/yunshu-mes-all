package com.yunshu.mes.planning.compat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** MES 兼容响应：TableDataInfo / AjaxResult 形状。 */
public final class MesApiResponse {

    private MesApiResponse() {}

    public static Map<String, Object> ok() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", 200);
        m.put("msg", "操作成功");
        return m;
    }

    public static Map<String, Object> ok(Object data) {
        Map<String, Object> m = ok();
        m.put("data", data);
        return m;
    }

    public static Map<String, Object> table(List<?> rows, long total) {
        Map<String, Object> m = ok();
        m.put("rows", rows);
        m.put("total", total);
        return m;
    }

    public static Map<String, Object> error(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", 500);
        m.put("msg", msg);
        return m;
    }

    public static Map<String, Object> badRequest(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", 400);
        m.put("msg", msg);
        return m;
    }

    public static Map<String, Object> toAjax(int rows) {
        return rows > 0 ? ok() : error("操作失败");
    }
}
