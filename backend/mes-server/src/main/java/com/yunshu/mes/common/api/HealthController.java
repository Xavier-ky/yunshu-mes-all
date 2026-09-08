package com.yunshu.mes.common.api;

import com.yunshu.mes.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health(HttpServletRequest request) {
        return ApiResponse.success(Map.of(
                "status", "UP",
                "service", "yunshu-mes-server",
                "product", "云枢智造 MES",
                "time", OffsetDateTime.now().toString()
        ), request);
    }
}
