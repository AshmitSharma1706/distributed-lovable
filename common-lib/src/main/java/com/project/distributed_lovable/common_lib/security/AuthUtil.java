package com.project.distributed_lovable.common_lib.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;

@Component
public class AuthUtil {

    @Value("${jwt.secret-key:${jwt.secretkey:}}")
    private String jwtSecretKey;

    public SecretKey getSecretKey() {
        if (jwtSecretKey == null || jwtSecretKey.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret key is not configured. Please define 'jwt.secret-key' or 'jwt.secretkey'.");
        }
        return Keys.hmacShaKeyFor((jwtSecretKey.getBytes(StandardCharsets.UTF_8)));
    }

    public String generateAccessToken(JwtUserPrincipal user) {
        return Jwts.builder()
                .subject(user.username())
                .claim("userId", user.userId().toString())
                .claim("name", user.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 100))
                .signWith(getSecretKey())
                .compact();
    }

    public JwtUserPrincipal verifyAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Object rawUserId = claims.get("userId");
        Long userId;
        if (rawUserId instanceof Number num) {
            userId = num.longValue();
        } else if (rawUserId != null) {
            userId = Long.parseLong(rawUserId.toString());
        } else {
            throw new IllegalArgumentException("JWT missing required 'userId' claim");
        }

        String name = claims.get("name", String.class);
        String username = claims.getSubject();
        return new JwtUserPrincipal(userId, name, username, null, new ArrayList<>());
    }

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal userPrincipal)) {
            throw new AuthenticationCredentialsNotFoundException("No JWT found");
        }
        return userPrincipal.userId();
    }
}
