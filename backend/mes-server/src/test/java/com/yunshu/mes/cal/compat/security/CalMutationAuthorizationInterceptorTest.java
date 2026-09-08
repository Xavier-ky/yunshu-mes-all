package com.yunshu.mes.cal.compat.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

class CalMutationAuthorizationInterceptorTest {

    private final CalMutationAuthorizationInterceptor interceptor =
            new CalMutationAuthorizationInterceptor();

    @ParameterizedTest
    @ValueSource(strings = {"MANAGER", "PROD_SUPERVISOR", "TESTER"})
    void allowsAuthorizedCalendarWriters(String role) {
        HttpServletRequest request = request("POST", List.of(role));

        assertDoesNotThrow(() -> interceptor.preHandle(
                request, mock(HttpServletResponse.class), new Object()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void rejectsEveryMutationMethodForOtherRoles(String method) {
        HttpServletRequest request = request(method, List.of("LINE_OPERATOR"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> interceptor.preHandle(request, mock(HttpServletResponse.class), new Object()));

        assertEquals(403, error.getStatusCode().value());
    }

    @Test
    void preservesCalendarReadsRegardlessOfRole() {
        for (String method : Stream.of("GET", "HEAD", "OPTIONS").toList()) {
            assertDoesNotThrow(() -> interceptor.preHandle(
                    request(method, List.of("LINE_OPERATOR")),
                    mock(HttpServletResponse.class), new Object()));
        }
    }

    private HttpServletRequest request(String method, Object roles) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getAttribute("currentRoles")).thenReturn(roles);
        return request;
    }
}
