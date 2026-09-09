package me.mmtr.ticketservice.configuration;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration("ticketsApiSecurityConfiguration")
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    private final JwtAuthenticationFilter authenticationFilter;

    public SecurityConfiguration(
            JwtAuthenticationFilter authenticationFilter
    ) {
        this.authenticationFilter = authenticationFilter;
    }

    @Bean
    public SecurityFilterChain ticketsApiSecurityFilterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .securityMatcher("/api/events/**", "/api/orders/**")
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((_, response, _) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"message\":" +
                                            " \"Authentication required\"}"
                            );
                        }))
                .sessionManagement(
                        session ->
                                session
                                        .sessionCreationPolicy(
                                                SessionCreationPolicy.STATELESS))
                .addFilterBefore(
                        authenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );
        return http.build();
    }
}