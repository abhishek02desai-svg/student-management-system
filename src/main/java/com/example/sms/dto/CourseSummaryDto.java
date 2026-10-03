package com.example.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/** Small version of a course, embedded inside the student response. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseSummaryDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String courseCode;
    private String title;
    private Integer credits;
}
