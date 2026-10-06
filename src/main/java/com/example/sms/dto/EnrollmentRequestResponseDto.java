package com.example.sms.dto;

import com.example.sms.enums.EnrollmentEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentRequestResponseDto {

    private Long id;
    private Long studentId;
    private String studentName;
    private Long courseId;
    private String courseCode;
    private String courseTitle;
    private EnrollmentEnum status;

    // from the audit columns
    private LocalDateTime requestedAt;
    private String requestedBy;
    private LocalDateTime updatedAt;
    private String updatedBy;       // who approved / rejected
}
