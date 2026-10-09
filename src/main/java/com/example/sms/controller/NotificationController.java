package com.example.sms.controller;

import com.example.sms.dto.NotificationResponseDto;
import com.example.sms.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Notifications of the logged-in student (newest first)")
    @GetMapping("/my")
    public ResponseEntity<List<NotificationResponseDto>> myNotifications(@AuthenticationPrincipal Jwt jwt) {
        Long studentId = ((Number) jwt.getClaims().get("studentId")).longValue();
        return ResponseEntity.ok(notificationService.getNotificationsForStudent(studentId));
    }

    @Operation(summary = "Notifications of a student by id (newest first)")
    @PreAuthorize("hasRole('ADMIN') or @authz.isSelf(authentication, #studentId)")
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<NotificationResponseDto>> byStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(notificationService.getNotificationsForStudent(studentId));
    }
}
