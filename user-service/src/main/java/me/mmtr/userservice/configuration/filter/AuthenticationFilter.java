package me.mmtr.userservice.configuration.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component("usersServiceAuthenticationFilter")
public class AuthenticationFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;

    public AuthenticationFilter(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    public void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String username = request.getHeader("X-User-Name");
        String email = request.getHeader("X-User-Email");
        String userRole = request.getHeader("X-User-Role");

        if (areHeadersValid(userId, username, email, userRole) &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }

        filterChain.doFilter(request, response);
    }

    private boolean areHeadersValid(String userId, String username, String email, String role) {
        return isHeaderValid(userId) &&
                isHeaderValid(username) &&
                isHeaderValid(email) &&
                isHeaderValid(role);
    }

    private boolean isHeaderValid(String header) {
        return header != null && !header.isBlank();
    }
}