package me.mmtr.apigateway;

import io.jsonwebtoken.Claims;
import me.mmtr.apigateway.security.GatewayAuthenticationFilter;
import me.mmtr.common.jwt.JwtParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GatewayAuthenticationFilterTests {
    private static final String TOKEN = "valid.jwt.token";
    private static final String BEARER_TOKEN = "Bearer " + TOKEN;

    @Mock
    private JwtParser jwtParser;

    @Mock
    private GatewayFilterChain filterChain;

    @Mock
    private Claims claims;

    private GatewayAuthenticationFilter authenticationFilter;

    @BeforeEach
    void setUp() {
        authenticationFilter = new GatewayAuthenticationFilter(jwtParser);
    }

    @Nested
    class FilterTests {
        @Test
        void shouldPassThroughWhenPathIsPublicLogin() {
            when(filterChain.filter(any(ServerWebExchange.class)))
                    .thenReturn(Mono.empty());

            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.post("/api/auth/login").build()
            );

            StepVerifier.create(authenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            verify(filterChain).filter(exchange);
            verifyNoInteractions(jwtParser);
        }

        @Test
        void shouldPassThroughWhenPathIsPublicRegister() {
            when(filterChain.filter(any(ServerWebExchange.class)))
                    .thenReturn(Mono.empty());

            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.post("/api/auth/register").build()
            );

            StepVerifier.create(authenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            verify(filterChain).filter(exchange);
            verifyNoInteractions(jwtParser);
        }

        @Test
        void shouldReturn401WhenAuthorizationHeaderMissesBearerPrefix() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/api/events")
                            .header(HttpHeaders.AUTHORIZATION, TOKEN)
                            .build()
            );

            StepVerifier.create(authenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            assertEquals(
                    HttpStatus.UNAUTHORIZED,
                    exchange.getResponse().getStatusCode()
            );
            verifyNoInteractions(filterChain, jwtParser);
        }

        @Test
        void shouldReturn401WhenTokenIsInvalid() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/api/events")
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN)
                            .build()
            );

            when(jwtParser.validateToken(TOKEN)).thenReturn(false);

            StepVerifier.create(authenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
            verify(jwtParser).validateToken(TOKEN);
            verifyNoInteractions(filterChain);
        }

        @Test
        void shouldAddUserHeadersAndFilterExchangeWhenTokenIsValid() {
            when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/api/protected-resource")
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN)
                            .build()
            );

            when(jwtParser.validateToken(TOKEN)).thenReturn(true);
            when(jwtParser.getClaims(TOKEN)).thenReturn(claims);
            when(claims.getSubject()).thenReturn("100");
            when(claims.get("username", String.class)).thenReturn("john_doe");
            when(claims.get("email", String.class)).thenReturn("john@example.com");
            when(claims.get("userRole", String.class)).thenReturn("ROLE_USER");

            StepVerifier.create(authenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.captor();
            verify(filterChain).filter(captor.capture());

            ServerHttpRequest modifiedRequest = captor.getValue().getRequest();
            assertEquals("100", modifiedRequest.getHeaders().getFirst("X-User-Id"));
            assertEquals("john_doe", modifiedRequest.getHeaders().getFirst("X-User-Name"));
            assertEquals("john@example.com", modifiedRequest.getHeaders().getFirst("X-User-Email"));
            assertEquals("ROLE_USER", modifiedRequest.getHeaders().getFirst("X-User-Role"));
        }

        @Test
        void shouldSetEmptyHeaderValuesWhenClaimsAreNull() {
            when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/api/protected-resource")
                            .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN)
                            .build()
            );

            when(jwtParser.validateToken(TOKEN)).thenReturn(true);
            when(jwtParser.getClaims(TOKEN)).thenReturn(claims);
            when(claims.getSubject()).thenReturn(null);
            when(claims.get("username", String.class)).thenReturn(null);
            when(claims.get("email", String.class)).thenReturn(null);
            when(claims.get("userRole", String.class)).thenReturn(null);

            StepVerifier.create(authenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
            verify(filterChain).filter(captor.capture());

            ServerHttpRequest modifiedRequest = captor.getValue().getRequest();
            assertEquals("", modifiedRequest.getHeaders().getFirst("X-User-Id"));
            assertEquals("", modifiedRequest.getHeaders().getFirst("X-User-Name"));
            assertEquals("", modifiedRequest.getHeaders().getFirst("X-User-Email"));
            assertEquals("", modifiedRequest.getHeaders().getFirst("X-User-Role"));
        }
    }

    @Test
    void shouldHaveHighestPriorityOrder() {
        assertEquals(-1, authenticationFilter.getOrder());
    }
}
