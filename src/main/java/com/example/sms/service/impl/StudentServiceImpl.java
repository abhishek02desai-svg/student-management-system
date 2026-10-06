package com.example.sms.service.impl;

import com.example.sms.dto.CourseSummaryDto;
import com.example.sms.dto.DepartmentSummaryDto;
import com.example.sms.dto.PageResponse;
import com.example.sms.dto.ProfileImageData;
import com.example.sms.dto.RegisterRequestDto;
import com.example.sms.dto.StudentPatchRequestDto;
import com.example.sms.dto.StudentRequestDto;
import com.example.sms.dto.StudentResponseDto;
import com.example.sms.dto.StudentSearchCriteria;
import com.example.sms.entity.Course;
import com.example.sms.entity.Department;
import com.example.sms.entity.Student;
import com.example.sms.exception.BadRequestException;
import com.example.sms.exception.DuplicateResourceException;
import com.example.sms.exception.ResourceNotFoundException;
import com.example.sms.repository.CourseRepository;
import com.example.sms.repository.DepartmentRepository;
import com.example.sms.repository.EnrollmentRequestRepository;
import com.example.sms.repository.NotificationRepository;
import com.example.sms.repository.StudentRepository;
import com.example.sms.service.FileStorageService;
import com.example.sms.service.StudentService;
import com.example.sms.specification.StudentSpecification;
import com.example.sms.util.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/*
 * CACHING RULES (Redis)
 *   "student"        -> one StudentResponseDto per id                (read: @Cacheable, write: @CachePut / @CacheEvict)
 *   "studentSearch"  -> paged search results, key = search criteria  (cleared completely on every student change)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentServiceImpl implements StudentService {

    // public sort name -> entity property
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "id",
            "firstName", "firstName",
            "lastName", "lastName",
            "email", "email",
            "dateOfBirth", "dateOfBirth",
            "department", "department.name"
    );

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final FileStorageService fileStorageService;
    private final EnrollmentRequestRepository enrollmentRequestRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    @Caching(
            put = @CachePut(cacheNames = "student", key = "#result.id"),
            evict = @CacheEvict(cacheNames = "studentSearch", allEntries = true)
    )
    public StudentResponseDto createStudent(StudentRequestDto studentRequestDto) {

        log.info("Creating student with email: {}", studentRequestDto.getEmail());

        if (studentRepository.existsByEmail(studentRequestDto.getEmail())) {

            log.warn("Student creation failed. Email already exists: {}",
                    studentRequestDto.getEmail());

            throw new DuplicateResourceException(
                    "Student already exists with email : "
                            + studentRequestDto.getEmail()
            );
        }

        Student student = dtoToEntity(studentRequestDto);
        if (studentRequestDto.getDepartmentId() != null) {
            student.setDepartment(findDepartment(studentRequestDto.getDepartmentId()));
        }

        Student saved = studentRepository.save(student);

        log.info("Student created successfully with ID: {}", saved.getId());

        return entityToDto(saved);
    }


    @Override
    @Cacheable(cacheNames = "student", key = "#id")
    public StudentResponseDto getStudentById(Long id) {

        log.info("Fetching student with ID: {} (cache miss - reading from database)", id);

        Student student = findStudent(id);

        log.info("Student found successfully with ID: {}", id);

        return entityToDto(student);
    }


    @Override
    public List<StudentResponseDto> getAllStudents() {

        log.info("Fetching all students");

        List<StudentResponseDto> students = studentRepository.findAll()
                .stream()
                .map(this::entityToDto)
                .toList();

        log.info("Successfully fetched {} students", students.size());

        return students;
    }


    @Override
    @Transactional
    @Caching(
            put = @CachePut(cacheNames = "student", key = "#id"),
            evict = @CacheEvict(cacheNames = "studentSearch", allEntries = true)
    )
    public StudentResponseDto updateStudent(
            Long id,
            StudentRequestDto studentRequestDto) {

        log.info("Updating student with ID: {}", id);

        Student existingStudent = findStudent(id);

        if (studentRepository.existsByEmailAndIdNot(studentRequestDto.getEmail(), id)) {
            throw new DuplicateResourceException(
                    "Another student already exists with email : "
                            + studentRequestDto.getEmail()
            );
        }

        // Update existing entity
        existingStudent.setFirstName(studentRequestDto.getFirstName());
        existingStudent.setLastName(studentRequestDto.getLastName());
        existingStudent.setEmail(studentRequestDto.getEmail());
        existingStudent.setPhoneNumber(studentRequestDto.getPhoneNumber());
        existingStudent.setDateOfBirth(studentRequestDto.getDateOfBirth());

        if (studentRequestDto.getDepartmentId() != null) {
            existingStudent.setDepartment(findDepartment(studentRequestDto.getDepartmentId()));
        }

        Student savedStudent = studentRepository.save(existingStudent);

        log.info("Student updated successfully with ID: {}", id);

        return entityToDto(savedStudent);
    }


    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "student", key = "#id"),
            @CacheEvict(cacheNames = "studentSearch", allEntries = true)
    })
    public void deleteStudent(Long id) {

        log.info("Deleting student with ID: {}", id);

        Student existingStudent = findStudent(id);
        String imageName = existingStudent.getProfileImageName();

        // remove rows that point to this student first (foreign keys)
        enrollmentRequestRepository.deleteByStudentId(id);
        notificationRepository.deleteByStudentId(id);

        studentRepository.delete(existingStudent);
        fileStorageService.delete(imageName);

        log.info("Student deleted successfully with ID: {}", id);
    }


    // PATCH
    @Override
    @Transactional
    @Caching(
            put = @CachePut(cacheNames = "student", key = "#id"),
            evict = @CacheEvict(cacheNames = "studentSearch", allEntries = true)
    )
    public StudentResponseDto patchStudent(
            Long id,
            StudentPatchRequestDto dto) {

        log.info("Partially updating student with ID: {}", id);

        Student existingStudent = findStudent(id);

        if (dto.getFirstName() != null) {
            log.debug("Updating first name for student ID: {}", id);
            existingStudent.setFirstName(dto.getFirstName());
        }

        if (dto.getLastName() != null) {
            log.debug("Updating last name for student ID: {}", id);
            existingStudent.setLastName(dto.getLastName());
        }

        if (dto.getEmail() != null) {

            log.debug("Updating email for student ID: {}", id);

            if (studentRepository.existsByEmailAndIdNot(
                    dto.getEmail(), id)) {

                log.warn("PATCH failed. Email already exists: {}",
                        dto.getEmail());

                throw new DuplicateResourceException(
                        "Another student already exists with email : "
                                + dto.getEmail()
                );
            }

            existingStudent.setEmail(dto.getEmail());
        }

        if (dto.getPhoneNumber() != null) {
            log.debug("Updating phone number for student ID: {}", id);
            existingStudent.setPhoneNumber(dto.getPhoneNumber());
        }

        if (dto.getDateOfBirth() != null) {
            log.debug("Updating date of birth for student ID: {}", id);
            existingStudent.setDateOfBirth(dto.getDateOfBirth());
        }

        if (dto.getDepartmentId() != null) {
            log.debug("Updating department for student ID: {}", id);
            existingStudent.setDepartment(findDepartment(dto.getDepartmentId()));
        }

        Student patchStudent = studentRepository.save(existingStudent);

        log.info("Student partially updated successfully with ID: {}", id);

        return entityToDto(patchStudent);
    }


    // ==========================================================
    // SEARCH + PAGINATION + SORTING
    // Scenario: admin "student directory" screen. The admin types part of a name /
    // email / phone, optionally filters by department and/or course, picks a sort
    // column and moves between pages.
    // ==========================================================
    @Override
    @Cacheable(cacheNames = "studentSearch", key = "#criteria.cacheKey()")
    public PageResponse<StudentResponseDto> searchStudents(StudentSearchCriteria criteria) {

        log.info("Searching students: {} (cache miss - reading from database)", criteria);

        Pageable pageable = PageUtil.build(criteria.page(), criteria.size(),
                criteria.sortBy(), criteria.direction(), SORT_FIELDS, "id");

        Specification<Student> spec = StudentSpecification.keywordContains(criteria.keyword())
                .and(StudentSpecification.inDepartment(criteria.departmentId()))
                .and(StudentSpecification.enrolledInCourse(criteria.courseId()));

        Page<StudentResponseDto> page = studentRepository.findAll(spec, pageable).map(this::entityToDto);

        return PageResponse.from(page);
    }


    // ==========================================================
    // MAPPING: department & courses
    // ==========================================================
    @Override
    @Transactional
    @Caching(
            put = @CachePut(cacheNames = "student", key = "#studentId"),
            evict = @CacheEvict(cacheNames = "studentSearch", allEntries = true)
    )
    public StudentResponseDto assignDepartment(Long studentId, Long departmentId) {

        log.info("Assigning student {} to department {}", studentId, departmentId);

        Student student = findStudent(studentId);
        student.setDepartment(findDepartment(departmentId));

        return entityToDto(studentRepository.save(student));
    }


    @Override
    @Transactional
    @Caching(
            put = @CachePut(cacheNames = "student", key = "#studentId"),
            evict = @CacheEvict(cacheNames = "studentSearch", allEntries = true)
    )
    public StudentResponseDto enrollInCourse(Long studentId, Long courseId) {

        log.info("Enrolling student {} in course {}", studentId, courseId);

        Student student = findStudent(studentId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found : " + courseId));

        boolean alreadyEnrolled = student.getCourses().stream()
                .anyMatch(c -> c.getId().equals(courseId));
        if (alreadyEnrolled) {
            throw new DuplicateResourceException(
                    "Student " + studentId + " is already enrolled in course " + courseId);
        }

        student.getCourses().add(course);

        return entityToDto(studentRepository.save(student));
    }


    @Override
    @Transactional
    @Caching(
            put = @CachePut(cacheNames = "student", key = "#studentId"),
            evict = @CacheEvict(cacheNames = "studentSearch", allEntries = true)
    )
    public StudentResponseDto unenrollFromCourse(Long studentId, Long courseId) {

        log.info("Removing student {} from course {}", studentId, courseId);

        Student student = findStudent(studentId);

        boolean removed = student.getCourses().removeIf(c -> c.getId().equals(courseId));
        if (!removed) {
            throw new ResourceNotFoundException(
                    "Student " + studentId + " is not enrolled in course " + courseId);
        }

        return entityToDto(studentRepository.save(student));
    }


    // ==========================================================
    // AUTH SUPPORT (called by AuthService so the cache annotations still apply)
    // ==========================================================
    @Override
    @Transactional
    @Caching(
            put = @CachePut(cacheNames = "student", key = "#result.id"),
            evict = @CacheEvict(cacheNames = "studentSearch", allEntries = true)
    )
    public StudentResponseDto registerStudent(RegisterRequestDto dto, String encodedPassword, MultipartFile photo) {

        log.info("Registering student with email: {}", dto.getEmail());

        if (studentRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException(
                    "Student already exists with email : " + dto.getEmail());
        }

        Student student = Student.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .dateOfBirth(dto.getDateOfBirth())
                .password(encodedPassword)
                .build();

        if (dto.getDepartmentId() != null) {
            student.setDepartment(findDepartment(dto.getDepartmentId()));
        }

        Student saved = studentRepository.save(student);

        // Optional photo. Validation happens inside store(); if it fails the exception
        // rolls back this transaction, so no student is created without the photo the user sent.
        if (photo != null && !photo.isEmpty()) {
            String imageName = fileStorageService.store(photo, saved.getId());
            saved.setProfileImageName(imageName);
            saved = studentRepository.save(saved);
        }

        return entityToDto(saved);
    }


    // ==========================================================
    // MULTIPART: profile image
    // ==========================================================
    @Override
    @Transactional
    @Caching(
            put = @CachePut(cacheNames = "student", key = "#studentId"),
            evict = @CacheEvict(cacheNames = "studentSearch", allEntries = true)
    )
    public StudentResponseDto uploadProfileImage(Long studentId, MultipartFile file) {

        log.info("Uploading profile image for student {}", studentId);

        Student student = findStudent(studentId);
        String oldImage = student.getProfileImageName();

        String newImage = fileStorageService.store(file, studentId);
        student.setProfileImageName(newImage);
        Student saved = studentRepository.save(student);

        fileStorageService.delete(oldImage);   // replace = remove the previous file

        return entityToDto(saved);
    }


    @Override
    public ProfileImageData getProfileImage(Long studentId) {

        Student student = findStudent(studentId);
        String imageName = student.getProfileImageName();

        if (imageName == null) {
            throw new ResourceNotFoundException("Student " + studentId + " has no profile image");
        }

        Resource resource = fileStorageService.load(imageName);
        return new ProfileImageData(resource, fileStorageService.contentTypeOf(imageName), imageName);
    }


    @Override
    @Transactional
    @Caching(
            put = @CachePut(cacheNames = "student", key = "#studentId"),
            evict = @CacheEvict(cacheNames = "studentSearch", allEntries = true)
    )
    public StudentResponseDto deleteProfileImage(Long studentId) {

        log.info("Deleting profile image of student {}", studentId);

        Student student = findStudent(studentId);
        String imageName = student.getProfileImageName();

        if (imageName == null) {
            throw new BadRequestException("Student " + studentId + " has no profile image to delete");
        }

        student.setProfileImageName(null);
        Student saved = studentRepository.save(student);
        fileStorageService.delete(imageName);

        return entityToDto(saved);
    }


    // HELPER METHODS

    // Helper method to find student
    public Student findStudent(Long id) {

        log.debug("Searching student with ID: {}", id);

        return studentRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Student not found with ID: {}", id);

                    return new ResourceNotFoundException(
                            "Student not found : " + id
                    );
                });
    }

    private Department findDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found : " + id));
    }


    // Helper method: DTO → Entity
    private Student dtoToEntity(StudentRequestDto studentRequestDto) {

        log.debug("Converting StudentRequestDto to Student entity");

        return Student.builder()
                .firstName(studentRequestDto.getFirstName())
                .lastName(studentRequestDto.getLastName())
                .email(studentRequestDto.getEmail())
                .phoneNumber(studentRequestDto.getPhoneNumber())
                .dateOfBirth(studentRequestDto.getDateOfBirth())
                .build();
    }


    // Helper method: Entity → DTO
    private StudentResponseDto entityToDto(Student student) {

        log.debug("Converting Student entity to StudentResponseDto");

        Department department = student.getDepartment();

        List<CourseSummaryDto> courses = student.getCourses().stream()
                .sorted(Comparator.comparing(Course::getCourseCode))
                .map(c -> CourseSummaryDto.builder()
                        .id(c.getId())
                        .courseCode(c.getCourseCode())
                        .title(c.getTitle())
                        .credits(c.getCredits())
                        .build())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        return StudentResponseDto.builder()
                .id(student.getId())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .email(student.getEmail())
                .phoneNumber(student.getPhoneNumber())
                .dateOfBirth(student.getDateOfBirth())
                .profileImageUrl(student.getProfileImageName() == null
                        ? null
                        : "/api/students/" + student.getId() + "/profile-image")
                .department(department == null ? null : DepartmentSummaryDto.builder()
                        .id(department.getId())
                        .name(department.getName())
                        .code(department.getCode())
                        .build())
                .courses(courses)
                .build();
    }
}
