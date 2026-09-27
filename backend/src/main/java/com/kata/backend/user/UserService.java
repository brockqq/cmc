package com.kata.backend.user;

import com.kata.backend.common.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getByUsername(String username) {
        return UserResponse.from(findByUsername(username));
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = findByUsername(username);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "目前密碼不正確");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "新密碼不可與目前密碼相同");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        // Revoke every token issued before the change (including stolen ones)
        user.setTokenVersion(user.getTokenVersion() + 1);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listAll() {
        return userRepository.findAll().stream()
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse changeRole(Long userId, Role role, String operatorUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "找不到此會員"));
        if (user.getUsername().equals(operatorUsername)) {
            // Prevents an admin from accidentally locking themselves out
            throw new ApiException(HttpStatus.BAD_REQUEST, "不能變更自己的角色");
        }
        user.setRole(role);
        return UserResponse.from(user);
    }

    private User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "使用者不存在"));
    }
}
