package com.kata.backend.community;

import com.kata.backend.common.ApiException;
import com.kata.backend.community.dto.CommunityDtos.*;
import com.kata.backend.user.User;
import com.kata.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final CommunityAccess access;
    private final InviteCodeGenerator inviteCodeGenerator;

    // ---- Resident / manager operations ----

    @Transactional(readOnly = true)
    public List<MyCommunity> myCommunities(String username) {
        return memberRepository.findByUserIdWithCommunity(findUser(username).getId()).stream()
                .map(MyCommunity::from)
                .toList();
    }

    @Transactional
    public MyCommunity join(String username, String inviteCode) {
        return MyCommunity.from(addResident(findUser(username), inviteCode));
    }

    /** Adds the user to the community owning the invite code. Also used during registration. */
    @Transactional
    public CommunityMember addResident(User user, String inviteCode) {
        Community community = communityRepository.findByInviteCode(InviteCodeGenerator.normalize(inviteCode))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "邀請碼無效"));
        if (user.getId() != null && memberRepository.existsByCommunityIdAndUserId(community.getId(), user.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "你已經是「" + community.getName() + "」的成員");
        }
        return memberRepository.save(new CommunityMember(community, user, CommunityRole.RESIDENT));
    }

    @Transactional(readOnly = true)
    public CommunityDetail detail(Long communityId, String username) {
        CommunityAccess.Context ctx = access.requireMember(communityId, username);
        Community c = ctx.community();
        return new CommunityDetail(c.getId(), c.getName(), c.getDescription(), ctx.role(), ctx.canManage(),
                memberRepository.countByCommunityId(c.getId()),
                ctx.canManage() ? c.getInviteCode() : null);
    }

    @Transactional(readOnly = true)
    public List<Member> members(Long communityId, String username) {
        access.requireManager(communityId, username);
        return memberRepository.findByCommunityIdWithUser(communityId).stream().map(Member::from).toList();
    }

    @Transactional
    public Member changeMemberRole(Long communityId, Long userId, CommunityRole role, String username) {
        CommunityAccess.Context ctx = access.requireManager(communityId, username);
        CommunityMember target = findOtherMember(ctx, userId);
        requireNotAnotherManager(ctx, target, "變更");
        target.setRole(role);
        return Member.from(target);
    }

    @Transactional
    public void removeMember(Long communityId, Long userId, String username) {
        CommunityAccess.Context ctx = access.requireManager(communityId, username);
        CommunityMember target = findOtherMember(ctx, userId);
        requireNotAnotherManager(ctx, target, "移除");
        memberRepository.delete(target);
    }

    /**
     * Managers cannot demote or remove each other, so one compromised or rogue manager account cannot take a
     * community over; only a platform admin can.
     */
    private static void requireNotAnotherManager(CommunityAccess.Context ctx, CommunityMember target, String verb) {
        if (target.getRole() == CommunityRole.MANAGER && !ctx.isPlatformAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "不能" + verb + "其他社區管理員，請聯絡平台管理員");
        }
    }

    @Transactional
    public InviteCodeResponse regenerateInviteCode(Long communityId, String username) {
        Community community = access.requireManager(communityId, username).community();
        community.setInviteCode(inviteCodeGenerator.generateUnique());
        return new InviteCodeResponse(community.getInviteCode());
    }

    // ---- Platform admin operations (endpoint restricted to ROLE_ADMIN) ----

    @Transactional(readOnly = true)
    public List<AdminCommunity> listAll() {
        Map<Long, Long> counts = memberRepository.countMembersPerCommunity().stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        return communityRepository.findAll().stream()
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .map(c -> toAdminCommunity(c, counts.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Transactional
    public AdminCommunity create(CreateCommunityRequest request) {
        String name = request.name().trim();
        if (communityRepository.existsByName(name)) {
            throw new ApiException(HttpStatus.CONFLICT, "社區名稱已存在");
        }
        Community community = new Community();
        community.setName(name);
        community.setDescription(request.description());
        community.setInviteCode(inviteCodeGenerator.generateUnique());
        return toAdminCommunity(communityRepository.save(community), 0);
    }

    /** Makes the user a manager of the community, adding them as a member first if needed. */
    @Transactional
    public Member assignManager(Long communityId, String targetUsername) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "找不到此社區"));
        User user = userRepository.findByUsernameIgnoreCase(targetUsername.trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "找不到帳號「" + targetUsername + "」"));
        CommunityMember member = memberRepository.findByCommunityIdAndUserId(communityId, user.getId())
                .orElseGet(() -> new CommunityMember(community, user, CommunityRole.MANAGER));
        member.setRole(CommunityRole.MANAGER);
        return Member.from(memberRepository.save(member));
    }

    private CommunityMember findOtherMember(CommunityAccess.Context ctx, Long userId) {
        if (userId.equals(ctx.user().getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "不能變更或移除自己");
        }
        return memberRepository.findByCommunityIdAndUserId(ctx.community().getId(), userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "此使用者不是社區成員"));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "使用者不存在"));
    }

    private static AdminCommunity toAdminCommunity(Community c, long memberCount) {
        return new AdminCommunity(c.getId(), c.getName(), c.getDescription(), c.getInviteCode(),
                memberCount, c.getCreatedAt());
    }
}
