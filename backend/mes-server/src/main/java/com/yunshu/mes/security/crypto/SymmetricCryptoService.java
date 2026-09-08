package com.yunshu.mes.security.crypto;

import com.yunshu.mes.common.exception.BusinessException;
import com.yunshu.mes.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class SymmetricCryptoService {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;

    private final CryptoProperties cryptoProperties;

    public SymmetricCryptoService(CryptoProperties cryptoProperties) {
        this.cryptoProperties = cryptoProperties;
    }

    public EncryptedValue encrypt(String plainText) {
        if (plainText == null) {
            throw cryptoError("待加密内容不能为空");
        }
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        new SecureRandom().nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, requireKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return new EncryptedValue(toBase64(encrypted), toBase64(iv));
        } catch (GeneralSecurityException ex) {
            throw cryptoError("AES-GCM 加密失败");
        }
    }

    public String decrypt(String cipherText, String ivText) {
        if (cipherText == null || cipherText.isBlank() || ivText == null || ivText.isBlank()) {
            throw cryptoError("密文或 IV 不能为空");
        }
        try {
            byte[] iv = fromBase64(ivText, "IV");
            if (iv.length != GCM_IV_LENGTH_BYTES) {
                throw cryptoError("AES-GCM IV 长度必须为 12 字节");
            }
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, requireKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] decrypted = cipher.doFinal(fromBase64(cipherText, "密文"));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (BusinessException ex) {
            throw ex;
        } catch (GeneralSecurityException ex) {
            throw cryptoError("AES-GCM 解密失败");
        }
    }

    private SecretKey requireKey() {
        if (!cryptoProperties.isEnabled()) {
            throw cryptoError("AES 登录加密未启用");
        }
        if (!"AES-GCM".equalsIgnoreCase(cryptoProperties.getAlgorithm())) {
            throw cryptoError("仅支持 AES-GCM 算法");
        }
        byte[] key = fromBase64(cryptoProperties.getKey(), "AES 密钥");
        int expectedBytes = cryptoProperties.getKeyLength() / Byte.SIZE;
        if (key.length != expectedBytes) {
            throw cryptoError("AES 密钥长度与配置不符");
        }
        return new SecretKeySpec(key, "AES");
    }

    private static byte[] fromBase64(String value, String field) {
        if (value == null || value.isBlank()) {
            throw cryptoError(field + "不能为空");
        }
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException ex) {
            throw cryptoError(field + "不是合法的 Base64 值");
        }
    }

    private static String toBase64(byte[] value) {
        return Base64.getEncoder().encodeToString(value);
    }

    private static BusinessException cryptoError(String message) {
        return new BusinessException(ErrorCode.CRYPTO_ERROR, message);
    }

    public record EncryptedValue(String cipherText, String iv) {
    }
}
