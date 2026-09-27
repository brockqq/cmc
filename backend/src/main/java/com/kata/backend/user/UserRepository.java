package com.kata.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Usernames keep the case they were registered with, but are unique and looked up ignoring case wherever a person
 * types one (login, registration, assigning a manager). Token subjects carry the stored spelling, so
 * {@link #findByUsername} is used for those. Emails are stored lower-cased.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);
}
