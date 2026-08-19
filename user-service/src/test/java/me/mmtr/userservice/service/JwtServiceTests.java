package me.mmtr.userservice.service;

import io.jsonwebtoken.JwtException;
import me.mmtr.userservice.model.Role;
import me.mmtr.userservice.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTests {
    private static final long ONE_HOUR_IN_MILLIS = 3600000;
    private final String TEST_SECRET =
            "NDA0RTYzNUY2QTU4NkU1QzY2MzM4RDQ5MzQ1NDMxMzMzMzc4MzMzMzM=";
    private UserDetails userDetails;
    private JwtService jwtService;

    @BeforeEach
    public void setUp() {
        jwtService = new JwtService(TEST_SECRET, ONE_HOUR_IN_MILLIS);

        userDetails = new User(
                "testUser",
                "password",
                Role.USER
        );
    }

    @Nested
    class GenerateTokenTests {
        @Test
        void shouldGenerateNonEmptyToken() {
            String token = jwtService.generateToken(userDetails);

            assertThat(token).isNotBlank();
            assertEquals(3, token.split("\\.").length);
        }
    }

    @Nested
    class ExtractUsernameTests {
        @Test
        void shouldCorrectlyExtractUsername() {
            String token = jwtService.generateToken(userDetails);

            String username = jwtService.extractUsername(token);
            assertEquals("testUser", username);
        }

        @Test
        void shouldThrowExceptionForMalformedToken() {
            String token = "not.a.valid.token";

            assertThrows(
                    JwtException.class,
                    () -> jwtService.extractUsername(token)
            );
        }
    }

    @Nested
    class IsTokenValidTests {
        @Test
        void shouldReturnTrueIfTokenIsValid() {
            String token = jwtService.generateToken(userDetails);

            boolean valid = jwtService.isTokenValid(token, userDetails);
            assertTrue(valid);
        }

        @Test
        void shouldReturnFalseForDifferentUser() {
            String token = jwtService.generateToken(userDetails);
            UserDetails anotherUserDetails = new User(
                    "anotherUser",
                    "password",
                    Role.USER
            );

            boolean valid = jwtService.isTokenValid(token, anotherUserDetails);
            assertFalse(valid);
        }

        @Test
        void shouldReturnFalseForExpiredToken() {
            jwtService = new JwtService(TEST_SECRET, -ONE_HOUR_IN_MILLIS);
            String token = jwtService.generateToken(userDetails);

            boolean valid = jwtService.isTokenValid(token, userDetails);
            assertFalse(valid);
        }

        @Test
        void shouldReturnFalseForMalformedToken() {
            String malformedToken =
                    "not.a.valid.token";

            boolean valid = jwtService.isTokenValid(malformedToken, userDetails);
            assertFalse(valid);
        }
    }
}
