package com.example.sms.controller;

import com.example.sms.dto.EnrollmentRequestCreateDto;
import com.example.sms.dto.EnrollmentRequestResponseDto;
import com.example.sms.dto.PageResponse;
import com.example.sms.enums.EnrollmentEnum;
import com.example.sms.service.EnrollmentRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enrollment-requests")
@RequiredArgsConstructor
@Tag(name = "Enrollment Requests", description = "Student asks for a course; ADMIN approves / rejects and the student is notified")
public class EnrollmentRequestController {

    private final EnrollmentRequestService enrollmentRequestService;

    @Operation(summary = "Request a course (status starts as PENDING). A student can only request for himself/herself")
    @PreAuthorize("hasRole('ADMIN') or @authz.isSelf(authentication, #dto.studentId)")
    @PostMapping("/create")
    public ResponseEntity<EnrollmentRequestResponseDto> create(@Valid @RequestBody EnrollmentRequestCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentRequestService.createRequest(dto));
    }

    @Operation(summary = "My own requests (logged-in student), newest first")
    @GetMapping("/my")
    public ResponseEntity<PageResponse<EnrollmentRequestResponseDto>> myRequests(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long studentId = ((Number) jwt.getClaims().get("studentId")).longValue();
        return ResponseEntity.ok(enrollmentRequestService.searchRequests(null, studentId, page, size, "id", "desc"));
    }

    @Operation(summary = "One request by id (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<EnrollmentRequestResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(enrollmentRequestService.getRequestById(id));
    }

    @Operation(summary = "List all requests (ADMIN only, paged + sorted)",
            description = "Optional filters: status (PENDING | APPROVE | REJECT) and studentId. "
                    + "sortBy: id | status | createdAt. direction: asc | desc")
    @GetMapping("/search")
    public ResponseEntity<PageResponse<EnrollmentRequestResponseDto>> search(
            @RequestParam(required = false) EnrollmentEnum status,
            @RequestParam(required = false) Long studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(enrollmentRequestService.searchRequests(
                status, studentId, page, size, sortBy, direction));
    }

    @Operation(summary = "Approve a PENDING request (ADMIN only): enrolls the student and sends a notification")
    @PutMapping("/{id}/approve")
    public ResponseEntity<EnrollmentRequestResponseDto> approve(@PathVariable Long id) {
        return ResponseEntity.ok(enrollmentRequestService.approveRequest(id));
    }

    @Operation(summary = "Reject a PENDING request (ADMIN only) and send a notification")
    @PutMapping("/{id}/reject")
    public ResponseEntity<EnrollmentRequestResponseDto> reject(@PathVariable Long id) {
        return ResponseEntity.ok(enrollmentRequestService.rejectRequest(id));
    }
}
