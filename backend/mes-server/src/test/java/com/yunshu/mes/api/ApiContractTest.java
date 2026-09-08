package com.yunshu.mes.api;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yunshu.mes.security.crypto.SymmetricCryptoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiContractTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SymmetricCryptoService symmetricCryptoService;

    @Test
    void healthEndpointReturnsUnifiedResponse() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.traceId", notNullValue()))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.service").value("yunshu-mes-server"));
    }

    @Test
    void systemUserEndpointReturnsInitialUsers() throws Exception {
        mockMvc.perform(get("/api/system/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.data[0].username").value("admin"))
                .andExpect(jsonPath("$.data[0].employeeNo").value("U0001"))
                .andExpect(jsonPath("$.data[0].roleCodes[0]").value("MANAGER"))
                .andExpect(jsonPath("$.data[0].roles[0]").value("管理人员"));
    }

    @Test
    void systemRoleEndpointReturnsInitialRoles() throws Exception {
        mockMvc.perform(get("/api/system/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(4))))
                .andExpect(jsonPath("$.data[0].roleCode").value("MANAGER"));
    }

    @Test
    void dashboardSummaryReturnsMesMetricsAndWorkItems() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.metrics", hasSize(greaterThanOrEqualTo(4))))
                .andExpect(jsonPath("$.data.metrics[0].name").value("今日计划产量"))
                .andExpect(jsonPath("$.data.metrics[1].name").value("今日完工数量"))
                .andExpect(jsonPath("$.data.metrics[2].name").value("工单达成率"))
                .andExpect(jsonPath("$.data.workOrders", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.data.alerts", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    void lineMonitorSummaryReturnsLinesAndMetrics() throws Exception {
        mockMvc.perform(get("/api/factory/lines/monitor/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.lines", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.metrics", hasSize(greaterThanOrEqualTo(6))))
                .andExpect(jsonPath("$.data.currentShiftName", notNullValue()))
                .andExpect(jsonPath("$.data.equipmentSummary.total", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.equipmentSummary.runningRate", notNullValue()))
                .andExpect(jsonPath("$.data.alertSummary.total", greaterThanOrEqualTo(0)));
    }

    @Test
    void loginEndpointReturnsTokenForDemoAccount() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.roleCodes[0]").value("MANAGER"))
                .andExpect(jsonPath("$.data.roles[0]").value("管理人员"));
    }

    @Test
    void loginEndpointRejectsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void loginEndpointAcceptsAesGcmEncryptedPassword() throws Exception {
        var encrypted = symmetricCryptoService.encrypt("admin123");
        String requestBody = "{\"username\":\"admin\",\"encryptedPassword\":\""
                + encrypted.cipherText() + "\",\"iv\":\"" + encrypted.iv() + "\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }
}
