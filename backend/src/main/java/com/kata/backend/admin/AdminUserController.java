package com.kata.backend.admin;

import com.kata.backend.user.UserResponse;
import com.kata.backend.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Access restricted to ROLE_ADMIN in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public List<UserResponse> list() {
        return userService.listAll();
    }

    @PutMapping("/{id}/role")
    public UserResponse changeRole(@PathVariable Long id,
                                   @Valid @RequestBody UpdateRoleRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        return userService.changeRole(id, request.role(), jwt.getSubject());
    }
}
