package com.kata.backend.config;

import com.kata.backend.user.Role;
import com.kata.backend.user.User;
import com.kata.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Creates the initial administrator account if it does not exist yet. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String username;

    @Value("${app.admin.email}")
    private String email;

    @Value("${app.admin.password}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("尚未建立平台管理員「" + username + "」：請以環境變數 APP_ADMIN_PASSWORD 設定初始密碼");
        }
        if (password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("APP_ADMIN_PASSWORD 需為至少 8 字元、最多 72 位元組");
        }
        User admin = new User();
        admin.setUsername(username);
        admin.setEmail(email.trim().toLowerCase(Locale.ROOT));
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        log.info("Created initial admin account '{}'", username);
    }
}
