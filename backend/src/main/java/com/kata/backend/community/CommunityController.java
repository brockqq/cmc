package com.kata.backend.community;

import com.kata.backend.community.dto.CommunityDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Community endpoints; per-community permissions are enforced by {@link CommunityAccess}. */
@RestController
@RequestMapping("/api/communities")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    @GetMapping("/mine")
    public List<MyCommunity> mine(@AuthenticationPrincipal Jwt jwt) {
        return communityService.myCommunities(jwt.getSubject());
    }

    @PostMapping("/join")
    @ResponseStatus(HttpStatus.CREATED)
    public MyCommunity join(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody JoinRequest request) {
        return communityService.join(jwt.getSubject(), request.inviteCode());
    }

    @GetMapping("/{id}")
    public CommunityDetail detail(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return communityService.detail(id, jwt.getSubject());
    }

    @GetMapping("/{id}/members")
    public List<Member> members(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return communityService.members(id, jwt.getSubject());
    }

    @PutMapping("/{id}/members/{userId}/role")
    public Member changeMemberRole(@PathVariable Long id, @PathVariable Long userId,
                                   @Valid @RequestBody UpdateMemberRoleRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        return communityService.changeMemberRole(id, userId, request.role(), jwt.getSubject());
    }

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long id, @PathVariable Long userId, @AuthenticationPrincipal Jwt jwt) {
        communityService.removeMember(id, userId, jwt.getSubject());
    }

    @PostMapping("/{id}/invite-code")
    public InviteCodeResponse regenerateInviteCode(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return communityService.regenerateInviteCode(id, jwt.getSubject());
    }
}
