package com.kata.backend.admin;

import com.kata.backend.user.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull(message = "請選擇角色") Role role) {
}
