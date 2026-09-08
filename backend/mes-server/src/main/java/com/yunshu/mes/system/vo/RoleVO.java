package com.yunshu.mes.system.vo;

public record RoleVO(
        Long roleId,
        String roleCode,
        String roleName,
        String roleDesc,
        String status
) {
}
