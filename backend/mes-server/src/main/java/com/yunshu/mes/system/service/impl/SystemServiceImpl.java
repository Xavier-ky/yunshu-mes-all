package com.yunshu.mes.system.service.impl;

import com.yunshu.mes.common.exception.BusinessException;
import com.yunshu.mes.common.exception.ErrorCode;
import com.yunshu.mes.system.dto.AuthUser;
import com.yunshu.mes.system.dto.UserCreateDTO;
import com.yunshu.mes.system.repository.RoleRepository;
import com.yunshu.mes.system.repository.UserRepository;
import com.yunshu.mes.system.service.SystemMockDataService;
import com.yunshu.mes.system.service.SystemService;
import com.yunshu.mes.system.vo.RoleVO;
import com.yunshu.mes.system.vo.UserVO;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 系统管理域服务门面：优先走 JDBC 仓储，查询异常（含未连库）时回退到 Mock 数据。
 * 标记 {@link Primary} 使 Controller / Auth 依赖 {@link SystemService} 时注入本实现。
 */
@Service
@Primary
public class SystemServiceImpl implements SystemService {

    private static final Logger log = LoggerFactory.getLogger(SystemServiceImpl.class);
    private static final String DEFAULT_PASSWORD = "123456";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SystemMockDataService mock;
    private final PasswordEncoder passwordEncoder;

    public SystemServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                             SystemMockDataService mock, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.mock = mock;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<UserVO> listUsers(String keyword, String status) {
        try {
            return userRepository.findAllWithRoles(keyword, status);
        } catch (DataAccessException e) {
            log.warn("用户列表查询回退 Mock：{}", e.getMessage());
            return mock.listUsers(keyword, status);
        }
    }

    @Override
    public Optional<UserVO> getUserById(Long id) {
        try {
            Optional<UserVO> result = userRepository.findById(id);
            return result.or(() -> mock.getUserById(id));
        } catch (DataAccessException e) {
            log.warn("用户详情查询回退 Mock：{}", e.getMessage());
            return mock.getUserById(id);
        }
    }

    @Override
    public UserVO createUser(UserCreateDTO dto) {
        try {
            String rawPassword = dto.password() == null || dto.password().isBlank() ? DEFAULT_PASSWORD : dto.password();
            String passwordHash = passwordEncoder.encode(rawPassword);
            String status = dto.status() == null || dto.status().isBlank() ? "ENABLED" : dto.status();
            Long id = userRepository.insert(dto.username(), passwordHash, dto.employeeNo(),
                    dto.realName(), dto.deptId(), status);
            if (id == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建用户失败：未返回主键");
            }
            userRepository.syncRoles(id, dto.roleCodes());
            return userRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "创建用户后回查失败"));
        } catch (DataAccessException e) {
            log.warn("创建用户回退 Mock：{}", e.getMessage());
            return mock.createUser(dto);
        }
    }

    @Override
    public UserVO updateUser(Long id, UserCreateDTO dto) {
        try {
            String status = dto.status() == null || dto.status().isBlank() ? "ENABLED" : dto.status();
            userRepository.update(id, dto.username(), dto.realName(), dto.employeeNo(), dto.deptId(), status);
            userRepository.syncRoles(id, dto.roleCodes());
            return userRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
        } catch (DataAccessException e) {
            log.warn("更新用户回退 Mock：{}", e.getMessage());
            return mock.updateUser(id, dto);
        }
    }

    @Override
    public void deleteUser(Long id) {
        try {
            userRepository.softDelete(id);
        } catch (DataAccessException e) {
            log.warn("删除用户回退 Mock：{}", e.getMessage());
            mock.deleteUser(id);
        }
    }

    @Override
    public List<RoleVO> listRoles() {
        try {
            return roleRepository.findAllRoles();
        } catch (DataAccessException e) {
            log.warn("角色列表查询回退 Mock：{}", e.getMessage());
            return mock.listRoles();
        }
    }

    @Override
    public Optional<AuthUser> findAuthUser(String username) {
        try {
            return userRepository.findAuthUserByUsername(username);
        } catch (DataAccessException e) {
            log.warn("登录凭证查询回退 Mock：{}", e.getMessage());
            return mock.findAuthUser(username);
        }
    }
}
