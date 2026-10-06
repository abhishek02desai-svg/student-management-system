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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enrollment-requests")
@RequiredArgsConstructor
@Tag(name = "Enrollment Requests", description = "Student asks for a course; approve / reject sends a notification")
public class EnrollmentRequestController {

    private final EnrollmentRequestService enrollmentRequestService;

    @Operation(summary = "Student requests a course (status starts as PENDING)")
    @PostMapping("/create")
    public ResponseEntity<EnrollmentRequestResponseDto> create(@Valid @RequestBody EnrollmentRequestCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentRequestService.createRequest(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnrollmentRequestResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(enrollmentRequestService.getRequestById(id));
    }

    @Operation(summary = "List requests (paged + sorted)",
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

    @Operation(summary = "Approve a PENDING request: enrolls the student and sends a notification")
    @PutMapping("/{id}/approve")
    public ResponseEntity<EnrollmentRequestResponseDto> approve(@PathVariable Long id) {
        return ResponseEntity.ok(enrollmentRequestService.approveRequest(id));
    }

    @Operation(summary = "Reject a PENDING request and send a notification")
    @PutMapping("/{id}/reject")
    public ResponseEntity<EnrollmentRequestResponseDto> reject(@PathVariable Long id) {
        return ResponseEntity.ok(enrollmentRequestService.rejectRequest(id));
    }
}
