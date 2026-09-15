package me.mmtr.userservice.configuration;

import jakarta.servlet.http.HttpServletResponse;
import me.mmtr.userservice.configuration.filter.JwtAuthenticationFilter;
import me.mmtr.userservice.configuration.filter.RateLimitingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.ForwardedHeaderFilter;

@Configuration("usersServiceSecurityConfiguration")
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {
    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter authenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;

    public SecurityConfiguration(
            UserDetailsService userDetailsService,
            JwtAuthenticationFilter authenticationFilter,
            RateLimitingFilter rateLimitingFilter
    ) {
        this.userDetailsService = userDetailsService;
        this.authenticationFilter = authenticationFilter;
        this.rateLimitingFilter = rateLimitingFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .securityMatcher("/api/auth/**", "/api/users/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/user/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"message\": \"Authentication required\"}"
                            );
                        }))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(
                        rateLimitingFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterBefore(
                        userServiceForwardedHeaderFilter(),
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterBefore(
                        authenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider =
                new DaoAuthenticationProvider(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());

        return daoAuthenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) {
        return config.getAuthenticationManager();
    }

    @Bean
    public ForwardedHeaderFilter userServiceForwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}