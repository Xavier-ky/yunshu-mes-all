package com.yunshu.mes.system.compat;

/** 导入时部门/角色解析结果 */
public record ResolveResult(Long id, String error) {

    public boolean ok() {
        return id != null && error == null;
    }

    public static ResolveResult success(Long id) {
        return new ResolveResult(id, null);
    }

    public static ResolveResult failure(String error) {
        return new ResolveResult(null, error);
    }
}
