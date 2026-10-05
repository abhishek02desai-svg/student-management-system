package com.example.sms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "department")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(length = 500)
    private String description;

    // One Department -> Many Students (owning side is Student.department)
    @OneToMany(mappedBy = "department")
    @Builder.Default
    private Set<Student> students = new HashSet<>();

    // One Department -> Many Courses (owning side is Course.department)
    @OneToMany(mappedBy = "department")
    @Builder.Default
    private Set<Course> courses = new HashSet<>();
}
