package com.kata.backend.community;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CommunityMemberRepository extends JpaRepository<CommunityMember, Long> {

    Optional<CommunityMember> findByCommunityIdAndUserId(Long communityId, Long userId);

    boolean existsByCommunityIdAndUserId(Long communityId, Long userId);

    long countByCommunityId(Long communityId);

    @Query("select m from CommunityMember m join fetch m.community c where m.user.id = :userId order by c.name")
    List<CommunityMember> findByUserIdWithCommunity(Long userId);

    @Query("select m from CommunityMember m join fetch m.user where m.community.id = :communityId order by m.joinedAt")
    List<CommunityMember> findByCommunityIdWithUser(Long communityId);

    @Query("select m.community.id, count(m) from CommunityMember m group by m.community.id")
    List<Object[]> countMembersPerCommunity();
}
