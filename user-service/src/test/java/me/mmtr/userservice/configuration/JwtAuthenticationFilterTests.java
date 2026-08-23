package me.mmtr.userservice.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.mmtr.userservice.model.Role;
import me.mmtr.userservice.model.User;
import me.mmtr.userservice.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTests {
    private static final String USERNAME = "test_user";
    private static final String TOKEN = "valid.jwt.token";
    private static final String AUTHORIZATION_HEADER = "Bearer " + TOKEN;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtService jwtService;

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
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void shouldSkipAuthenticationWhenAuthorizationHeaderMissesBearerPrefix() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(TOKEN);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void shouldNotSetAuthenticationWhenUserIsAlreadyAuthenticated() throws ServletException, IOException {
        when(request.getHeader("Authorization"))
                .thenReturn(AUTHORIZATION_HEADER);

        when(jwtService.parseToken(TOKEN))
                .thenReturn(claims);
        when(claims.get("username", String.class)).thenReturn(USERNAME);

        UserDetails user =
                new User(USERNAME, "password", Role.USER);

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(USERNAME, null,
                        user.getAuthorities()
                );
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertEquals(
                authenticationToken,
                SecurityContextHolder.getContext().getAuthentication()
        );
        verifyNoMoreInteractions(jwtService, userDetailsService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotSetAuthenticationWhenTokenIsInvalid() throws ServletException, IOException {
        when(request.getHeader("Authorization"))
                .thenReturn(AUTHORIZATION_HEADER);

        when(jwtService.parseToken(TOKEN))
                .thenThrow(new JwtException("Invalid token"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService).parseToken(TOKEN);
        verifyNoInteractions(userDetailsService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotSetAuthenticationWhenTokenIsExpired() throws ServletException, IOException {
        when(request.getHeader("Authorization"))
                .thenReturn(AUTHORIZATION_HEADER);

        when(jwtService.parseToken(TOKEN))
                .thenThrow(new JwtException("JWT expired"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService).parseToken(TOKEN);
        verifyNoInteractions(userDetailsService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotSetAuthenticationWhenUsernameIsMissing() throws ServletException, IOException {
        when(request.getHeader("Authorization"))
                .thenReturn(AUTHORIZATION_HEADER);

        when(jwtService.parseToken(TOKEN)).thenReturn(claims);
        when(claims.get("username", String.class)).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService).parseToken(TOKEN);
        verifyNoInteractions(userDetailsService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSetAuthenticationWhenTokenIsValid() throws ServletException, IOException {
        when(request.getHeader("Authorization"))
                .thenReturn(AUTHORIZATION_HEADER);

        when(jwtService.parseToken(TOKEN)).thenReturn(claims);
        when(claims.get("username", String.class)).thenReturn(USERNAME);

        UserDetails user =
                new User(USERNAME, "password", Role.USER);
        when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(user);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication);
        assertEquals(user, authentication.getPrincipal());
        assertTrue(
                authentication
                        .getAuthorities()
                        .contains(new SimpleGrantedAuthority("ROLE_" + Role.USER))
        );
        verify(jwtService).parseToken(TOKEN);
        verify(userDetailsService).loadUserByUsername(USERNAME);
        verify(filterChain).doFilter(request, response);
    }
}
