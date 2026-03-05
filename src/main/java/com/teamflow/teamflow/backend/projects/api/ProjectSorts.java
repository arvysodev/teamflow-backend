package com.teamflow.teamflow.backend.projects.api;

import com.teamflow.teamflow.backend.common.errors.BadRequestException;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class ProjectSorts {
    private ProjectSorts() {
    }

    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "updatedAt",
            "createdAt",
            "name"
    );

    public static Sort parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "updatedAt");
        }

        String[] parts = raw.split(",");
        String field = parts[0].trim();
        String dirRaw = parts.length > 1 ? parts[1].trim() : "desc";

        if (!ALLOWED_FIELDS.contains(field)) {
            throw new BadRequestException("Unsupported sort field: " + field);
        }

        Sort.Direction dir;
        try {
            dir = Sort.Direction.fromString(dirRaw);
        } catch (Exception e) {
            throw new BadRequestException("Unsupported sort direction: " + dirRaw);
        }

        return Sort.by(dir, field);
    }
}
