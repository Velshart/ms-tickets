package me.mmtr.ticketservice.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.mmtr.common.jwt.JwtParser;
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
    private final JwtParser jwtParser;

    public JwtAuthenticationFilter(JwtParser jwtParser) {
        this.jwtParser = jwtParser;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        if (
                authorizationHeader == null ||
                        !authorizationHeader.startsWith("Bearer ")
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = getJwtTokenFromAuthorizationHeader(authorizationHeader);
        try {
            Claims claims = jwtParser.parse(jwt);
            String userRole = claims.get("userRole", String.class);
            if (
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication() == null
            ) {

                var authenticatedUser = new AuthenticatedUser(
                        Long.valueOf(claims.getSubject()),
                        claims.get("username", String.class),
                        claims.get("email", String.class),
                        List.of(new SimpleGrantedAuthority(userRole))
                );

                var authentication =
                        new UsernamePasswordAuthenticationToken(
                                authenticatedUser,
                                null,
                                authenticatedUser.getAuthorities()
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException | IllegalArgumentException _) {
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtTokenFromAuthorizationHeader(String authorizationHeader) {
        final int jwtTokenBeginIndex = 7;
        return authorizationHeader.substring(jwtTokenBeginIndex);
    }
}
