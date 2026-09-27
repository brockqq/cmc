package com.kata.backend.announcement;

import com.kata.backend.announcement.AnnouncementDtos.*;
import com.kata.backend.common.ApiException;
import com.kata.backend.common.PageResponse;
import com.kata.backend.community.Community;
import com.kata.backend.community.CommunityAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final CommunityAccess access;

    public static final int MAX_PAGE_SIZE = 100;

    // Pinned first, then newest; id breaks ties so paging is stable
    private static final Sort ORDER = Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    /**
     * One page of announcements visible to the user:
     * <ul>
     *   <li>default: platform-wide plus every community the user belongs to</li>
     *   <li>{@code platformOnly}: platform-wide only</li>
     *   <li>{@code communityId}: only that community (membership required)</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    public PageResponse<AnnouncementResponse> list(String username, Long communityId, boolean platformOnly,
                                                   int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "page 需 ≥ 0，size 需介於 1 到 " + MAX_PAGE_SIZE);
        }
        if (communityId != null && platformOnly) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "communityId 與 platform 不可同時指定");
        }
        CommunityAccess.Viewer viewer = access.viewer(username);
        Pageable pageable = PageRequest.of(page, size, ORDER);
        Page<Announcement> result;
        if (communityId != null) {
            access.requireMember(communityId, username);
            result = announcementRepository.findByCommunity(communityId, pageable);
        } else if (platformOnly || viewer.memberOf().isEmpty()) {
            result = announcementRepository.findPlatformWide(pageable);
        } else {
            result = announcementRepository.findPlatformWideOrIn(viewer.memberOf(), pageable);
        }
        return PageResponse.from(result, a -> AnnouncementResponse.from(a, viewer.canManage(communityIdOf(a))));
    }

    @Transactional
    public AnnouncementResponse create(String username, CreateRequest request) {
        CommunityAccess.Viewer viewer = access.viewer(username);
        Community community = null;
        if (request.communityId() == null) {
            requirePlatformAdmin(viewer);
        } else {
            community = access.requireManager(request.communityId(), username).community();
        }
        Announcement a = new Announcement();
        a.setCommunity(community);
        a.setAuthor(viewer.user());
        apply(a, request.title(), request.content(), request.pinned());
        return AnnouncementResponse.from(announcementRepository.save(a), true);
    }

    @Transactional
    public AnnouncementResponse update(String username, Long id, UpdateRequest request) {
        Announcement a = findEditable(username, id);
        apply(a, request.title(), request.content(), request.pinned());
        announcementRepository.flush(); // populate updatedAt before mapping
        return AnnouncementResponse.from(a, true);
    }

    @Transactional
    public void delete(String username, Long id) {
        announcementRepository.delete(findEditable(username, id));
    }

    private Announcement findEditable(String username, Long id) {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "找不到此公告"));
        CommunityAccess.Viewer viewer = access.viewer(username);
        Long communityId = communityIdOf(a);
        if (!viewer.canView(communityId)) {
            // Don't reveal that announcements of other communities exist
            throw new ApiException(HttpStatus.NOT_FOUND, "找不到此公告");
        }
        if (!viewer.canManage(communityId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, communityId == null ? "只有平台管理員可以管理全平台公告" : "需要社區管理員權限");
        }
        return a;
    }

    private static void requirePlatformAdmin(CommunityAccess.Viewer viewer) {
        if (!viewer.isPlatformAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "只有平台管理員可以發布全平台公告");
        }
    }

    private static void apply(Announcement a, String title, String content, Boolean pinned) {
        a.setTitle(title.trim());
        a.setContent(content.trim());
        a.setPinned(Boolean.TRUE.equals(pinned));
    }

    private static Long communityIdOf(Announcement a) {
        return a.getCommunity() == null ? null : a.getCommunity().getId();
    }
}
