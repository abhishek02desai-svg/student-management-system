package com.example.sms.dto;

import com.example.sms.exception.BadRequestException;

import java.io.Serializable;
import java.util.Locale;

/**
 * All inputs of the student directory search.
 * The compact constructor cleans the values so equal searches produce the same cache key.
 */
public record StudentSearchCriteria(
        String keyword,
        Long departmentId,
        Long courseId,
        int page,
        int size,
        String sortBy,
        String direction
) implements Serializable {

    public StudentSearchCriteria {
        keyword = keyword == null ? "" : keyword.trim();
        if (keyword.length() > 100) {
            throw new BadRequestException("Search keyword must be up to 100 characters");
        }
        page = Math.max(page, 0);
        size = size < 1 ? 10 : Math.min(size, 100);
        sortBy = sortBy == null ? "" : sortBy.trim();
        direction = direction == null ? "asc" : direction.trim().toLowerCase(Locale.ROOT);
    }

    public String cacheKey() {
        return String.join("|",
                keyword.toLowerCase(Locale.ROOT),
                String.valueOf(departmentId),
                String.valueOf(courseId),
                String.valueOf(page),
                String.valueOf(size),
                sortBy,
                direction);
    }
}
