package me.mmtr.userservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import me.mmtr.common.jwt.JwtParser;
import me.mmtr.userservice.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {
    private final long jwtExpiration;

    private final JwtParser jwtParser;

    public JwtService(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration}") long jwtExpiration
    ) {
        this.jwtExpiration = jwtExpiration;
        this.jwtParser = new JwtParser(jwtSecret);
    }

    public String generateToken(User user) {
        String userRole = user.getAuthorities()
                .stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElseThrow(() -> new IllegalStateException("User has no authorities"));

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("userRole", userRole)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(jwtParser.getSigningKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return jwtParser.parse(token);
    }
}