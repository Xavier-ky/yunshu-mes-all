package com.yunshu.mes.system.controller;

import com.yunshu.mes.common.response.ApiResponse;
import com.yunshu.mes.system.service.SystemService;
import com.yunshu.mes.system.vo.RoleVO;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/roles")
public class RoleController {

    private final SystemService systemService;

    public RoleController(SystemService systemService) {
        this.systemService = systemService;
    }

    @GetMapping
    public ApiResponse<List<RoleVO>> listRoles(HttpServletRequest request) {
        return ApiResponse.success(systemService.listRoles(), request);
    }
}
