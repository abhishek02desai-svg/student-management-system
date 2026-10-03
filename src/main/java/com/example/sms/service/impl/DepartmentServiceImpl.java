package com.example.sms.service.impl;

import com.example.sms.dto.DepartmentRequestDto;
import com.example.sms.dto.DepartmentResponseDto;
import com.example.sms.dto.DepartmentSearchCriteria;
import com.example.sms.dto.PageResponse;
import com.example.sms.entity.Department;
import com.example.sms.exception.ConflictException;
import com.example.sms.exception.DuplicateResourceException;
import com.example.sms.exception.ResourceNotFoundException;
import com.example.sms.repository.CourseRepository;
import com.example.sms.repository.DepartmentRepository;
import com.example.sms.repository.StudentRepository;
import com.example.sms.service.DepartmentService;
import com.example.sms.util.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentServiceImpl implements DepartmentService {

    // public sort name -> entity property
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "id",
            "name", "name",
            "code", "code"
    );

    private final DepartmentRepository departmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    @Override
    @Transactional
    @Caching(
            put = @CachePut(cacheNames = "department", key = "#result.id"),
            evict = @CacheEvict(cacheNames = "departmentSearch", allEntries = true)
    )
    public DepartmentResponseDto createDepartment(DepartmentRequestDto dto) {

        log.info("Creating department with code: {}", dto.getCode());

        if (departmentRepository.existsByNameIgnoreCase(dto.getName().trim())) {
            throw new DuplicateResourceException("Department already exists with name : " + dto.getName());
        }
        if (departmentRepository.existsByCodeIgnoreCase(dto.getCode().trim())) {
            throw new DuplicateResourceException("Department already exists with code : " + dto.getCode());
        }

        Department saved = departmentRepository.save(
                Department.builder()
                        .name(dto.getName().trim())
                        .code(dto.getCode().trim().toUpperCase(Locale.ROOT))
                        .description(dto.getDescription())
                        .build());

        return toDto(saved);
    }

    @Override
    @Cacheable(cacheNames = "department", key = "#id")
    public DepartmentResponseDto getDepartmentById(Long id) {
        return toDto(findDepartment(id));
    }

    @Override
    @Cacheable(cacheNames = "departmentSearch", key = "#criteria.cacheKey()")
    public PageResponse<DepartmentResponseDto> searchDepartments(DepartmentSearchCriteria criteria) {

        log.info("Searching departments: {}", criteria);

        Pageable pageable = PageUtil.build(criteria.page(), criteria.size(),
                criteria.sortBy(), criteria.direction(), SORT_FIELDS, "name");

        // empty keyword => LIKE '%%' => everything
        Page<DepartmentResponseDto> page = departmentRepository
                .findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
                        criteria.keyword(), criteria.keyword(), pageable)
                .map(this::toDto);

        return PageResponse.from(page);
    }

    // A department name/code is shown inside cached student and course responses,
    // so those caches must be cleared as well.
    @Override
    @Transactional
    @Caching(
            put = @CachePut(cacheNames = "department", key = "#id"),
            evict = {
                    @CacheEvict(cacheNames = "departmentSearch", allEntries = true),
                    @CacheEvict(cacheNames = {"student", "studentSearch", "course", "courseSearch"}, allEntries = true)
            }
    )
    public DepartmentResponseDto updateDepartment(Long id, DepartmentRequestDto dto) {

        log.info("Updating department with ID: {}", id);

        Department department = findDepartment(id);

        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(dto.getName().trim(), id)) {
            throw new DuplicateResourceException("Another department already exists with name : " + dto.getName());
        }
        if (departmentRepository.existsByCodeIgnoreCaseAndIdNot(dto.getCode().trim(), id)) {
            throw new DuplicateResourceException("Another department already exists with code : " + dto.getCode());
        }

        department.setName(dto.getName().trim());
        department.setCode(dto.getCode().trim().toUpperCase(Locale.ROOT));
        department.setDescription(dto.getDescription());

        return toDto(departmentRepository.save(department));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "department", key = "#id"),
            @CacheEvict(cacheNames = "departmentSearch", allEntries = true)
    })
    public void deleteDepartment(Long id) {

        log.info("Deleting department with ID: {}", id);

        Department department = findDepartment(id);

        if (studentRepository.existsByDepartmentId(id)) {
            throw new ConflictException("Cannot delete department: students are still assigned to it");
        }
        if (courseRepository.existsByDepartmentId(id)) {
            throw new ConflictException("Cannot delete department: it still has courses");
        }

        departmentRepository.delete(department);
    }

    private Department findDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found : " + id));
    }

    private DepartmentResponseDto toDto(Department d) {
        return DepartmentResponseDto.builder()
                .id(d.getId())
                .name(d.getName())
                .code(d.getCode())
                .description(d.getDescription())
                .build();
    }
}
