package com.example.sms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentPatchRequestDto {

    @Size(min = 2, max = 50, message = "First name must be 2 to 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name must be up to 50 characters")
    private String lastName;

    @Email(message = "Email format is invalid")
    private String email;

    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Phone number must be a valid 10 digit number"
    )
    private String phoneNumber;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    // Optional: move the student to another department
    private Long departmentId;
}
