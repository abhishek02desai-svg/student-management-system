package com.example.sms.controller;

import com.example.sms.dto.AuthResponseDto;
import com.example.sms.dto.LoginRequestDto;
import com.example.sms.dto.RegisterRequestDto;
import com.example.sms.dto.StudentResponseDto;
import com.example.sms.service.AuthService;
import com.example.sms.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Student registration and login (JWT)")
public class AuthController {

    private final AuthService authService;
    private final StudentService studentService;

    @Operation(summary = "Register a new student with an optional photo (public, multipart/form-data)",
            description = "Send form-data fields: firstName, lastName, email, phoneNumber, "
                    + "dateOfBirth (yyyy-MM-dd), password, departmentId (optional) and photo (optional file, JPEG/PNG max 2 MB)")
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentResponseDto> register(
            @Valid @ModelAttribute RegisterRequestDto dto,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {

        StudentResponseDto response = authService.register(dto, photo);
        log.info("Student registered with ID: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Login and get a JWT token (public)")
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @Operation(summary = "Profile of the logged-in student (needs token)")
    @GetMapping("/me")
    public ResponseEntity<StudentResponseDto> me(@AuthenticationPrincipal Jwt jwt) {
        Long studentId = ((Number) jwt.getClaims().get("studentId")).longValue();
        return ResponseEntity.ok(studentService.getStudentById(studentId));
    }
}
