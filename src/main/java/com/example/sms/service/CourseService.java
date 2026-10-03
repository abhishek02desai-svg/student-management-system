package com.example.sms.service;

import com.example.sms.dto.CourseRequestDto;
import com.example.sms.dto.CourseResponseDto;
import com.example.sms.dto.CourseSearchCriteria;
import com.example.sms.dto.PageResponse;

public interface CourseService {

    CourseResponseDto createCourse(CourseRequestDto dto);

    CourseResponseDto getCourseById(Long id);

    PageResponse<CourseResponseDto> searchCourses(CourseSearchCriteria criteria);

    CourseResponseDto updateCourse(Long id, CourseRequestDto dto);

    void deleteCourse(Long id);
}
