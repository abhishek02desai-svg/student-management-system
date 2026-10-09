package com.example.sms.entity;

import com.example.sms.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "student")
@Setter
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class Student extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(nullable = false, length = 10)
    private String phoneNumber;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    // BCrypt hash. Nullable so students created before login existed are not broken.
    @Column(length = 100)
    private String password;

    // STUDENT (default) or ADMIN. columnDefinition gives old rows a value when Hibernate adds the column.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'STUDENT'")
    @Builder.Default
    private Role role = Role.STUDENT;

    // Name of the uploaded profile image file (stored on disk, not in the DB)
    @Column(length = 255)
    private String profileImageName;

    // Many Students -> One Department
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    // Many Students <-> Many Courses (join table: student_course)
    @ManyToMany
    @JoinTable(
            name = "student_course",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    @Builder.Default
    private Set<Course> courses = new HashSet<>();

}
