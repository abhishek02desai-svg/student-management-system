package com.example.sms.service.impl;

import com.example.sms.dto.AuthResponseDto;
import com.example.sms.dto.LoginRequestDto;
import com.example.sms.dto.RegisterRequestDto;
import com.example.sms.dto.StudentResponseDto;
import com.example.sms.entity.Student;
import com.example.sms.repository.StudentRepository;
import com.example.sms.security.JwtTokenService;
import com.example.sms.service.AuthService;
import com.example.sms.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final StudentRepository studentRepository;
    private final StudentService studentService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    @Override
    public StudentResponseDto register(RegisterRequestDto dto, MultipartFile photo) {

        log.info("Register request for email: {}", dto.getEmail());

        // The password is hashed with BCrypt - the plain text is never stored.
        return studentService.registerStudent(dto, passwordEncoder.encode(dto.getPassword()), photo);
    }

    @Override
    public AuthResponseDto login(LoginRequestDto dto) {

        log.info("Login request for email: {}", dto.getEmail());

        // Same message for "unknown email" and "wrong password" so attackers can't tell which one failed.
        Student student = studentRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // Students created through /api/students/create have no password and cannot log in.
        if (student.getPassword() == null
                || !passwordEncoder.matches(dto.getPassword(), student.getPassword())) {
            log.warn("Login failed for email: {}", dto.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }

        log.info("Login successful for student ID: {}", student.getId());

        return AuthResponseDto.builder()
                .token(jwtTokenService.generateToken(student))
                .tokenType("Bearer")
                .expiresInSeconds(jwtTokenService.getExpiresInSeconds())
                .student(studentService.getStudentById(student.getId()))
                .build();
    }
}
