package com.yunshu.mes.security.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yunshu.mes.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SymmetricCryptoServiceTest {

    private SymmetricCryptoService cryptoService;

    @BeforeEach
    void setUp() {
        CryptoProperties properties = new CryptoProperties();
        properties.setEnabled(true);
        properties.setAlgorithm("AES-GCM");
        properties.setKeyLength(256);
        properties.setKey("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWY=");
        cryptoService = new SymmetricCryptoService(properties);
    }

    @Test
    void roundTripsUtf8TextWithAesGcm() {
        var encrypted = cryptoService.encrypt("密码-123456");

        assertEquals("密码-123456", cryptoService.decrypt(encrypted.cipherText(), encrypted.iv()));
    }

    @Test
    void rejectsTamperedCipherText() {
        var encrypted = cryptoService.encrypt("admin123");
        String tampered = encrypted.cipherText().substring(0, encrypted.cipherText().length() - 2) + "AA";

        assertThrows(BusinessException.class, () -> cryptoService.decrypt(tampered, encrypted.iv()));
    }
}
