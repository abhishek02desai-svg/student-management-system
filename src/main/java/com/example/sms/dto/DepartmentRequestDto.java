package com.example.sms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DepartmentRequestDto {

    @NotBlank(message = "Department name is required")
    @Size(min = 2, max = 100, message = "Department name must be 2 to 100 characters")
    private String name;

    @NotBlank(message = "Department code is required")
    @Size(min = 2, max = 10, message = "Department code must be 2 to 10 characters")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "Department code can contain only letters and digits")
    private String code;

    @Size(max = 500, message = "Description must be up to 500 characters")
    private String description;
}
