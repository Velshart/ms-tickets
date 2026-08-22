package me.mmtr.userservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import me.mmtr.userservice.model.Role;
import me.mmtr.userservice.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTests {
    private static final long ONE_HOUR_IN_MILLIS = 3600000;
    private static final String TEST_SECRET =
            "NDA0RTYzNUY2QTU4NkU1QzY2MzM4RDQ5MzQ1NDMxMzMzMzc4MzMzMzM=";
    private User user;
    private JwtService jwtService;

    @BeforeEach
    public void setUp() {
        jwtService = new JwtService(TEST_SECRET, ONE_HOUR_IN_MILLIS);

        user = new User(
                "testUser",
                "password",
                Role.USER
        );
    }

    @Nested
    class GenerateTokenTests {
        @Test
        void shouldGenerateNonEmptyToken() {
            String token = jwtService.generateToken(user);

            assertThat(token).isNotBlank();
            assertEquals(3, token.split("\\.").length);
        }

        @Test
        void shouldAttachIdAndRoleClaimsToToken() {
            String token = jwtService.generateToken(user);

            SecretKey secretKey = Keys
                    .hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));

            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            assertEquals(user.getId(), claims.get("userId"));
            assertEquals("ROLE_" + Role.USER, claims.get("userRole"));
        }
    }

    @Nested
    class ExtractUsernameTests {
        @Test
        void shouldCorrectlyExtractUsername() {
            String token = jwtService.generateToken(user);

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
            String token = jwtService.generateToken(user);

            boolean valid = jwtService.isTokenValid(token, user);
            assertTrue(valid);
        }

        @Test
        void shouldReturnFalseForDifferentUser() {
            String token = jwtService.generateToken(user);
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
            String token = jwtService.generateToken(user);

            boolean valid = jwtService.isTokenValid(token, user);
            assertFalse(valid);
        }

        @Test
        void shouldReturnFalseForMalformedToken() {
            String malformedToken =
                    "not.a.valid.token";

            boolean valid = jwtService.isTokenValid(malformedToken, user);
            assertFalse(valid);
        }
    }
}
