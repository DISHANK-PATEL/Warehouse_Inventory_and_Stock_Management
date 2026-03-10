package com.warehouse.inventory.dto.response;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class PagedResponse<T> {

    private final List<T> content;
    private final PaginationMeta pagination;

    public PagedResponse(Page<T> page) {
        this.content    = page.getContent();
        this.pagination = new PaginationMeta(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Getter
    public static class PaginationMeta {
        private final int     page;
        private final int     size;
        private final long    totalElements;
        private final int     totalPages;
        private final boolean isLast;

        public PaginationMeta(int page, int size, long totalElements, int totalPages, boolean isLast) {
            this.page          = page;
            this.size          = size;
            this.totalElements = totalElements;
            this.totalPages    = totalPages;
            this.isLast        = isLast;
        }
    }

}
