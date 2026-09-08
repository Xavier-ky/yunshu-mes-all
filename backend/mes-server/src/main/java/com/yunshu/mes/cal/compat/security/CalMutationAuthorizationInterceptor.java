package com.yunshu.mes.cal.compat.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class CalMutationAuthorizationInterceptor implements HandlerInterceptor {

    private static final Set<String> WRITE_ROLES =
            Set.of("MANAGER", "PROD_SUPERVISOR", "TESTER");

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response, @NonNull Object handler) {
        if (isReadMethod(request.getMethod())) {
            return true;
        }
        Object roles = request.getAttribute("currentRoles");
        if (roles instanceof Collection<?> collection
                && collection.stream().map(String::valueOf).anyMatch(WRITE_ROLES::contains)) {
            return true;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无日历维护权限");
    }

    private boolean isReadMethod(String method) {
        return HttpMethod.GET.matches(method)
                || HttpMethod.HEAD.matches(method)
                || HttpMethod.OPTIONS.matches(method);
    }
}
