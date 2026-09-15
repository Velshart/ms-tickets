package me.mmtr.userservice.configuration.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.mmtr.userservice.model.Role;
import me.mmtr.userservice.model.User;
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
    private static final String X_USER_ID_HEADER = "X-User-Id";
    private static final String X_USER_NAME_HEADER = "X-User-Name";
    private static final String X_USER_EMAIL_HEADER = "X-User-Email";
    private static final String X_USER_ROLE_HEADER = "X-User-Role";

    private static final String USER_ID_STR = "100";
    private static final String USERNAME = "test_user";
    private static final String EMAIL = "test@example.com";
    private static final String USER_ROLE = "ROLE_USER";

    @Mock
    private UserDetailsService userDetailsService;

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
    void shouldNotSetAuthenticationWhenUserIsAlreadyAuthenticated() throws ServletException, IOException {
        when(request.getHeader(X_USER_ID_HEADER)).thenReturn(USER_ID_STR);
        when(request.getHeader(X_USER_NAME_HEADER)).thenReturn(USERNAME);
        when(request.getHeader(X_USER_EMAIL_HEADER)).thenReturn(EMAIL);
        when(request.getHeader(X_USER_ROLE_HEADER)).thenReturn(USER_ROLE);

        UsernamePasswordAuthenticationToken existingAuth =
                new UsernamePasswordAuthenticationToken(USERNAME, null, null);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertEquals(existingAuth, SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(userDetailsService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSetAuthenticationWhenAllHeadersAreValid() throws ServletException, IOException {
        when(request.getHeader(X_USER_ID_HEADER)).thenReturn(USER_ID_STR);
        when(request.getHeader(X_USER_NAME_HEADER)).thenReturn(USERNAME);
        when(request.getHeader(X_USER_EMAIL_HEADER)).thenReturn(EMAIL);
        when(request.getHeader(X_USER_ROLE_HEADER)).thenReturn(USER_ROLE);

        UserDetails user = new User(
                USERNAME,
                EMAIL,
                "password",
                Role.USER
        );
        when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(user);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication);
        assertEquals(user, authentication.getPrincipal());
        assertTrue(
                authentication
                        .getAuthorities()
                        .contains(new SimpleGrantedAuthority("ROLE_" + Role.USER))
        );
        verify(userDetailsService).loadUserByUsername(USERNAME);
        verify(filterChain).doFilter(request, response);
    }
}
