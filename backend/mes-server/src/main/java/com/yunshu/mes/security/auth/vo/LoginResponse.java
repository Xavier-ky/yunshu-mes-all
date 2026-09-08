package com.yunshu.mes.security.auth.vo;

import java.util.List;

public record LoginResponse(
        String token,
        String tokenType,
        Long userId,
        String username,
        String realName,
        String deptName,
        String roleCode,
        List<String> roleCodes,
        List<String> roles,
        List<String> permissions
) {
}
