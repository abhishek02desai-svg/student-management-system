package com.example.sms.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CourseRequestDto {

    @NotBlank(message = "Course code is required")
    @Size(max = 20, message = "Course code must be up to 20 characters")
    private String courseCode;

    @NotBlank(message = "Course title is required")
    @Size(min = 2, max = 150, message = "Course title must be 2 to 150 characters")
    private String title;

    @Size(max = 500, message = "Description must be up to 500 characters")
    private String description;

    @NotNull(message = "Credits are required")
    @Min(value = 1, message = "Credits must be at least 1")
    @Max(value = 10, message = "Credits must be at most 10")
    private Integer credits;

    @NotNull(message = "Capacity are required")
    private Integer capacity;

    @NotNull(message = "Department id is required")
    private Long departmentId;
}
