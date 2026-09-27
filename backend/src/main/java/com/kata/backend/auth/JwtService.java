package com.kata.backend.auth;

import com.kata.backend.config.UserJwtAuthenticationConverter;
import com.kata.backend.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final Duration expiration;

    public JwtService(JwtEncoder jwtEncoder, @Value("${app.jwt.expiration}") Duration expiration) {
        this.jwtEncoder = jwtEncoder;
        this.expiration = expiration;
    }

    public String issueToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("kata-backend")
                .issuedAt(now)
                .expiresAt(now.plus(expiration))
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim(UserJwtAuthenticationConverter.TOKEN_VERSION_CLAIM, user.getTokenVersion())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long expiresInSeconds() {
        return expiration.toSeconds();
    }
}
