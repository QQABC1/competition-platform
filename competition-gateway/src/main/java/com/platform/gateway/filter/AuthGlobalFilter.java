package com.platform.gateway.filter;

import com.platform.common.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 全局认证过滤器
 * 作用：校验请求头中的 Token
 */
@Component
@Slf4j
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    // 白名单：不需要Token就能访问的接口
    private static final List<String> SKIP_URLS = Arrays.asList(
            "/api/user/auth/send-code", // 发送验证码
            "/api/user/auth/login",     // 登录
            "/doc.html",                // Knife4j文档
            "/webjars/**",              // 静态资源
            "/v3/api-docs/**"           // Swagger资源
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 白名单放行
        for (String skipUrl : SKIP_URLS) {
            if (pathMatcher.match(skipUrl, path)) {
                return chain.filter(exchange);
            }
        }

        // 2. 获取 Token
        String token = request.getHeaders().getFirst("Authorization");

        // 3. 校验 Token
        Long userId = null;
        try {
            // 如果 Common 模块的 JwtUtils 抛异常，说明 Token 无效
            if (token == null || token.isEmpty()) {
                throw new RuntimeException("Token为空");
            }
            userId = JwtUtils.getUserId(token);
        } catch (Exception e) {
            log.warn("拦截请求: {}, 原因: Token无效", path);
            return unauthorizedResponse(exchange, "未授权，请先登录");
        }

        // 4. Token 有效，将 UserId 传递给下游微服务
        // 这样下游微服务在 Controller 里可以通过 request.getHeader("Service-User-Id") 拿到当前是谁
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("Service-User-Id", userId.toString())
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    // 返回 401 错误 JSON
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");

        String json = "{\"code\": 401, \"msg\": \"" + msg + "\", \"data\": null}";
        DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return 0; // 优先级，越小越先执行
    }
}