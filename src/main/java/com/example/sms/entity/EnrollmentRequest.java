package com.example.sms.entity;

import com.example.sms.enums.EnrollmentEnum;
import jakarta.persistence.*;
import lombok.*;

/**
 * A student asks to join a course. An admin later approves or rejects it.
 * createdBy/createdAt (BaseEntity) = who asked and when,
 * updatedBy/updatedAt (BaseEntity) = who approved/rejected and when.
 */
@Entity
@Table(name = "enrollment_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many requests -> One student
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // Many requests -> One course
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EnrollmentEnum status = EnrollmentEnum.PENDING;
}
