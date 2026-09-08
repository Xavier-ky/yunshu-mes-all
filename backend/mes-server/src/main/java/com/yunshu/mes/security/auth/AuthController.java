package com.yunshu.mes.security.auth;

import com.yunshu.mes.common.response.ApiResponse;
import com.yunshu.mes.security.crypto.SymmetricCryptoService;
import com.yunshu.mes.security.auth.dto.FaceLoginRequest;
import com.yunshu.mes.security.auth.dto.LoginRequest;
import com.yunshu.mes.security.auth.vo.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final FaceAuthenticationService faceAuthenticationService;
    private final SymmetricCryptoService symmetricCryptoService;

    public AuthController(AuthService authService, FaceAuthenticationService faceAuthenticationService,
                          SymmetricCryptoService symmetricCryptoService) {
        this.authService = authService;
        this.faceAuthenticationService = faceAuthenticationService;
        this.symmetricCryptoService = symmetricCryptoService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String password = request.hasEncryptedPassword()
                ? symmetricCryptoService.decrypt(request.encryptedPassword(), request.iv())
                : request.password();
        return ApiResponse.success(authService.login(new LoginRequest(request.username(), password)), httpRequest);
    }

    @PostMapping("/face-login")
    public ApiResponse<LoginResponse> faceLogin(
            @Valid @RequestBody FaceLoginRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.success(faceAuthenticationService.loginTester(request), httpRequest);
    }

    @GetMapping("/me")
    public ApiResponse<LoginResponse> me(HttpServletRequest httpRequest) {
        Object username = httpRequest.getAttribute("currentUser");
        if (username == null) {
            return ApiResponse.fail("UNAUTHORIZED", "未登录或令牌无效", httpRequest);
        }
        return ApiResponse.success(authService.currentUser(String.valueOf(username)), httpRequest);
    }
}
