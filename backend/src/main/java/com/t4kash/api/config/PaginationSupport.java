package com.t4kash.api.config;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public final class PaginationSupport {
    public static final int DEFAULT_SIZE = 50;
    public static final int MAXIMUM_SIZE = 100;

    private PaginationSupport() {
    }

    public static Pageable page(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, MAXIMUM_SIZE));
        return PageRequest.of(safePage, safeSize);
    }
}
