package me.mmtr.userservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import me.mmtr.common.JwtParser;
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
                .claim("userRole", userRole)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(jwtParser.getSigningKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return jwtParser.parse(token);
    }

//    public String extractUsername(String token) {
//        return extractClaim(token, claims -> claims.get("username").toString());
//    }

//    public boolean isTokenValid(String token, UserDetails userDetails) {
//        try {
//            String username = extractUsername(token);
//            return (username.equals(userDetails.getUsername())) &&
//                    !isTokenExpired(token);
//        } catch (JwtException e) {
//            return false;
//        }
//    }

//    public boolean isTokenValid(String token, UserDetails userDetails) {
//        try {
//            Claims claims = parseToken(token);
//            String username = claims.get("username").toString();
//
//            return username.equals(userDetails.getUsername());
//        } catch (Exception e) {
//            return false;
//        }
//    }

//    private boolean isTokenExpired(String token) {
//        return extractClaim(token, Claims::getExpiration).before(new Date());
//    }

//    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
//        Claims claims = Jwts.parser()
//                .verifyWith(getSigningKey())
//                .build()
//                .parseSignedClaims(token)
//                .getPayload();
//
//        return claimsResolver.apply(claims);
//    }

//    private SecretKey getSigningKey() {
//        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
//        return Keys.hmacShaKeyFor(keyBytes);
//    }
}