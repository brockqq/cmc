package com.kata.backend.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            UserJwtAuthenticationConverter jwtAuthenticationConverter,
                                            @Value("${spring.h2.console.enabled:false}") boolean h2Console)
            throws Exception {
        if (h2Console) { // dev profile only
            http
                    .authorizeHttpRequests(auth -> auth.requestMatchers(PathRequest.toH2Console()).permitAll())
                    // H2 console is rendered in frames
                    .headers(h -> h.frameOptions(f -> f.sameOrigin()));
        }
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/error").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    @Bean
    SecretKey jwtSecretKey(@Value("${app.jwt.secret}") String secret) {
        return new SecretKeySpec(decodeJwtSecret(secret), "HmacSHA256");
    }

    /** Refuses to start without a usable secret: a guessable key lets anyone forge an admin token. */
    static byte[] decodeJwtSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("未設定 JWT 密鑰：請以環境變數 APP_JWT_SECRET 提供 Base64 編碼、至少 256 bits 的隨機值"
                    + "（例：openssl rand -base64 32）");
        }
        byte[] key;
        try {
            key = Base64.getDecoder().decode(secret.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("APP_JWT_SECRET 不是有效的 Base64 字串", e);
        }
        if (key.length < 32) {
            throw new IllegalStateException(
                    "APP_JWT_SECRET 太短：HS256 需要至少 256 bits（32 位元組），目前只有 " + key.length + " 位元組");
        }
        return key;
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder() {
            // BCrypt rejects input over 72 bytes by throwing; such a password can never have been stored,
            // so treat it as a mismatch instead of a server error (login, current-password checks)
            @Override
            protected boolean matchesNonNull(String rawPassword, String encodedPassword) {
                if (rawPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
                    return false;
                }
                return super.matchesNonNull(rawPassword, encodedPassword);
            }
        };
    }
}
