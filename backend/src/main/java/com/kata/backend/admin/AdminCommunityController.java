package com.kata.backend.admin;

import com.kata.backend.community.CommunityService;
import com.kata.backend.community.dto.CommunityDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Access restricted to ROLE_ADMIN in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/communities")
@RequiredArgsConstructor
public class AdminCommunityController {

    private final CommunityService communityService;

    @GetMapping
    public List<AdminCommunity> list() {
        return communityService.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminCommunity create(@Valid @RequestBody CreateCommunityRequest request) {
        return communityService.create(request);
    }

    @PostMapping("/{id}/managers")
    public Member assignManager(@PathVariable Long id, @Valid @RequestBody AssignManagerRequest request) {
        return communityService.assignManager(id, request.username());
    }
}
