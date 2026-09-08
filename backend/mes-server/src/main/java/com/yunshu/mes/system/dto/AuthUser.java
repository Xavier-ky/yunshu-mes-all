package com.yunshu.mes.system.dto;

import java.util.List;

/**
 * 登录鉴权用的用户凭证与角色信息，包含密码哈希，仅在 security/auth 内部使用，不对外暴露。
 */
public record AuthUser(
        Long userId,
        String username,
        String passwordHash,
        String realName,
        String deptName,
        String status,
        List<String> roleCodes,
        List<String> roleNames
) {
}
