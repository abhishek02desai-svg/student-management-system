package com.example.sms.controller;

import com.example.sms.dto.PageResponse;
import com.example.sms.dto.ProfileImageData;
import com.example.sms.dto.StudentPatchRequestDto;
import com.example.sms.dto.StudentRequestDto;
import com.example.sms.dto.StudentResponseDto;
import com.example.sms.dto.StudentSearchCriteria;
import com.example.sms.enums.Role;
import com.example.sms.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/students")
@Tag(name = "Students")
public class StudentController {

    private final StudentService studentService;

    @Autowired
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping("/create")
    public ResponseEntity<StudentResponseDto> createStudent(
            @Valid @RequestBody StudentRequestDto studentRequestDto) {

        log.info("Creating new student with email: {}", studentRequestDto.getEmail());

        StudentResponseDto response = studentService.createStudent(studentRequestDto);

        log.info("Student created successfully with ID: {}", response.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping("/getAll")
    public ResponseEntity<List<StudentResponseDto>> getAllStudents() {

        log.info("Fetching all students");

        List<StudentResponseDto> students = studentService.getAllStudents();

        log.info("Successfully fetched {} students", students.size());

        return ResponseEntity.ok(students);
    }


    @PreAuthorize("hasRole('ADMIN') or @authz.isSelf(authentication, #id)")
    @GetMapping("/{id}")
    public ResponseEntity<StudentResponseDto> getStudentById(
            @PathVariable Long id) {

        log.info("Fetching student with ID: {}", id);

        StudentResponseDto student = studentService.getStudentById(id);

        log.info("Student found with ID: {}", id);

        return ResponseEntity.ok(student);
    }


    @PreAuthorize("hasRole('ADMIN') or @authz.isSelf(authentication, #id)")
    @PutMapping("/update/{id}")
    public ResponseEntity<StudentResponseDto> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequestDto studentRequestDto) {

        log.info("Updating student with ID: {}", id);

        StudentResponseDto response =
                studentService.updateStudent(id, studentRequestDto);

        log.info("Student updated successfully with ID: {}", id);

        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteStudent(
            @PathVariable Long id) {

        log.info("Deleting student with ID: {}", id);

        studentService.deleteStudent(id);

        log.info("Student deleted successfully with ID: {}", id);

        return ResponseEntity.noContent().build();
    }


    @PreAuthorize("hasRole('ADMIN') or @authz.isSelf(authentication, #id)")
    @PatchMapping("/patch/{id}")
    public ResponseEntity<StudentResponseDto> patchStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentPatchRequestDto studentPatchRequestDto) {

        log.info("Partially updating student with ID: {}", id);

        StudentResponseDto response =
                studentService.patchStudent(id, studentPatchRequestDto);

        log.info("Student partially updated successfully with ID: {}", id);

        return ResponseEntity.ok(response);
    }

    // ==========================================================
    // SEARCH + PAGINATION + SORTING
    // ==========================================================
    @Operation(summary = "Student directory: search + filter + sort + page",
            description = "keyword matches name, email or phone. departmentId / courseId are optional filters. "
                    + "sortBy: id | firstName | lastName | email | dateOfBirth | department. "
                    + "direction: asc | desc. page starts at 0, size max 100. "
                    + "Example: /api/students/search?keyword=ra&departmentId=1&sortBy=firstName&direction=asc&page=0&size=5")
    @GetMapping("/search")
    public ResponseEntity<PageResponse<StudentResponseDto>> searchStudents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        log.info("Searching students keyword='{}', departmentId={}, courseId={}, page={}, size={}, sortBy={}, direction={}",
                keyword, departmentId, courseId, page, size, sortBy, direction);

        return ResponseEntity.ok(studentService.searchStudents(
                new StudentSearchCriteria(keyword, departmentId, courseId, page, size, sortBy, direction)));
    }


    // ==========================================================
    // MAPPING: department & courses
    // ==========================================================
    @Operation(summary = "Assign (or move) a student to a department")
    @PutMapping("/{studentId}/department/{departmentId}")
    public ResponseEntity<StudentResponseDto> assignDepartment(
            @PathVariable Long studentId,
            @PathVariable Long departmentId) {

        return ResponseEntity.ok(studentService.assignDepartment(studentId, departmentId));
    }

    @Operation(summary = "Enroll a student in a course")
    @PostMapping("/{studentId}/courses/{courseId}")
    public ResponseEntity<StudentResponseDto> enrollInCourse(
            @PathVariable Long studentId,
            @PathVariable Long courseId) {

        return ResponseEntity.ok(studentService.enrollInCourse(studentId, courseId));
    }

    @Operation(summary = "Remove a student from a course")
    @DeleteMapping("/{studentId}/courses/{courseId}")
    public ResponseEntity<StudentResponseDto> unenrollFromCourse(
            @PathVariable Long studentId,
            @PathVariable Long courseId) {

        return ResponseEntity.ok(studentService.unenrollFromCourse(studentId, courseId));
    }


    // ==========================================================
    // MULTIPART: profile image
    // ==========================================================
    @Operation(summary = "Upload / replace profile image (multipart, JPEG or PNG, max 2 MB)")
    @PreAuthorize("hasRole('ADMIN') or @authz.isSelf(authentication, #id)")
    @PostMapping(value = "/{id}/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentResponseDto> uploadProfileImage(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file) {

        log.info("Uploading profile image for student {} ({} bytes)", id, file.getSize());

        return ResponseEntity.ok(studentService.uploadProfileImage(id, file));
    }

    @Operation(summary = "Download / view the profile image")
    @PreAuthorize("hasRole('ADMIN') or @authz.isSelf(authentication, #id)")
    @GetMapping("/{id}/profile-image")
    public ResponseEntity<Resource> getProfileImage(@PathVariable Long id) {

        ProfileImageData image = studentService.getProfileImage(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.fileName() + "\"")
                .body(image.resource());
    }

    @Operation(summary = "Delete the profile image")
    @PreAuthorize("hasRole('ADMIN') or @authz.isSelf(authentication, #id)")
    @DeleteMapping("/{id}/profile-image")
    public ResponseEntity<StudentResponseDto> deleteProfileImage(@PathVariable Long id) {

        return ResponseEntity.ok(studentService.deleteProfileImage(id));
    }


    // ==========================================================
    // ROLES (ADMIN only - enforced in SecurityConfig)
    // ==========================================================
    @Operation(summary = "Change a student's role (ADMIN only). role = STUDENT or ADMIN")
    @PutMapping("/{id}/role/{role}")
    public ResponseEntity<StudentResponseDto> changeRole(
            @PathVariable Long id,
            @PathVariable Role role) {

        return ResponseEntity.ok(studentService.changeRole(id, role));
    }
}
