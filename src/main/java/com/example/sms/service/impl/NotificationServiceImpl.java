package com.example.sms.service.impl;

import com.example.sms.dto.NotificationResponseDto;
import com.example.sms.entity.Course;
import com.example.sms.entity.Notification;
import com.example.sms.entity.Student;
import com.example.sms.enums.EnrollmentEnum;
import com.example.sms.exception.ResourceNotFoundException;
import com.example.sms.repository.NotificationRepository;
import com.example.sms.repository.StudentRepository;
import com.example.sms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final StudentRepository studentRepository;

    @Override
    @Transactional
    public void notifyEnrollmentDecision(Student student, Course course, EnrollmentEnum decision) {

        if (decision == EnrollmentEnum.PENDING) {
            throw new IllegalArgumentException("A notification is only sent for APPROVE or REJECT");
        }

        String result = decision == EnrollmentEnum.APPROVE ? "approved" : "rejected";
        String message = "Hi " + student.getFirstName() + ", your enrollment request for "
                + course.getCourseCode() + " - " + course.getTitle() + " has been " + result + ".";

        notificationRepository.save(Notification.builder()
                .student(student)
                .message(message)
                .build());

        // The "send" step. To also send a real e-mail, call a mail sender here.
        log.info("NOTIFICATION to {} : {}", student.getEmail(), message);
    }

    @Override
    public List<NotificationResponseDto> getNotificationsForStudent(Long studentId) {

        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found : " + studentId);
        }

        return notificationRepository.findByStudentIdOrderByCreatedAtDescIdDesc(studentId)
                .stream()
                .map(n -> NotificationResponseDto.builder()
                        .id(n.getId())
                        .message(n.getMessage())
                        .sentAt(n.getCreatedAt())
                        .build())
                .toList();
    }
}
