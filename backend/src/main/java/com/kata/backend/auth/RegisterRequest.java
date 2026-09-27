package com.kata.backend.auth;

import com.kata.backend.common.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "請輸入帳號")
        @Size(min = 3, max = 50, message = "帳號長度需為 3-50 字元")
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "帳號只能包含英文字母、數字與底線")
        String username,

        @NotBlank(message = "請輸入 Email")
        @Email(message = "Email 格式不正確")
        @Size(max = 100, message = "Email 過長")
        String email,

        @NotBlank(message = "請輸入密碼")
        @Size(min = 8, message = "密碼至少需要 8 個字元")
        @MaxUtf8Bytes(value = 72, message = "密碼過長（上限 72 位元組，中文字每字佔 3 位元組）")
        String password,

        /** Optional; when present the new user joins that community as a resident. */
        String inviteCode
) {
}
