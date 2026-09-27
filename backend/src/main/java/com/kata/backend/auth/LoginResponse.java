package com.kata.backend.auth;

import com.kata.backend.user.UserResponse;

public record LoginResponse(String token, long expiresIn, UserResponse user) {
}
