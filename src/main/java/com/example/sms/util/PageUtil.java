package com.example.sms.util;

import com.example.sms.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.TreeSet;

/**
 * Builds a safe Pageable.
 * Only whitelisted sort fields are accepted, so a caller can never sort by
 * (or probe) arbitrary entity properties such as "password".
 */
public final class PageUtil {

    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 100;

    private PageUtil() {
    }

    /**
     * @param allowedSortFields public sort name -> entity property path
     * @param defaultSortField  public sort name used when the caller sends none
     */
    public static Pageable build(int page, int size, String sortBy, String direction,
                                 Map<String, String> allowedSortFields, String defaultSortField) {

        int safePage = Math.max(page, 0);
        int safeSize = size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        String key = (sortBy == null || sortBy.isBlank()) ? defaultSortField : sortBy.trim();
        String property = allowedSortFields.get(key);
        if (property == null) {
            throw new BadRequestException(
                    "Invalid sortBy '" + key + "'. Allowed values: " + new TreeSet<>(allowedSortFields.keySet()));
        }

        String dir = direction == null ? "asc" : direction.trim().toLowerCase();
        if (!dir.equals("asc") && !dir.equals("desc")) {
            throw new BadRequestException("Invalid direction '" + direction + "'. Use 'asc' or 'desc'");
        }

        Sort sort = Sort.by(dir.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, property);
        if (!"id".equals(property)) {
            // tie-breaker so pages never overlap when many rows share the same sort value
            sort = sort.and(Sort.by(Sort.Direction.ASC, "id"));
        }
        return PageRequest.of(safePage, safeSize, sort);
    }
}
