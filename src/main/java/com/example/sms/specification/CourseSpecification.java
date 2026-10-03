package com.example.sms.specification;

import com.example.sms.entity.Course;
import com.example.sms.entity.Department;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class CourseSpecification {

    private CourseSpecification() {
    }

    /** keyword matches course code or title (case-insensitive "contains") */
    public static Specification<Course> keywordContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + escapeLike(keyword.trim().toLowerCase(Locale.ROOT)) + "%";
            return cb.or(
                    cb.like(cb.lower(root.<String>get("courseCode")), pattern, '\\'),
                    cb.like(cb.lower(root.<String>get("title")), pattern, '\\')
            );
        };
    }

    public static Specification<Course> inDepartment(Long departmentId) {
        return (root, query, cb) -> {
            if (departmentId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.<Department>get("department").<Long>get("id"), departmentId);
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
