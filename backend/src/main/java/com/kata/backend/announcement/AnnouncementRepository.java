package com.kata.backend.announcement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;

/** Sorting is supplied through the {@link Pageable}; count queries avoid fetch joins. */
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    @Query(value = "select a from Announcement a left join fetch a.community c join fetch a.author where c is null",
            countQuery = "select count(a) from Announcement a where a.community is null")
    Page<Announcement> findPlatformWide(Pageable pageable);

    @Query(value = "select a from Announcement a left join fetch a.community c join fetch a.author"
            + " where c is null or c.id in :communityIds",
            countQuery = "select count(a) from Announcement a left join a.community c"
                    + " where c is null or c.id in :communityIds")
    Page<Announcement> findPlatformWideOrIn(Collection<Long> communityIds, Pageable pageable);

    @Query(value = "select a from Announcement a join fetch a.community c join fetch a.author where c.id = :communityId",
            countQuery = "select count(a) from Announcement a where a.community.id = :communityId")
    Page<Announcement> findByCommunity(Long communityId, Pageable pageable);
}
