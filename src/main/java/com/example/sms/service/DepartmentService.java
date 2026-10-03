package com.example.sms.service;

import com.example.sms.dto.DepartmentRequestDto;
import com.example.sms.dto.DepartmentResponseDto;
import com.example.sms.dto.DepartmentSearchCriteria;
import com.example.sms.dto.PageResponse;

public interface DepartmentService {

    DepartmentResponseDto createDepartment(DepartmentRequestDto dto);

    DepartmentResponseDto getDepartmentById(Long id);

    PageResponse<DepartmentResponseDto> searchDepartments(DepartmentSearchCriteria criteria);

    DepartmentResponseDto updateDepartment(Long id, DepartmentRequestDto dto);

    void deleteDepartment(Long id);
}
