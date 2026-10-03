package com.example.sms.specification;

import com.example.sms.entity.Course;
import com.example.sms.entity.Department;
import com.example.sms.entity.Student;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;
import java.util.Objects;

/**
 * Building blocks for the student directory search.
 * Each method returns one optional filter; the service combines them with .and(...).
 */
public final class StudentSpecification {

    private StudentSpecification() {
    }

    /** keyword matches full name, email or phone (case-insensitive "contains") */
    public static Specification<Student> keywordContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + escapeLike(keyword.trim().toLowerCase(Locale.ROOT)) + "%";

            Expression<String> fullName = cb.lower(
                    cb.concat(cb.concat(root.<String>get("firstName"), " "), root.<String>get("lastName")));

            return cb.or(
                    cb.like(fullName, pattern, '\\'),
                    cb.like(cb.lower(root.<String>get("email")), pattern, '\\'),
                    cb.like(root.<String>get("phoneNumber"), pattern, '\\')
            );
        };
    }

    /** students belonging to one department */
    public static Specification<Student> inDepartment(Long departmentId) {
        return (root, query, cb) -> {
            if (departmentId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.<Department>get("department").<Long>get("id"), departmentId);
        };
    }

    /**
     * students enrolled in one course.
     * Uses EXISTS (sub-query) instead of a JOIN so we never get duplicate rows
     * and ORDER BY / paging keep working.
     */
    public static Specification<Student> enrolledInCourse(Long courseId) {
        return (root, query, cb) -> {
            if (courseId == null) {
                return cb.conjunction();
            }
            Objects.requireNonNull(query);
            Subquery<Long> sub = query.subquery(Long.class);
            Root<Student> subStudent = sub.from(Student.class);
            Join<Student, Course> subCourse = subStudent.join("courses");
            sub.select(subStudent.<Long>get("id"))
                    .where(
                            cb.equal(subStudent.get("id"), root.get("id")),
                            cb.equal(subCourse.get("id"), courseId)
                    );
            return cb.exists(sub);
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
