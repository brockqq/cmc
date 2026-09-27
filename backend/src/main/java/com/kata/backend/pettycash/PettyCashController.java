package com.kata.backend.pettycash;

import com.kata.backend.pettycash.PettyCashDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Petty cash endpoints; role checks live in {@link PettyCashService}. */
@RestController
@RequiredArgsConstructor
public class PettyCashController {

    private final PettyCashService service;

    @GetMapping("/api/communities/{communityId}/petty-cash")
    public Summary summary(@PathVariable Long communityId, @AuthenticationPrincipal Jwt jwt) {
        return service.summary(communityId, jwt.getSubject());
    }

    @PutMapping("/api/communities/{communityId}/petty-cash/fund")
    public Summary setFund(@PathVariable Long communityId, @AuthenticationPrincipal Jwt jwt,
                           @Valid @RequestBody FundRequest request) {
        return service.setFundAmount(communityId, jwt.getSubject(), request.amount());
    }

    @PostMapping("/api/communities/{communityId}/petty-cash/replenish")
    public Summary replenish(@PathVariable Long communityId, @AuthenticationPrincipal Jwt jwt,
                             @Valid @RequestBody(required = false) ReplenishRequest request) {
        return service.replenish(communityId, jwt.getSubject(), request == null ? null : request.note());
    }

    /** {@code period} = period number; defaults to the open period. */
    @GetMapping("/api/communities/{communityId}/petty-cash/expenses")
    public List<ExpenseResponse> expenses(@PathVariable Long communityId, @AuthenticationPrincipal Jwt jwt,
                                          @RequestParam(required = false) Integer period) {
        return service.expenses(communityId, jwt.getSubject(), period);
    }

    /** Audit trail of a period (default: the open one), newest first; includes deleted expenses. */
    @GetMapping("/api/communities/{communityId}/petty-cash/log")
    public List<LogEntry> log(@PathVariable Long communityId, @AuthenticationPrincipal Jwt jwt,
                              @RequestParam(required = false) Integer period) {
        return service.log(communityId, jwt.getSubject(), period);
    }

    @PostMapping("/api/communities/{communityId}/petty-cash/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse createExpense(@PathVariable Long communityId, @AuthenticationPrincipal Jwt jwt,
                                         @Valid @RequestBody ExpenseRequest request) {
        return service.createExpense(communityId, jwt.getSubject(), request);
    }

    @PutMapping("/api/petty-cash/expenses/{expenseId}")
    public ExpenseResponse updateExpense(@PathVariable Long expenseId, @AuthenticationPrincipal Jwt jwt,
                                         @Valid @RequestBody ExpenseRequest request) {
        return service.updateExpense(expenseId, jwt.getSubject(), request);
    }

    @DeleteMapping("/api/petty-cash/expenses/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(@PathVariable Long expenseId, @AuthenticationPrincipal Jwt jwt) {
        service.deleteExpense(expenseId, jwt.getSubject());
    }

    /** Uploads (or replaces) the receipt file: multipart field {@code file}, image or PDF up to 5 MB. */
    @PostMapping(path = "/api/petty-cash/expenses/{expenseId}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExpenseResponse attach(@PathVariable Long expenseId, @AuthenticationPrincipal Jwt jwt,
                                  @RequestParam("file") MultipartFile file) {
        return service.attach(expenseId, jwt.getSubject(), file);
    }

    @DeleteMapping("/api/petty-cash/expenses/{expenseId}/attachment")
    public ExpenseResponse removeAttachment(@PathVariable Long expenseId, @AuthenticationPrincipal Jwt jwt) {
        return service.removeAttachment(expenseId, jwt.getSubject());
    }

    @GetMapping("/api/petty-cash/expenses/{expenseId}/attachment")
    public ResponseEntity<Resource> download(@PathVariable Long expenseId, @AuthenticationPrincipal Jwt jwt) {
        PettyCashService.Download d = service.attachment(expenseId, jwt.getSubject());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(d.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(d.fileName(), StandardCharsets.UTF_8).build().toString())
                // Receipts may be sensitive; don't let shared caches keep them
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                // Uploaded content: never let a browser guess another type, run scripts or load anything from it
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; img-src 'self'; style-src 'unsafe-inline'")
                .body(d.resource());
    }
}
