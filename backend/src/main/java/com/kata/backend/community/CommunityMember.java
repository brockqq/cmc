package com.kata.backend.community;

import com.kata.backend.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "community_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"community_id", "user_id"}),
        indexes = @Index(columnList = "user_id")) // "my communities"; community_id is covered by the unique key
@Getter
@Setter
@NoArgsConstructor
public class CommunityMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_id")
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommunityRole role;

    @Column(nullable = false, updatable = false)
    private Instant joinedAt;

    public CommunityMember(Community community, User user, CommunityRole role) {
        this.community = community;
        this.user = user;
        this.role = role;
    }

    @PrePersist
    void onCreate() {
        joinedAt = Instant.now();
    }
}
