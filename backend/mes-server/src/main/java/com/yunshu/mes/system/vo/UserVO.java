package com.yunshu.mes.system.vo;

import java.util.List;

public record UserVO(
        Long userId,
        String username,
        String realName,
        String employeeNo,
        String deptName,
        String status,
        List<String> roleCodes,
        List<String> roles
) {
}
