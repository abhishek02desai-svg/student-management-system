package com.example.sms.service;

import com.example.sms.dto.EnrollmentRequestCreateDto;
import com.example.sms.dto.EnrollmentRequestResponseDto;
import com.example.sms.dto.PageResponse;
import com.example.sms.enums.EnrollmentEnum;

public interface EnrollmentRequestService {

    /** Student asks to join a course. Status starts as PENDING. */
    EnrollmentRequestResponseDto createRequest(EnrollmentRequestCreateDto dto);

    EnrollmentRequestResponseDto getRequestById(Long id);

    PageResponse<EnrollmentRequestResponseDto> searchRequests(
            EnrollmentEnum status, Long studentId, int page, int size, String sortBy, String direction);

    /** PENDING -> APPROVE: the student is enrolled and a notification is sent. */
    EnrollmentRequestResponseDto approveRequest(Long id);

    /** PENDING -> REJECT: a notification is sent. */
    EnrollmentRequestResponseDto rejectRequest(Long id);
}
