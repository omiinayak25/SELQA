package com.omiinqa.reference.catalog;

import java.util.List;

/**
 * A single page of results returned by {@link PaginationService}.
 *
 * @param <T>        element type
 * @param items      elements on this page (may be empty when page is beyond range)
 * @param page       1-based page number requested
 * @param size       requested page size
 * @param total      total number of elements across all pages
 * @param totalPages total number of pages (ceil(total / size), min 1)
 * @param hasNext    true when a next page exists
 * @param hasPrev    true when a previous page exists
 */
public record Page<T>(
        List<T> items,
        int page,
        int size,
        int total,
        int totalPages,
        boolean hasNext,
        boolean hasPrev) {
}
