package com.kata.backend.config;

import com.kata.backend.user.User;
import com.kata.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Turns a validated JWT into an authentication using the user's current state in the database:
 * the role is read fresh on every request (so role changes apply immediately), and tokens whose
 * version no longer matches (e.g. issued before a password change) are rejected.
 */
@Component
@RequiredArgsConstructor
public class UserJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    public static final String TOKEN_VERSION_CLAIM = "ver";

    private final UserRepository userRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Number version = jwt.getClaim(TOKEN_VERSION_CLAIM);
        User user = userRepository.findByUsername(jwt.getSubject())
                .filter(u -> version != null && version.intValue() == u.getTokenVersion())
                .orElseThrow(() -> new InvalidBearerTokenException("Token 已失效，請重新登入"));
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        return new JwtAuthenticationToken(jwt, authorities, user.getUsername());
    }
}
