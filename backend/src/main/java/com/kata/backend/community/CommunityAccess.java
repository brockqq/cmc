package com.kata.backend.community;

import com.kata.backend.common.ApiException;
import com.kata.backend.user.Role;
import com.kata.backend.user.User;
import com.kata.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Central authorization for anything scoped to a community. Future community features
 * (announcements, repair requests, ...) should call {@link #requireMember} or
 * {@link #requireManager} before touching community data.
 * <p>
 * Platform admins pass every check, even for communities they have not joined.
 */
@Component
@RequiredArgsConstructor
public class CommunityAccess {

    private final UserRepository userRepository;
    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository memberRepository;

    public record Context(User user, Community community, CommunityMember membership) {

        public boolean isPlatformAdmin() {
            return user.getRole() == Role.ADMIN;
        }

        public CommunityRole role() {
            return membership == null ? null : membership.getRole();
        }

        public boolean canManage() {
            return isPlatformAdmin() || role() == CommunityRole.MANAGER;
        }
    }

    /** Everything needed to filter lists across communities (announcements, events, ...). */
    public record Viewer(User user, Set<Long> memberOf, Set<Long> manages) {

        public boolean isPlatformAdmin() {
            return user.getRole() == Role.ADMIN;
        }

        /** {@code communityId == null} means platform-wide content. */
        public boolean canView(Long communityId) {
            return communityId == null || isPlatformAdmin() || memberOf.contains(communityId);
        }

        /** Platform-wide content is managed by platform admins only. */
        public boolean canManage(Long communityId) {
            return isPlatformAdmin() || (communityId != null && manages.contains(communityId));
        }
    }

    public Viewer viewer(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "使用者不存在"));
        Set<Long> memberOf = new HashSet<>();
        Set<Long> manages = new HashSet<>();
        for (CommunityMember m : memberRepository.findByUserIdWithCommunity(user.getId())) {
            memberOf.add(m.getCommunity().getId());
            if (m.getRole() == CommunityRole.MANAGER) {
                manages.add(m.getCommunity().getId());
            }
        }
        return new Viewer(user, memberOf, manages);
    }

    /**
     * To non-members a community looks exactly like one that does not exist (404), so community ids cannot be
     * probed. Members lacking a role get 403 from the checks below: they already know the community exists.
     */
    public Context requireMember(Long communityId, String username) {
        Context ctx = load(communityId, username);
        if (ctx.membership() == null && !ctx.isPlatformAdmin()) {
            throw notFound();
        }
        return ctx;
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "找不到此社區");
    }

    /** Requires one of the given community roles; platform admins always pass. */
    public Context requireAnyRole(Long communityId, String username, String deniedMessage, CommunityRole... roles) {
        Context ctx = requireMember(communityId, username);
        if (ctx.isPlatformAdmin() || (ctx.role() != null && Set.of(roles).contains(ctx.role()))) {
            return ctx;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, deniedMessage);
    }

    public Context requireManager(Long communityId, String username) {
        Context ctx = requireMember(communityId, username);
        if (!ctx.canManage()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "需要社區管理員權限");
        }
        return ctx;
    }

    private Context load(Long communityId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "使用者不存在"));
        Community community = communityRepository.findById(communityId).orElseThrow(CommunityAccess::notFound);
        CommunityMember membership = memberRepository.findByCommunityIdAndUserId(communityId, user.getId())
                .orElse(null);
        return new Context(user, community, membership);
    }
}
