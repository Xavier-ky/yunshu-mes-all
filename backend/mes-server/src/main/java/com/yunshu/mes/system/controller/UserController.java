package com.yunshu.mes.system.controller;

import com.yunshu.mes.common.response.ApiResponse;
import com.yunshu.mes.system.dto.UserCreateDTO;
import com.yunshu.mes.system.service.SystemService;
import com.yunshu.mes.system.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/users")
public class UserController {

    private final SystemService systemService;

    public UserController(SystemService systemService) {
        this.systemService = systemService;
    }

    @GetMapping
    public ApiResponse<List<UserVO>> listUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            HttpServletRequest request) {
        return ApiResponse.success(systemService.listUsers(keyword, status), request);
    }

    @GetMapping("/{id}")
    public ApiResponse<UserVO> getUser(@PathVariable("id") Long id, HttpServletRequest request) {
        return ApiResponse.success(systemService.getUserById(id)
                .orElseThrow(() -> new com.yunshu.mes.common.exception.BusinessException(
                        com.yunshu.mes.common.exception.ErrorCode.NOT_FOUND, "用户不存在")), request);
    }

    @PostMapping
    public ApiResponse<UserVO> createUser(@Valid @RequestBody UserCreateDTO dto, HttpServletRequest request) {
        return ApiResponse.success(systemService.createUser(dto), request);
    }

    @PutMapping("/{id}")
    public ApiResponse<UserVO> updateUser(@PathVariable("id") Long id, @RequestBody UserCreateDTO dto,
                                          HttpServletRequest request) {
        return ApiResponse.success(systemService.updateUser(id, dto), request);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable("id") Long id, HttpServletRequest request) {
        systemService.deleteUser(id);
        return ApiResponse.success(null, request);
    }
}
