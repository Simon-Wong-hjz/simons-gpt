package com.simwong.simonsgpt.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtTokenService {

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Jwts.SIG.HS256.key().build();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Collection<? extends GrantedAuthority> extractAuthorities(String token) {
        return extractClaim(token, claims -> claims.get("authorities", Collection.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .claims().add("authorities", userDetails.getAuthorities()).and()
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public Boolean validateToken(String token) {
        final String username = extractUsername(token);
        return StringUtils.isNotBlank(username) && !isTokenExpired(token);
    }

    public Authentication getAuthentication(String token) {
        return new UsernamePasswordAuthenticationToken(extractUsername(token), token, extractAuthorities(token));
    }
}
