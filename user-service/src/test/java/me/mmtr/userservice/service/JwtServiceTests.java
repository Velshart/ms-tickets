package me.mmtr.userservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import me.mmtr.userservice.model.Role;
import me.mmtr.userservice.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        user.setId(1L);
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
        void shouldAttachUsernameAndRoleClaimsToToken() {
            String token = jwtService.generateToken(user);

            Claims claims = parseToken(token);

            assertEquals(user.getUsername(), claims.get("username"));
            assertEquals("ROLE_" + Role.USER, claims.get("userRole"));
        }

        @Test
        void shouldSetCorrectExpirationTime() {
            String token = jwtService.generateToken(user);
            Claims claims = parseToken(token);

            assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
        }

        private Claims parseToken(String token) {
            SecretKey secretKey = Keys
                    .hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));

            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        }
    }
}
