package com.yunshu.mes.security.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 鉴权相关配置，绑定 {@code mes.security.jwt}。
 */
@ConfigurationProperties(prefix = "mes.security")
public class AuthProperties {

    private Jwt jwt = new Jwt();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public static class Jwt {
        private String secret = "yunshu-mes-dev-jwt-secret-key-please-change-in-production-2026";
        private int expiryMinutes = 480;
        private boolean enforce = false;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public int getExpiryMinutes() {
            return expiryMinutes;
        }

        public void setExpiryMinutes(int expiryMinutes) {
            this.expiryMinutes = expiryMinutes;
        }

        public boolean isEnforce() {
            return enforce;
        }

        public void setEnforce(boolean enforce) {
            this.enforce = enforce;
        }
    }
}
