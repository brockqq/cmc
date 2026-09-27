package com.kata.backend.user;

import com.kata.backend.common.MaxUtf8Bytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "請輸入目前密碼")
        String currentPassword,

        @NotBlank(message = "請輸入新密碼")
        @Size(min = 8, message = "密碼至少需要 8 個字元")
        @MaxUtf8Bytes(value = 72, message = "密碼過長（上限 72 位元組，中文字每字佔 3 位元組）")
        String newPassword
) {
}
