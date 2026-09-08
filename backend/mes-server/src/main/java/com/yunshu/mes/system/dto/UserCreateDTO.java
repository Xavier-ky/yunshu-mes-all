package com.yunshu.mes.system.dto;

import java.util.List;

/**
 * 用户新增/编辑入参。password 为空时后端按默认密码处理。
 */
public record UserCreateDTO(
        String username,
        String realName,
        String employeeNo,
        Long deptId,
        String status,
        String password,
        List<String> roleCodes
) {
}
