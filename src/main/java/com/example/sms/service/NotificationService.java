package com.example.sms.service;

import com.example.sms.dto.NotificationResponseDto;
import com.example.sms.entity.Course;
import com.example.sms.entity.Student;
import com.example.sms.enums.EnrollmentEnum;

import java.util.List;

public interface NotificationService {

    /** Tells the student their request was approved or rejected (saved in DB + written to the log). */
    void notifyEnrollmentDecision(Student student, Course course, EnrollmentEnum decision);

    List<NotificationResponseDto> getNotificationsForStudent(Long studentId);
}
