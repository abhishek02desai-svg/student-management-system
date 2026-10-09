package com.example.sms.service;

import com.example.sms.dto.PageResponse;
import com.example.sms.dto.ProfileImageData;
import com.example.sms.dto.RegisterRequestDto;
import com.example.sms.dto.StudentPatchRequestDto;
import com.example.sms.dto.StudentRequestDto;
import com.example.sms.dto.StudentResponseDto;
import com.example.sms.dto.StudentSearchCriteria;
import com.example.sms.enums.Role;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StudentService {

    StudentResponseDto createStudent(StudentRequestDto studentRequestDto);

    StudentResponseDto getStudentById(Long id);

    List<StudentResponseDto> getAllStudents();

    StudentResponseDto updateStudent(Long id, StudentRequestDto studentRequestDto);

    StudentResponseDto patchStudent(Long id, StudentPatchRequestDto studentPatchRequestDto);

    void deleteStudent(Long id);

    // ---- search / pagination / sorting ----
    PageResponse<StudentResponseDto> searchStudents(StudentSearchCriteria criteria);

    // ---- mapping: department & courses ----
    StudentResponseDto assignDepartment(Long studentId, Long departmentId);

    StudentResponseDto enrollInCourse(Long studentId, Long courseId);

    StudentResponseDto unenrollFromCourse(Long studentId, Long courseId);

    // ---- roles (ADMIN only) ----
    StudentResponseDto changeRole(Long studentId, Role role);

    // ---- authentication support ----
    // photo is optional (null or empty = no photo). Student and photo are saved together:
    // if the photo is invalid, nothing is saved.
    StudentResponseDto registerStudent(RegisterRequestDto dto, String encodedPassword, MultipartFile photo);

    // ---- multipart: profile image ----
    StudentResponseDto uploadProfileImage(Long studentId, MultipartFile file);

    ProfileImageData getProfileImage(Long studentId);

    StudentResponseDto deleteProfileImage(Long studentId);
}
