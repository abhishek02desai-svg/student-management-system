package com.example.sms.dto;

import com.example.sms.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

// Serializable because responses are cached in Redis
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponseDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 2L;   // changed: new "role" field

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private Role role;

    // null when the student has no profile image
    private String profileImageUrl;

    private DepartmentSummaryDto department;
    private List<CourseSummaryDto> courses;

}
