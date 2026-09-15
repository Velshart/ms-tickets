package me.mmtr.ticketservice.configuration.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.mmtr.ticketservice.model.authentication.AuthenticatedUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component("ticketsApiJwtAuthenticationFilter")
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    public void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String userIdHeader = request.getHeader("X-User-Id");
        String username = request.getHeader("X-User-Name");
        String email = request.getHeader("X-User-Email");
        String userRole = request.getHeader("X-User-Role");

        if (areHeadersValid(userIdHeader, username, email, userRole) &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            Long userId = Long.valueOf(userIdHeader);

            var authenticatedUser = new AuthenticatedUser(
                    userId,
                    username,
                    email,
                    List.of(new SimpleGrantedAuthority(userRole))
            );

            var authentication = new UsernamePasswordAuthenticationToken(
                    authenticatedUser,
                    null,
                    authenticatedUser.getAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
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