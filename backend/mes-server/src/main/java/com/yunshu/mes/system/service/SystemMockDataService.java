package com.yunshu.mes.system.service;

import com.yunshu.mes.common.exception.BusinessException;
import com.yunshu.mes.common.exception.ErrorCode;
import com.yunshu.mes.system.dto.AuthUser;
import com.yunshu.mes.system.dto.UserCreateDTO;
import com.yunshu.mes.system.vo.RoleVO;
import com.yunshu.mes.system.vo.UserVO;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 系统管理域的 Mock 数据实现，与 {@code 001_seed_basic_data.sql} 保持一致。
 * 在未连接 MySQL 或 JDBC 查询失败时作为回退数据源使用。写操作在 Mock 模式下不可用。
 */
@Service
public class SystemMockDataService implements SystemService {

    private static final List<UserVO> USERS = List.of(
            new UserVO(1L, "admin", "管理员", "U0001", "管理部", "ENABLED", List.of("MANAGER"), List.of("管理人员")),
            new UserVO(2L, "supervisor01", "生产主管", "U0002", "生产部", "ENABLED", List.of("PROD_SUPERVISOR"), List.of("生产主管")),
            new UserVO(3L, "warehouse01", "仓库物料员", "U0003", "仓储部", "ENABLED", List.of("WAREHOUSE_CLERK"), List.of("仓库物料员")),
            new UserVO(4L, "qc01", "质检员", "U0004", "质量部", "ENABLED", List.of("QUALITY_INSPECTOR"), List.of("质检员")),
            new UserVO(5L, "repair01", "设备维修员", "U0005", "设备部", "ENABLED", List.of("EQUIPMENT_MAINTAINER"), List.of("设备维修员")),
            new UserVO(6L, "worker01", "产线操作工人", "U0006", "生产部", "ENABLED", List.of("LINE_OPERATOR"), List.of("产线操作工人"))
    );

    private static final List<RoleVO> ROLES = List.of(
            new RoleVO(1L, "MANAGER", "管理人员", "查看看板、报表与追溯，并维护用户、角色、权限和系统参数", "ENABLED"),
            new RoleVO(2L, "PROD_SUPERVISOR", "生产主管", "负责订单、工单、排产、齐套、派工和现场生产管理", "ENABLED"),
            new RoleVO(3L, "WAREHOUSE_CLERK", "仓库物料员", "处理备料、领料、发料、退料和库存批次", "ENABLED"),
            new RoleVO(4L, "LINE_OPERATOR", "产线操作工人", "执行工位作业、扫码、报工和发起安灯", "ENABLED"),
            new RoleVO(5L, "QUALITY_INSPECTOR", "质检员", "执行首末件、巡检、成品检验和质量放行", "ENABLED"),
            new RoleVO(6L, "EQUIPMENT_MAINTAINER", "设备维修员", "处理设备点检、保养、报修和维修", "ENABLED")
    );

    private static final List<AuthUser> AUTH_USERS = List.of(
            new AuthUser(1L, "admin", "{noop}admin123", "管理员", "管理部", "ENABLED", List.of("MANAGER"), List.of("管理人员")),
            new AuthUser(2L, "supervisor01", "{noop}123456", "生产主管", "生产部", "ENABLED", List.of("PROD_SUPERVISOR"), List.of("生产主管")),
            new AuthUser(3L, "warehouse01", "{noop}123456", "仓库物料员", "仓储部", "ENABLED", List.of("WAREHOUSE_CLERK"), List.of("仓库物料员")),
            new AuthUser(4L, "qc01", "{noop}123456", "质检员", "质量部", "ENABLED", List.of("QUALITY_INSPECTOR"), List.of("质检员")),
            new AuthUser(5L, "repair01", "{noop}123456", "设备维修员", "设备部", "ENABLED", List.of("EQUIPMENT_MAINTAINER"), List.of("设备维修员")),
            new AuthUser(6L, "worker01", "{noop}123456", "产线操作工人", "生产部", "ENABLED", List.of("LINE_OPERATOR"), List.of("产线操作工人"))
    );

    @Override
    public List<UserVO> listUsers(String keyword, String status) {
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        return USERS.stream()
                .filter(u -> kw.isBlank()
                        || u.username().toLowerCase().contains(kw)
                        || u.realName().toLowerCase().contains(kw)
                        || u.employeeNo().toLowerCase().contains(kw))
                .filter(u -> status == null || status.isBlank() || u.status().equalsIgnoreCase(status))
                .toList();
    }

    @Override
    public Optional<UserVO> getUserById(Long id) {
        return USERS.stream().filter(u -> u.userId().equals(id)).findFirst();
    }

    @Override
    public UserVO createUser(UserCreateDTO dto) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
                "写操作需连接 MySQL 且 fan_mes 库已执行迁移脚本，当前为 Mock 模式");
    }

    @Override
    public UserVO updateUser(Long id, UserCreateDTO dto) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
                "写操作需连接 MySQL 且 fan_mes 库已执行迁移脚本，当前为 Mock 模式");
    }

    @Override
    public void deleteUser(Long id) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
                "写操作需连接 MySQL 且 fan_mes 库已执行迁移脚本，当前为 Mock 模式");
    }

    @Override
    public List<RoleVO> listRoles() {
        return ROLES;
    }

    @Override
    public Optional<AuthUser> findAuthUser(String username) {
        return AUTH_USERS.stream()
                .filter(user -> user.username().equals(username))
                .findFirst();
    }
}
