package com.example.sms.service.impl;

import com.example.sms.dto.EnrollmentRequestCreateDto;
import com.example.sms.dto.EnrollmentRequestResponseDto;
import com.example.sms.dto.PageResponse;
import com.example.sms.entity.Course;
import com.example.sms.entity.EnrollmentRequest;
import com.example.sms.entity.Student;
import com.example.sms.enums.EnrollmentEnum;
import com.example.sms.exception.ConflictException;
import com.example.sms.exception.ResourceNotFoundException;
import com.example.sms.repository.CourseRepository;
import com.example.sms.repository.EnrollmentRequestRepository;
import com.example.sms.repository.StudentRepository;
import com.example.sms.service.EnrollmentRequestService;
import com.example.sms.service.NotificationService;
import com.example.sms.service.StudentService;
import com.example.sms.util.PageUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentRequestServiceImpl implements EnrollmentRequestService {

    // public sort name -> entity property
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "id",
            "status", "status",
            "createdAt", "createdAt"
    );

    private final EnrollmentRequestRepository enrollmentRequestRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final StudentService studentService;           // enrolls the student (keeps Redis cache correct)
    private final NotificationService notificationService;

    @Override
    @Transactional
    public EnrollmentRequestResponseDto createRequest(EnrollmentRequestCreateDto dto) {

        log.info("Enrollment request: student {} -> course {}", dto.getStudentId(), dto.getCourseId());

        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found : " + dto.getStudentId()));
        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found : " + dto.getCourseId()));

        if (studentRepository.existsByIdAndCoursesId(student.getId(), course.getId())) {
            throw new ConflictException("Student is already enrolled in this course");
        }
        if (enrollmentRequestRepository.existsByStudentIdAndCourseIdAndStatus(
                student.getId(), course.getId(), EnrollmentEnum.PENDING)) {
            throw new ConflictException("A pending request already exists for this student and course");
        }

        EnrollmentRequest saved = enrollmentRequestRepository.save(
                EnrollmentRequest.builder()
                        .student(student)
                        .course(course)
                        .status(EnrollmentEnum.PENDING)
                        .build());

        return toDto(saved);
    }

    @Override
    public EnrollmentRequestResponseDto getRequestById(Long id) {
        return toDto(findRequest(id));
    }

    @Override
    public PageResponse<EnrollmentRequestResponseDto> searchRequests(
            EnrollmentEnum status, Long studentId, int page, int size, String sortBy, String direction) {

        Pageable pageable = PageUtil.build(page, size, sortBy, direction, SORT_FIELDS, "id");

        Specification<EnrollmentRequest> spec = (root, query, cb) -> {
            List<Predicate> filters = new ArrayList<>();
            if (status != null) {
                filters.add(cb.equal(root.get("status"), status));
            }
            if (studentId != null) {
                filters.add(cb.equal(root.get("student").get("id"), studentId));
            }
            return cb.and(filters.toArray(new Predicate[0]));
        };

        Page<EnrollmentRequestResponseDto> result =
                enrollmentRequestRepository.findAll(spec, pageable).map(this::toDto);

        return PageResponse.from(result);
    }

    @Override
    @Transactional
    public EnrollmentRequestResponseDto approveRequest(Long id) {

        log.info("Approving enrollment request {}", id);

        EnrollmentRequest request = findRequest(id);
        ensurePending(request);

        // 1) really enroll the student (goes through StudentService so the cached student is refreshed)
        studentService.enrollInCourse(request.getStudent().getId(), request.getCourse().getId());

        // 2) mark the request. saveAndFlush => audit columns (updatedBy/updatedAt) are filled before we reply
        request.setStatus(EnrollmentEnum.APPROVE);
        EnrollmentRequest saved = enrollmentRequestRepository.saveAndFlush(request);

        // 3) notify the student
        notificationService.notifyEnrollmentDecision(saved.getStudent(), saved.getCourse(), EnrollmentEnum.APPROVE);

        return toDto(saved);
    }

    @Override
    @Transactional
    public EnrollmentRequestResponseDto rejectRequest(Long id) {

        log.info("Rejecting enrollment request {}", id);

        EnrollmentRequest request = findRequest(id);
        ensurePending(request);

        request.setStatus(EnrollmentEnum.REJECT);
        EnrollmentRequest saved = enrollmentRequestRepository.saveAndFlush(request);

        notificationService.notifyEnrollmentDecision(saved.getStudent(), saved.getCourse(), EnrollmentEnum.REJECT);

        return toDto(saved);
    }

    // ---------------- helpers ----------------

    private EnrollmentRequest findRequest(Long id) {
        return enrollmentRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment request not found : " + id));
    }

    // only a PENDING request can be decided, and only once
    private void ensurePending(EnrollmentRequest request) {
        if (request.getStatus() != EnrollmentEnum.PENDING) {
            throw new ConflictException("Request is already " + request.getStatus() + " and cannot be changed");
        }
    }

    private EnrollmentRequestResponseDto toDto(EnrollmentRequest r) {
        Student s = r.getStudent();
        Course c = r.getCourse();
        return EnrollmentRequestResponseDto.builder()
                .id(r.getId())
                .studentId(s.getId())
                .studentName(s.getFirstName() + " " + s.getLastName())
                .courseId(c.getId())
                .courseCode(c.getCourseCode())
                .courseTitle(c.getTitle())
                .status(r.getStatus())
                .requestedAt(r.getCreatedAt())
                .requestedBy(r.getCreatedBy())
                .updatedAt(r.getUpdatedAt())
                .updatedBy(r.getUpdatedBy())
                .build();
    }
}
