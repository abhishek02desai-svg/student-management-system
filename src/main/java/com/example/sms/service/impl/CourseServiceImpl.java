package com.example.sms.service.impl;

import com.example.sms.dto.CourseRequestDto;
import com.example.sms.dto.CourseResponseDto;
import com.example.sms.dto.CourseSearchCriteria;
import com.example.sms.dto.DepartmentSummaryDto;
import com.example.sms.dto.PageResponse;
import com.example.sms.entity.Course;
import com.example.sms.entity.Department;
import com.example.sms.exception.ConflictException;
import com.example.sms.exception.DuplicateResourceException;
import com.example.sms.exception.ResourceNotFoundException;
import com.example.sms.repository.CourseRepository;
import com.example.sms.repository.DepartmentRepository;
import com.example.sms.repository.EnrollmentRequestRepository;
import com.example.sms.repository.StudentRepository;
import com.example.sms.service.CourseService;
import com.example.sms.specification.CourseSpecification;
import com.example.sms.util.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

    // public sort name -> entity property
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "id",
            "courseCode", "courseCode",
            "title", "title",
            "credits", "credits",
            "department", "department.name"
    );

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRequestRepository enrollmentRequestRepository;

    @Override
    @Transactional
    @Caching(
            put = @CachePut(cacheNames = "course", key = "#result.id"),
            evict = @CacheEvict(cacheNames = "courseSearch", allEntries = true)
    )
    public CourseResponseDto createCourse(CourseRequestDto dto) {

        log.info("Creating course with code: {}", dto.getCourseCode());

        String code = dto.getCourseCode().trim().toUpperCase(Locale.ROOT);
        if (courseRepository.existsByCourseCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("Course already exists with code : " + code);
        }

        Course saved = courseRepository.save(
                Course.builder()
                        .courseCode(code)
                        .title(dto.getTitle().trim())
                        .description(dto.getDescription())
                        .credits(dto.getCredits())
                        .department(findDepartment(dto.getDepartmentId()))
                        .build());

        return toDto(saved);
    }

    @Override
    @Cacheable(cacheNames = "course", key = "#id")
    public CourseResponseDto getCourseById(Long id) {
        return toDto(findCourse(id));
    }

    @Override
    @Cacheable(cacheNames = "courseSearch", key = "#criteria.cacheKey()")
    public PageResponse<CourseResponseDto> searchCourses(CourseSearchCriteria criteria) {

        log.info("Searching courses: {}", criteria);

        Pageable pageable = PageUtil.build(criteria.page(), criteria.size(),
                criteria.sortBy(), criteria.direction(), SORT_FIELDS, "courseCode");

        Specification<Course> spec = CourseSpecification.keywordContains(criteria.keyword())
                .and(CourseSpecification.inDepartment(criteria.departmentId()));

        Page<CourseResponseDto> page = courseRepository.findAll(spec, pageable).map(this::toDto);

        return PageResponse.from(page);
    }

    // Courses are shown inside cached student responses, so clear those too.
    @Override
    @Transactional
    @Caching(
            put = @CachePut(cacheNames = "course", key = "#id"),
            evict = {
                    @CacheEvict(cacheNames = "courseSearch", allEntries = true),
                    @CacheEvict(cacheNames = {"student", "studentSearch"}, allEntries = true)
            }
    )
    public CourseResponseDto updateCourse(Long id, CourseRequestDto dto) {

        log.info("Updating course with ID: {}", id);

        Course course = findCourse(id);

        String code = dto.getCourseCode().trim().toUpperCase(Locale.ROOT);
        if (courseRepository.existsByCourseCodeIgnoreCaseAndIdNot(code, id)) {
            throw new DuplicateResourceException("Another course already exists with code : " + code);
        }

        course.setCourseCode(code);
        course.setTitle(dto.getTitle().trim());
        course.setDescription(dto.getDescription());
        course.setCredits(dto.getCredits());
        course.setDepartment(findDepartment(dto.getDepartmentId()));

        return toDto(courseRepository.save(course));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "course", key = "#id"),
            @CacheEvict(cacheNames = "courseSearch", allEntries = true)
    })
    public void deleteCourse(Long id) {

        log.info("Deleting course with ID: {}", id);

        Course course = findCourse(id);

        if (studentRepository.existsByCoursesId(id)) {
            throw new ConflictException("Cannot delete course: students are still enrolled in it");
        }

        // remove enrollment requests of this course first (foreign key)
        enrollmentRequestRepository.deleteByCourseId(id);

        courseRepository.delete(course);
    }

    private Course findCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found : " + id));
    }

    private Department findDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found : " + id));
    }

    private CourseResponseDto toDto(Course c) {
        Department d = c.getDepartment();
        return CourseResponseDto.builder()
                .id(c.getId())
                .courseCode(c.getCourseCode())
                .title(c.getTitle())
                .description(c.getDescription())
                .credits(c.getCredits())
                .department(DepartmentSummaryDto.builder()
                        .id(d.getId())
                        .name(d.getName())
                        .code(d.getCode())
                        .build())
                .build();
    }
}
