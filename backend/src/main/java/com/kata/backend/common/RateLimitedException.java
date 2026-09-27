package com.kata.backend.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.Duration;

/** 429 Too Many Requests; the handler adds a Retry-After header. */
@Getter
public class RateLimitedException extends ApiException {

    private final long retryAfterSeconds;

    public RateLimitedException(Duration retryAfter) {
        super(HttpStatus.TOO_MANY_REQUESTS, "嘗試次數過多，請 " + minutes(retryAfter) + " 分鐘後再試");
        this.retryAfterSeconds = Math.max(1, (retryAfter.toMillis() + 999) / 1000);
    }

    private static long minutes(Duration d) {
        return Math.max(1, (d.toSeconds() + 59) / 60);
    }
}
