package com.kata.backend.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Keeps the spelling it was registered with; see {@link #usernameKey} for uniqueness. */
    @Column(nullable = false, length = 50)
    private String username;

    /**
     * Lower-cased username. The unique constraint on it makes "Bob" and "bob" the same account even when two
     * registrations race past the service's check.
     */
    @Column(name = "username_key", nullable = false, unique = true, length = 50)
    @Setter(AccessLevel.NONE)
    private String usernameKey;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    /** Embedded in issued JWTs; incrementing it invalidates all previously issued tokens. */
    @Column(nullable = false)
    private int tokenVersion;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        syncUsernameKey();
    }

    @PreUpdate
    void syncUsernameKey() {
        usernameKey = username.toLowerCase(Locale.ROOT);
    }
}
