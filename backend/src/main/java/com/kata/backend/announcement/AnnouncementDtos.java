package com.kata.backend.announcement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AnnouncementDtos {

    private AnnouncementDtos() {
    }

    /** {@code communityId} null = platform-wide (platform admins only). */
    public record CreateRequest(
            Long communityId,
            @NotBlank(message = "請輸入標題") @Size(max = 200, message = "標題過長") String title,
            @NotBlank(message = "請輸入內容") @Size(max = 5000, message = "內容過長") String content,
            Boolean pinned
    ) {
    }

    /** The scope (community) of an announcement cannot be changed after posting. */
    public record UpdateRequest(
            @NotBlank(message = "請輸入標題") @Size(max = 200, message = "標題過長") String title,
            @NotBlank(message = "請輸入內容") @Size(max = 5000, message = "內容過長") String content,
            Boolean pinned
    ) {
    }

    public record AnnouncementResponse(Long id, Long communityId, String communityName, String title,
                                       String content, boolean pinned, String author,
                                       Instant createdAt, Instant updatedAt, boolean canEdit) {

        static AnnouncementResponse from(Announcement a, boolean canEdit) {
            var c = a.getCommunity();
            return new AnnouncementResponse(a.getId(), c == null ? null : c.getId(), c == null ? null : c.getName(),
                    a.getTitle(), a.getContent(), a.isPinned(), a.getAuthor().getUsername(),
                    a.getCreatedAt(), a.getUpdatedAt(), canEdit);
        }
    }
}
