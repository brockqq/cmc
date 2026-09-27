package com.kata.backend.auth;

import com.kata.backend.common.RateLimitedException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthRateLimiterTest {

    static final Duration WINDOW = Duration.ofMinutes(15);

    /** A clock the test moves by hand. */
    static class TestClock extends Clock {
        Instant now = Instant.parse("2026-09-27T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    final TestClock clock = new TestClock();
    final AuthRateLimiter limiter = new AuthRateLimiter(WINDOW, 3, 5, 8, Duration.ofHours(1), 2, clock);

    void fail(String ip, String user, int times) {
        for (int i = 0; i < times; i++) {
            limiter.checkLogin(ip, user);
            limiter.loginFailed(ip, user);
        }
    }

    @Test
    void blocksAnAccountFromOneIpAfterTooManyFailuresUntilTheWindowPasses() {
        fail("1.1.1.1", "alice", 3);
        assertThatThrownBy(() -> limiter.checkLogin("1.1.1.1", "ALICE")) // same account, any spelling
                .isInstanceOfSatisfying(RateLimitedException.class,
                        e -> assertThat(e.getRetryAfterSeconds()).isEqualTo(WINDOW.toSeconds()));
        // The real user on another IP is not locked out
        assertThatNoException().isThrownBy(() -> limiter.checkLogin("2.2.2.2", "alice"));

        clock.now = clock.now.plus(WINDOW);
        assertThatNoException().isThrownBy(() -> limiter.checkLogin("1.1.1.1", "alice"));
    }

    @Test
    void successClearsTheAccountCountForThatIp() {
        fail("1.1.1.1", "bob", 2);
        limiter.loginSucceeded("1.1.1.1", "bob");
        fail("1.1.1.1", "bob", 2);
        assertThatNoException().isThrownBy(() -> limiter.checkLogin("1.1.1.1", "bob"));
    }

    @Test
    void blocksAnIpTryingManyAccounts() {
        fail("3.3.3.3", "u1", 2);
        fail("3.3.3.3", "u2", 2);
        fail("3.3.3.3", "u3", 1);
        assertThatThrownBy(() -> limiter.checkLogin("3.3.3.3", "someone-new")).isInstanceOf(RateLimitedException.class);
    }

    @Test
    void capsFailuresOnOneAccountAcrossIps() {
        for (int i = 0; i < 8; i++) {
            fail("10.0.0." + i, "carol", 1);
        }
        assertThatThrownBy(() -> limiter.checkLogin("10.0.0.99", "carol")).isInstanceOf(RateLimitedException.class);
    }

    @Test
    void limitsRegistrationsPerIp() {
        limiter.registrationAttempt("4.4.4.4");
        limiter.registrationAttempt("4.4.4.4");
        assertThatThrownBy(() -> limiter.registrationAttempt("4.4.4.4")).isInstanceOf(RateLimitedException.class);
        assertThatNoException().isThrownBy(() -> limiter.registrationAttempt("5.5.5.5"));
    }
}
