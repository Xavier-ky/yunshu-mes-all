package com.yunshu.mes.security.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.mes.common.response.ApiResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 鉴权过滤器：
 * <ul>
 *   <li>携带合法 Bearer 令牌时，将用户名与角色写入请求属性供下游使用。</li>
 *   <li>{@code mes.security.jwt.enforce=true} 时，未通过鉴权的 /api 请求返回 401；默认 false，仅校验不拦截。</li>
 *   <li>/api/auth/login、/api/health、上传文件路径（/api/common/files/…）始终放行。</li>
 * </ul>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String CLIENT_API_PREFIX = "/api/client/";

    private static final Set<String> PUBLIC_API_PATHS = Set.of("/api/auth/login", "/api/auth/face-login", "/api/health");
    private static final String UPLOAD_FILES_PREFIX = "/api/common/files/";

    private final JwtTokenService jwtTokenService;
    private final AuthProperties authProperties;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(
            JwtTokenService jwtTokenService,
            AuthProperties authProperties,
            ObjectMapper objectMapper) {
        this.jwtTokenService = jwtTokenService;
        this.authProperties = authProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (shouldBypassAuth(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length());
            try {
                Jws<Claims> parsed = jwtTokenService.parse(token);
                request.setAttribute("currentUser", parsed.getPayload().getSubject());
                request.setAttribute("currentUserId", parsed.getPayload().get("userId"));
                request.setAttribute("currentRoles", parsed.getPayload().get("roles"));
            } catch (Exception ignored) {
                // 令牌无效：不写入上下文，由 enforce 决定是否拦截
            }
        }

        if (authProperties.getJwt().isEnforce() && request.getAttribute("currentUser") == null) {
            writeUnauthorized(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static boolean shouldBypassAuth(String requestUri) {
        if (!requestUri.startsWith("/api/")) {
            return true;
        }
        if (PUBLIC_API_PATHS.contains(requestUri)) {
            return true;
        }
        if (requestUri.startsWith(CLIENT_API_PREFIX)) {
            return true;
        }
        return requestUri.startsWith(UPLOAD_FILES_PREFIX);
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Object traceId = request.getAttribute(ApiResponse.TRACE_ID_ATTRIBUTE);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<Void> body = ApiResponse.fail(
                "UNAUTHORIZED", "未登录或令牌无效", traceId == null ? "" : String.valueOf(traceId));
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
