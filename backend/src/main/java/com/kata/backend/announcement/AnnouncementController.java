package com.kata.backend.announcement;

import com.kata.backend.announcement.AnnouncementDtos.*;
import com.kata.backend.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    /** {@code page} is 0-based. {@code platform=true} limits to platform-wide announcements. */
    @GetMapping
    public PageResponse<AnnouncementResponse> list(@AuthenticationPrincipal Jwt jwt,
                                                   @RequestParam(required = false) Long communityId,
                                                   @RequestParam(defaultValue = "false") boolean platform,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return announcementService.list(jwt.getSubject(), communityId, platform, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnnouncementResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateRequest request) {
        return announcementService.create(jwt.getSubject(), request);
    }

    @PutMapping("/{id}")
    public AnnouncementResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                       @Valid @RequestBody UpdateRequest request) {
        return announcementService.update(jwt.getSubject(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        announcementService.delete(jwt.getSubject(), id);
    }
}
