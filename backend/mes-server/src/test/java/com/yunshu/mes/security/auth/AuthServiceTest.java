package com.yunshu.mes.security.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.yunshu.mes.common.exception.BusinessException;
import com.yunshu.mes.security.auth.dto.LoginRequest;
import com.yunshu.mes.system.compat.repository.LogininforCompatRepository;
import com.yunshu.mes.system.compat.repository.PermissionRepository;
import com.yunshu.mes.system.dto.AuthUser;
import com.yunshu.mes.system.service.SystemService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private SystemService systemService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private LogininforCompatRepository logininforRepo;
    @Mock
    private PermissionRepository permissionRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(systemService, passwordEncoder, jwtTokenService,
                logininforRepo, permissionRepository);
    }

    @Test
    void loginRejectsMultipleRoles() {
        AuthUser user = new AuthUser(1L, "multi", "hash", "Multi", "Dept", "ENABLED",
                List.of("MANAGER", "PROD_SUPERVISOR"), List.of("A", "B"));
        when(systemService.findAuthUser("multi")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        BusinessException error = assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("multi", "123456")));

        assertEquals("账号绑定多个角色，请联系管理员", error.getMessage());
    }

    @Test
    void loginReturnsRoleCodeAndPermissions() {
        AuthUser user = new AuthUser(2L, "supervisor", "hash", "Supervisor", "Prod", "ENABLED",
                List.of("PROD_SUPERVISOR"), List.of("生产主管"));
        when(systemService.findAuthUser("supervisor")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtTokenService.generate(anyLong(), anyString(), any())).thenReturn("token");
        when(permissionRepository.findPermsByUserId(2L)).thenReturn(List.of("system:user:query"));

        var response = authService.login(new LoginRequest("supervisor", "123456"));

        assertEquals("PROD_SUPERVISOR", response.roleCode());
        assertEquals(List.of("system:user:query"), response.permissions());
        assertEquals("token", response.token());
    }
}
