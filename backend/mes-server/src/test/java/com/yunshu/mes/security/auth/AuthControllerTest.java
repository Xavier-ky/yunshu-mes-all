package com.yunshu.mes.security.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yunshu.mes.security.auth.dto.LoginRequest;
import com.yunshu.mes.security.auth.vo.LoginResponse;
import com.yunshu.mes.security.crypto.CryptoProperties;
import com.yunshu.mes.security.crypto.SymmetricCryptoService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;
    @Mock
    private FaceAuthenticationService faceAuthenticationService;
    @Mock
    private HttpServletRequest httpRequest;

    private AuthController authController;
    private SymmetricCryptoService cryptoService;

    @BeforeEach
    void setUp() {
        CryptoProperties properties = new CryptoProperties();
        properties.setEnabled(true);
        properties.setKey("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWY=");
        cryptoService = new SymmetricCryptoService(properties);
        authController = new AuthController(authService, faceAuthenticationService, cryptoService);
    }

    @Test
    void decryptsPasswordBeforeCallingExistingAuthenticationService() {
        var encrypted = cryptoService.encrypt("admin123");
        when(authService.login(any())).thenReturn(new LoginResponse(
                "token", "Bearer", 1L, "admin", "Admin", "System", "MANAGER",
                List.of("MANAGER"), List.of("Manager"), List.of()));

        var response = authController.login(
                new LoginRequest("admin", null, encrypted.cipherText(), encrypted.iv()), httpRequest);

        ArgumentCaptor<LoginRequest> requestCaptor = ArgumentCaptor.forClass(LoginRequest.class);
        verify(authService).login(requestCaptor.capture());
        assertEquals("admin", requestCaptor.getValue().username());
        assertEquals("admin123", requestCaptor.getValue().password());
        assertEquals("SUCCESS", response.code());
    }
}
