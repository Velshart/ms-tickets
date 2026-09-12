package me.mmtr.apigateway.security;

import io.jsonwebtoken.Claims;
import me.mmtr.common.jwt.JwtParser;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class GatewayAuthenticationFilter implements GlobalFilter, Ordered {
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/auth/login",
            "/api/auth/register"
    );
    private final JwtParser jwtParser;

    public GatewayAuthenticationFilter(JwtParser jwtParser) {
        this.jwtParser = jwtParser;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        String authorizationHeader =
                request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (
                authorizationHeader == null ||
                        !authorizationHeader.startsWith("Bearer ")
        ) {
            return onError(exchange);
        }

        String token = authorizationHeader.substring(7);

        if (!jwtParser.validateToken(token)) {
            return onError(exchange);
        }

        Claims claims = jwtParser.getClaims(token);
        String userId = claims.getSubject();
        String username = claims.get("username", String.class);
        String email = claims.get("email", String.class);
        String userRole = claims.get("userRole", String.class);

        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", userId != null ? userId : "")
                .header("X-User-Name", username != null ? username : "")
                .header("X-User-Email", email != null ? email : "")
                .header("X-User-Role", userRole != null ? userRole : "")
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private Mono<Void> onError(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
    }
}
