package com.kata.backend.auth;

import com.kata.backend.common.RateLimitedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slows down password guessing and mass registration, with sliding windows kept in memory (per server instance).
 * <p>
 * Failed logins are counted three ways, so that one attacker cannot lock a user out from everywhere, yet a
 * distributed attack on one account still hits a ceiling:
 * <ul>
 *   <li>per account and IP (small limit: someone guessing one account)</li>
 *   <li>per IP (someone trying many accounts)</li>
 *   <li>per account from all IPs (large limit: a botnet on one account)</li>
 * </ul>
 * A successful login clears that account's count for the IP it came from. Behind a reverse proxy, set
 * {@code server.forward-headers-strategy} so the client IP is the real one rather than the proxy's.
 */
@Component
public class AuthRateLimiter {

    private static final int MAX_TRACKED_KEYS = 10_000;

    private final Duration loginWindow;
    private final int perAccountAndIp;
    private final int perIp;
    private final int perAccount;
    private final Duration registrationWindow;
    private final int registrationsPerIp;
    private final Clock clock;

    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

    @Autowired
    public AuthRateLimiter(@Value("${app.rate-limit.login-window}") Duration loginWindow,
                           @Value("${app.rate-limit.login-failures-per-account-and-ip}") int perAccountAndIp,
                           @Value("${app.rate-limit.login-failures-per-ip}") int perIp,
                           @Value("${app.rate-limit.login-failures-per-account}") int perAccount,
                           @Value("${app.rate-limit.registration-window}") Duration registrationWindow,
                           @Value("${app.rate-limit.registrations-per-ip}") int registrationsPerIp) {
        this(loginWindow, perAccountAndIp, perIp, perAccount, registrationWindow, registrationsPerIp,
                Clock.systemUTC());
    }

    AuthRateLimiter(Duration loginWindow, int perAccountAndIp, int perIp, int perAccount,
                    Duration registrationWindow, int registrationsPerIp, Clock clock) {
        this.loginWindow = loginWindow;
        this.perAccountAndIp = perAccountAndIp;
        this.perIp = perIp;
        this.perAccount = perAccount;
        this.registrationWindow = registrationWindow;
        this.registrationsPerIp = registrationsPerIp;
        this.clock = clock;
    }

    /** Throws before the password is even checked when this login is over a limit. */
    public void checkLogin(String ip, String username) {
        String account = normalize(username);
        requireUnder("login:" + account + "@" + ip, perAccountAndIp, loginWindow);
        requireUnder("login-ip:" + ip, perIp, loginWindow);
        requireUnder("login-account:" + account, perAccount, loginWindow);
    }

    public void loginFailed(String ip, String username) {
        String account = normalize(username);
        record("login:" + account + "@" + ip);
        record("login-ip:" + ip);
        record("login-account:" + account);
    }

    public void loginSucceeded(String ip, String username) {
        hits.remove("login:" + normalize(username) + "@" + ip);
    }

    /** Counts a registration attempt and throws when the IP has made too many. */
    public void registrationAttempt(String ip) {
        String key = "register:" + ip;
        requireUnder(key, registrationsPerIp, registrationWindow);
        record(key);
    }

    private void requireUnder(String key, int limit, Duration window) {
        Deque<Instant> times = hits.get(key);
        if (times == null) {
            return;
        }
        Instant now = clock.instant();
        synchronized (times) {
            prune(times, now, window);
            if (times.size() >= limit) {
                Duration wait = Duration.between(now, times.peekFirst().plus(window));
                throw new RateLimitedException(wait);
            }
        }
    }

    private void record(String key) {
        if (hits.size() > MAX_TRACKED_KEYS) {
            evictExpired();
        }
        Deque<Instant> times = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (times) {
            times.addLast(clock.instant());
        }
    }

    /** Keeps memory bounded when many distinct IPs/accounts show up. */
    private void evictExpired() {
        Instant now = clock.instant();
        Duration longest = loginWindow.compareTo(registrationWindow) > 0 ? loginWindow : registrationWindow;
        hits.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                prune(e.getValue(), now, longest);
                return e.getValue().isEmpty();
            }
        });
    }

    private static void prune(Deque<Instant> times, Instant now, Duration window) {
        while (!times.isEmpty() && !times.peekFirst().plus(window).isAfter(now)) {
            times.pollFirst();
        }
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
