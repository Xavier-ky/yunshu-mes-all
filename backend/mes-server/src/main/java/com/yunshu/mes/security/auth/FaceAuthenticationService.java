package com.yunshu.mes.security.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.mes.common.exception.BusinessException;
import com.yunshu.mes.common.exception.ErrorCode;
import com.yunshu.mes.security.auth.dto.FaceLoginRequest;
import com.yunshu.mes.security.auth.vo.LoginResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Trusted bridge to the loopback-only DeepFace service.
 *
 * The browser never selects a target account and the Python service never
 * issues tokens.  A successful local match is bound exclusively to tester;
 * AuthService remains the single issuer of MES JWTs.
 */
@Service
public class FaceAuthenticationService {

    private static final String TESTER_USERNAME = "tester";
    private static final int FACE_SERVICE_MAX_ATTEMPTS = 5;
    private static final long FACE_SERVICE_RETRY_DELAY_MS = 3_500L;
    private static final Logger log = LoggerFactory.getLogger(FaceAuthenticationService.class);

    private final AuthService authService;
    private final String faceAuthBaseUrl;
    private final ObjectMapper objectMapper;

    public FaceAuthenticationService(
            AuthService authService,
            ObjectMapper objectMapper,
            @Value("${mes.face-auth.base-url:http://127.0.0.1:8091}") String faceAuthBaseUrl) {
        this.authService = authService;
        this.objectMapper = objectMapper;
        this.faceAuthBaseUrl = faceAuthBaseUrl.replaceAll("/+$", "");
    }

    public LoginResponse loginTester(FaceLoginRequest request) {
        if (!verifyLocalFace(request.imageBase64())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "\u4eba\u8138\u6838\u9a8c\u672a\u901a\u8fc7");
        }
        return authService.loginWithVerifiedFace(TESTER_USERNAME);
    }

    @SuppressWarnings("unchecked")
    private boolean verifyLocalFace(String imageBase64) {
        try {
            // Send an explicit JSON string. This avoids any form-style
            // conversion ambiguity for FastAPI's required image_base64 field.
            String payload = objectMapper.writeValueAsString(Map.of("image_base64", imageBase64));
            IOException lastFailure = null;
            for (int attempt = 1; attempt <= FACE_SERVICE_MAX_ATTEMPTS; attempt++) {
                try {
                    return invokeFaceService(payload);
                } catch (IOException ex) {
                    lastFailure = ex;
                    if (attempt < FACE_SERVICE_MAX_ATTEMPTS) {
                        // The local watchdog restarts a stopped worker, then
                        // DeepFace warms the enrollment cache. Retry only on
                        // transport failure so this brief recovery window is
                        // not surfaced as a user login failure.
                        log.info("Face service unavailable; retrying after watchdog recovery: {}", ex.getMessage());
                        try {
                            Thread.sleep(FACE_SERVICE_RETRY_DELAY_MS);
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "\u4eba\u8138\u6838\u9a8c\u670d\u52a1\u6682\u4e0d\u53ef\u7528");
                        }
                    }
                }
            }
            throw lastFailure == null ? new IOException("Face service did not return a result.") : lastFailure;
        } catch (IOException ex) {
            // Do not log the image payload; only record the transport failure
            // needed to diagnose the loopback integration.
            log.warn("Face verification service request failed: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "\u4eba\u8138\u6838\u9a8c\u670d\u52a1\u6682\u4e0d\u53ef\u7528");
        }
    }

    @SuppressWarnings("unchecked")
    private boolean invokeFaceService(String payload) throws IOException {
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = (HttpURLConnection) URI
                .create(faceAuthBaseUrl + "/api/face/verify")
                .toURL()
                .openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(90_000);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setFixedLengthStreamingMode(payloadBytes.length);
        try {
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payloadBytes);
            }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException("Face service returned HTTP " + status);
            }
            try (InputStream input = connection.getInputStream()) {
                Map<String, Object> response = objectMapper.readValue(input, Map.class);
                return response != null && Boolean.TRUE.equals(response.get("verified"));
            }
        } finally {
            connection.disconnect();
        }
    }
}
