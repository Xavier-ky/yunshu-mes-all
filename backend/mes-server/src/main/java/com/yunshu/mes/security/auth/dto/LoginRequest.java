package com.yunshu.mes.security.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String username,
        String password,
        String encryptedPassword,
        String iv
) {
    public LoginRequest(String username, String password) {
        this(username, password, null, null);
    }

    @AssertTrue(message = "password 或 encryptedPassword 不能为空")
    public boolean hasPassword() {
        return (password != null && !password.isBlank())
                || (encryptedPassword != null && !encryptedPassword.isBlank());
    }

    public boolean hasEncryptedPassword() {
        return encryptedPassword != null && !encryptedPassword.isBlank();
    }
}
