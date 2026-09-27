package com.kata.backend.community;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommunityRepository extends JpaRepository<Community, Long> {

    Optional<Community> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);

    boolean existsByName(String name);
}
