package com.example.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/** Small version of a department, embedded inside student / course responses. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentSummaryDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String code;
}
