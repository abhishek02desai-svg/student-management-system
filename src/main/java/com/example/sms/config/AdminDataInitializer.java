package com.example.sms.config;

import com.example.sms.entity.Student;
import com.example.sms.enums.Role;
import com.example.sms.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Runs once at startup and creates the first ADMIN if it does not exist yet.
 * (Public registration can only ever create STUDENTs, so the first admin has to come from here.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements CommandLineRunner {

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@sms.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@12345}")
    private String adminPassword;

    @Override
    public void run(String... args) {

        if (studentRepository.existsByEmail(adminEmail)) {
            log.info("Admin account already exists: {}", adminEmail);
            return;
        }

        studentRepository.save(Student.builder()
                .firstName("System")
                .lastName("Admin")
                .email(adminEmail)
                .phoneNumber("9999999999")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .build());

        log.info("Default admin created: {}", adminEmail);
    }
}
