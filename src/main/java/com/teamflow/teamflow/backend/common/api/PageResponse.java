package com.teamflow.teamflow.backend.common.api;

import java.util.List;

public record PageResponse<T>(
    List<T> items,
    PageMeta meta
) {
    public record PageMeta(
            int page,
            int size,
            long totalItems,
            int totalPages,
            boolean hasNext,
            boolean hasPrev
    ) {}
}
