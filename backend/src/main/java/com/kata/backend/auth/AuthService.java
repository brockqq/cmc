package com.kata.backend.auth;

import com.kata.backend.common.ApiException;
import com.kata.backend.community.CommunityService;
import com.kata.backend.user.User;
import com.kata.backend.user.UserRepository;
import com.kata.backend.user.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CommunityService communityService;
    private final AuthRateLimiter rateLimiter;

    private volatile String dummyHash;

    /** A hash of a random password, compared against when the username is unknown (same cost as a real check). */
    private String dummyHash() {
        if (dummyHash == null) {
            dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
        }
        return dummyHash;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "此帳號已被使用");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "此 Email 已被註冊");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);
        if (request.inviteCode() != null && !request.inviteCode().isBlank()) {
            // An invalid code throws and rolls back the whole registration
            communityService.addResident(user, request.inviteCode());
        }
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request, String ip) {
        rateLimiter.checkLogin(ip, request.username());
        User found = userRepository.findByUsernameIgnoreCase(request.username().trim()).orElse(null);
        // Hash even when the account doesn't exist, so response time doesn't reveal which usernames exist
        boolean matches = passwordEncoder.matches(request.password(),
                found != null ? found.getPassword() : dummyHash());
        User user = matches && found != null ? found : null;
        if (user == null) {
            rateLimiter.loginFailed(ip, request.username());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "帳號或密碼錯誤");
        }
        rateLimiter.loginSucceeded(ip, request.username());
        return new LoginResponse(jwtService.issueToken(user), jwtService.expiresInSeconds(), UserResponse.from(user));
    }
}
