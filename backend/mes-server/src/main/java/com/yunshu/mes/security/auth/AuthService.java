package com.yunshu.mes.security.auth;

import com.yunshu.mes.common.exception.BusinessException;
import com.yunshu.mes.common.exception.ErrorCode;
import com.yunshu.mes.security.auth.dto.LoginRequest;
import com.yunshu.mes.security.auth.vo.LoginResponse;
import com.yunshu.mes.system.compat.repository.LogininforCompatRepository;
import com.yunshu.mes.system.compat.repository.PermissionRepository;
import com.yunshu.mes.system.dto.AuthUser;
import com.yunshu.mes.system.service.SystemService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 登录鉴权服务：校验账号密码并签发 JWT。
 */
@Service
public class AuthService {

    private final SystemService systemService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final LogininforCompatRepository logininforRepo;
    private final PermissionRepository permissionRepository;

    public AuthService(SystemService systemService, PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService, LogininforCompatRepository logininforRepo,
                       PermissionRepository permissionRepository) {
        this.systemService = systemService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.logininforRepo = logininforRepo;
        this.permissionRepository = permissionRepository;
    }

    public LoginResponse login(LoginRequest request) {
        String ip = resolveClientIp();
        String username = request.username();
        var userOpt = systemService.findAuthUser(username);
        if (userOpt.isEmpty()) {
            logininforRepo.insert(username, ip, "1", "账号或密码不正确");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码不正确");
        }
        AuthUser user = userOpt.get();
        if (!"ENABLED".equals(user.status())) {
            logininforRepo.insert(username, ip, "1", "账号已停用");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号已停用");
        }
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            logininforRepo.insert(username, ip, "1", "账号或密码不正确");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码不正确");
        }

        List<String> roleCodes = user.roleCodes();
        if (roleCodes == null || roleCodes.isEmpty()) {
            logininforRepo.insert(username, ip, "1", "未分配角色");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未分配角色");
        }
        if (roleCodes.size() > 1) {
            logininforRepo.insert(username, ip, "1", "账号绑定多个角色");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号绑定多个角色，请联系管理员");
        }

        logininforRepo.insert(username, ip, "0", "登录成功");
        return issueToken(user);
    }

    /** Called only after the loopback face verifier has matched the tester enrollment. */
    public LoginResponse loginWithVerifiedFace(String username) {
        String ip = resolveClientIp();
        var userOpt = systemService.findAuthUser(username);
        if (userOpt.isEmpty()) {
            logininforRepo.insert(username, ip, "1", "人脸登录账号不存在");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "人脸核验未通过");
        }
        AuthUser user = userOpt.get();
        if (!"ENABLED".equals(user.status())) {
            logininforRepo.insert(username, ip, "1", "人脸登录账号已停用");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "人脸核验未通过");
        }

        List<String> roleCodes = user.roleCodes();
        if (roleCodes == null || roleCodes.isEmpty() || roleCodes.size() > 1) {
            logininforRepo.insert(username, ip, "1", "人脸登录账号角色配置异常");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "人脸核验未通过");
        }

        logininforRepo.insert(username, ip, "0", "人脸核验登录成功");
        return issueToken(user);
    }

    private LoginResponse issueToken(AuthUser user) {
        List<String> roleCodes = user.roleCodes();
        String roleCode = roleCodes.get(0);
        List<String> permissions = permissionRepository.findPermsByUserId(user.userId());
        String token = jwtTokenService.generate(user.userId(), user.username(), roleCodes);
        return new LoginResponse(token, "Bearer", user.userId(), user.username(),
                user.realName(), user.deptName(), roleCode, roleCodes, user.roleNames(), permissions);
    }

    public LoginResponse currentUser(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或令牌无效");
        }
        var userOpt = systemService.findAuthUser(username);
        if (userOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户不存在或已停用");
        }
        AuthUser user = userOpt.get();
        List<String> roleCodes = user.roleCodes();
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未分配角色");
        }
        if (roleCodes.size() > 1) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号绑定多个角色，请联系管理员");
        }
        String roleCode = roleCodes.get(0);
        List<String> permissions = permissionRepository.findPermsByUserId(user.userId());
        return new LoginResponse(null, "Bearer", user.userId(), user.username(),
                user.realName(), user.deptName(), roleCode, roleCodes, user.roleNames(), permissions);
    }

    private String resolveClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "";
        }
        HttpServletRequest req = attrs.getRequest();
        String ip = req.getRemoteAddr();
        return ip == null ? "" : ip;
    }
}
