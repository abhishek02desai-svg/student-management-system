package com.example.sms.repository;

import com.example.sms.entity.EnrollmentRequest;
import com.example.sms.enums.EnrollmentEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EnrollmentRequestRepository
        extends JpaRepository<EnrollmentRequest, Long>, JpaSpecificationExecutor<EnrollmentRequest> {

    boolean existsByStudentIdAndCourseIdAndStatus(Long studentId, Long courseId, EnrollmentEnum status);

    // used when a student / course is deleted
    void deleteByStudentId(Long studentId);

    void deleteByCourseId(Long courseId);
}
