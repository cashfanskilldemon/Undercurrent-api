package com.undercurrent.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

/**
 * 把 {@code Authorization: Bearer <token>} 解出来的用户 id 注入成 X-User-Id，
 * 这样所有 Controller 的 @RequestHeader("X-User-Id") 都不用改。
 *
 * <p>规则：
 * <ol>
 *   <li>带 token 且校验通过 → 以 token 里的用户为准；</li>
 *   <li>带 token 但签名错 / 过期 → 直接 401，让前端清掉本地凭证；</li>
 *   <li>没带 token → 是否信任手写的 X-User-Id 由 app.auth.allow-header-fallback 决定，
 *       默认 false（关闭），需要本地联调时再打开。</li>
 * </ol>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class TokenAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-User-Id";
    private static final String BEARER = "Bearer ";

    private final TokenService tokenService;

    @Value("${app.auth.allow-header-fallback:false}")
    private boolean allowHeaderFallback;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        if (authorization != null && authorization.startsWith(BEARER)) {
            Long userId = tokenService.verify(authorization.substring(BEARER.length()).trim());
            if (userId == null) {
                unauthorized(response);
                return;
            }
            chain.doFilter(new UserIdRequest(request, String.valueOf(userId)), response);
            return;
        }

        chain.doFilter(new UserIdRequest(request, allowHeaderFallback ? null : ""), response);
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"登录已过期，请重新登录\"}");
    }

    /**
     * 包装请求，改写 X-User-Id：
     * override 为 null 表示沿用原请求头；为空串表示不信任原请求头，直接返回 null。
     */
    private static class UserIdRequest extends HttpServletRequestWrapper {

        private final String override;

        UserIdRequest(HttpServletRequest request, String override) {
            super(request);
            this.override = override;
        }

        @Override
        public String getHeader(String name) {
            if (HEADER.equalsIgnoreCase(name)) {
                if (override == null) {
                    return super.getHeader(name);
                }
                return override.isEmpty() ? null : override;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (HEADER.equalsIgnoreCase(name)) {
                String value = getHeader(name);
                return value == null
                        ? Collections.emptyEnumeration()
                        : Collections.enumeration(Collections.singletonList(value));
            }
            return super.getHeaders(name);
        }
    }
}
