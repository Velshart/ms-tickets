package me.mmtr.ticketservice;

import jakarta.transaction.Transactional;
import me.mmtr.ticketservice.configuration.filter.RateLimitingFilter;
import me.mmtr.ticketservice.model.authentication.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
public abstract class TicketServiceModuleIntegrationTest {
    @ServiceConnection
    protected static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine");

    @ServiceConnection
    @SuppressWarnings("resource")
    protected static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);
    @Autowired
    protected RateLimitingFilter rateLimitingFilter;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected MockMvc mockMvc;

    protected RequestPostProcessor withUser(Long userId, String role) {
        var authorities = List.of(new SimpleGrantedAuthority(role));

        String username = "user_" + userId;
        var principal = new AuthenticatedUser(
                userId,
                username,
                username + "@example.com",
                authorities
        );

        return request -> {
            var authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    authorities
            );
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            return securityContext(context).postProcessRequest(request);
        };
    }
}
