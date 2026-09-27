package com.kata.backend.community.dto;

import com.kata.backend.community.CommunityMember;
import com.kata.backend.community.CommunityRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Request/response payloads for the community APIs. */
public final class CommunityDtos {

    private CommunityDtos() {
    }

    /** One entry in "my communities". */
    public record MyCommunity(Long id, String name, String description, CommunityRole role) {

        public static MyCommunity from(CommunityMember m) {
            return new MyCommunity(m.getCommunity().getId(), m.getCommunity().getName(),
                    m.getCommunity().getDescription(), m.getRole());
        }
    }

    /**
     * Community detail as seen by the caller. {@code myRole} is null for a platform admin who has not joined;
     * {@code inviteCode} is only present when the caller can manage the community.
     */
    public record CommunityDetail(Long id, String name, String description, CommunityRole myRole,
                                  boolean canManage, long memberCount, String inviteCode) {
    }

    public record Member(Long userId, String username, String email, CommunityRole role, Instant joinedAt) {

        public static Member from(CommunityMember m) {
            return new Member(m.getUser().getId(), m.getUser().getUsername(), m.getUser().getEmail(),
                    m.getRole(), m.getJoinedAt());
        }
    }

    public record AdminCommunity(Long id, String name, String description, String inviteCode,
                                 long memberCount, Instant createdAt) {
    }

    public record JoinRequest(@NotBlank(message = "請輸入邀請碼") String inviteCode) {
    }

    public record UpdateMemberRoleRequest(@NotNull(message = "請選擇角色") CommunityRole role) {
    }

    public record InviteCodeResponse(String inviteCode) {
    }

    public record CreateCommunityRequest(
            @NotBlank(message = "請輸入社區名稱") @Size(max = 100, message = "社區名稱過長") String name,
            @Size(max = 500, message = "描述過長") String description
    ) {
    }

    public record AssignManagerRequest(@NotBlank(message = "請輸入帳號") String username) {
    }
}
