package me.mmtr.ticketservice.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.mmtr.common.jwt.JwtParser;
import me.mmtr.ticketservice.model.authentication.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTests {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String VALID_JWT_TOKEN = "valid.jwt.token";

    private static final String USER_ID_STR = "100";
    private static final Long USER_ID = 100L;
    private static final String USERNAME = "test";
    private static final String EMAIL = "test@example.com";
    private static final String USER_ROLE = "ROLE_USER";

    @Mock
    private JwtParser jwtParser;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Claims claims;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSkipAuthenticationWhenAuthorizationHeaderIsMissing() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtParser);
    }

    @Test
    void shouldSkipAuthenticationWhenAuthorizationHeaderIsMissingBearerPrefix() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Not Bearer");
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtParser);
    }

    @Test
    void shouldSkipAuthenticationWhenUserIsAlreadyAuthenticated() throws ServletException, IOException {
        when(request.getHeader("Authorization"))
                .thenReturn(BEARER_PREFIX + VALID_JWT_TOKEN);
        when(jwtParser.parse(VALID_JWT_TOKEN)).thenReturn(claims);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                USERNAME, null
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertEquals(
                authentication,
                SecurityContextHolder.getContext().getAuthentication()
        );
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAuthenticateUserWhenValidJwtProvided() throws ServletException, IOException {
        when(request.getHeader("Authorization"))
                .thenReturn(BEARER_PREFIX + VALID_JWT_TOKEN);
        when(jwtParser.parse(VALID_JWT_TOKEN)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(USER_ID_STR);
        when(claims.get("username", String.class)).thenReturn(USERNAME);
        when(claims.get("email", String.class)).thenReturn(EMAIL);
        when(claims.get("userRole", String.class)).thenReturn(USER_ROLE);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertInstanceOf(AuthenticatedUser.class, authentication.getPrincipal());

        AuthenticatedUser principal =
                (AuthenticatedUser) authentication.getPrincipal();

        assertEquals(USER_ID, principal.id());
        assertEquals(USERNAME, principal.username());
        assertEquals(EMAIL, principal.email());

        assertEquals(1, authentication.getAuthorities().size());
        assertEquals(
                USER_ROLE,
                authentication.getAuthorities().iterator().next().getAuthority()
        );
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkipAuthenticationWhenJwtExceptionWasThrown() throws ServletException, IOException {
        when(request.getHeader("Authorization"))
                .thenReturn(BEARER_PREFIX + "invalid.jwt.token");
        when(jwtParser.parse("invalid.jwt.token"))
                .thenThrow(new JwtException("Invalid JWT token"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldContinueFilterChainWhenJwtParsingThrowsIllegalArgumentException() throws ServletException, IOException {
        when(request.getHeader(AUTHORIZATION_HEADER))
                .thenReturn(BEARER_PREFIX + VALID_JWT_TOKEN);
        when(jwtParser.parse(VALID_JWT_TOKEN))
                .thenThrow(new IllegalArgumentException("Token is empty or null"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
