package me.mmtr.ticketservice.configuration.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private static final String X_USER_ID_HEADER = "X-User-Id";
    private static final String X_USER_NAME_HEADER = "X-User-Name";
    private static final String X_USER_EMAIL_HEADER = "X-User-Email";
    private static final String X_USER_ROLE_HEADER = "X-User-Role";

    private static final String USER_ID_STR = "100";
    private static final Long USER_ID = 100L;
    private static final String USERNAME = "test";
    private static final String EMAIL = "test@example.com";
    private static final String USER_ROLE = "ROLE_USER";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSkipAuthenticationWhenUserIdHeaderIsInvalid() throws ServletException, IOException {
        when(request.getHeader(X_USER_ID_HEADER)).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkipAuthenticationWhenUsernameHeaderIsInvalid() throws ServletException, IOException {
        when(request.getHeader(X_USER_ID_HEADER)).thenReturn(USER_ID_STR);
        when(request.getHeader(X_USER_NAME_HEADER)).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkipAuthenticationWhenUserEmailHeaderIsInvalid() throws ServletException, IOException {
        when(request.getHeader(X_USER_ID_HEADER)).thenReturn(USER_ID_STR);
        when(request.getHeader(X_USER_NAME_HEADER)).thenReturn(USERNAME);
        when(request.getHeader(X_USER_EMAIL_HEADER)).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkipAuthenticationWhenUserRoleHeaderIsInvalid() throws ServletException, IOException {
        when(request.getHeader(X_USER_ID_HEADER)).thenReturn(USER_ID_STR);
        when(request.getHeader(X_USER_NAME_HEADER)).thenReturn(USERNAME);
        when(request.getHeader(X_USER_EMAIL_HEADER)).thenReturn(EMAIL);
        when(request.getHeader(X_USER_ROLE_HEADER)).thenReturn(null);


        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkipAuthenticationWhenUserIsAlreadyAuthenticated() throws ServletException, IOException {
        when(request.getHeader(X_USER_ID_HEADER)).thenReturn(USER_ID_STR);

        Authentication existingAuthentication = new UsernamePasswordAuthenticationToken(
                USERNAME, null
        );
        SecurityContextHolder.getContext().setAuthentication(existingAuthentication);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertEquals(existingAuthentication, SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAuthenticateUserWhenValidHeadersProvided() throws ServletException, IOException {
        when(request.getHeader(X_USER_ID_HEADER)).thenReturn(USER_ID_STR);
        when(request.getHeader(X_USER_NAME_HEADER)).thenReturn(USERNAME);
        when(request.getHeader(X_USER_EMAIL_HEADER)).thenReturn(EMAIL);
        when(request.getHeader(X_USER_ROLE_HEADER)).thenReturn(USER_ROLE);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertInstanceOf(AuthenticatedUser.class, authentication.getPrincipal());

        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();

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
}