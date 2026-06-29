package com.omiinqa.reference.catalog;

import com.omiinqa.reference.core.DomainException;

import java.util.List;

/**
 * Generic pagination utility for the reference catalog domain.
 *
 * <p>Pages are 1-based. Requesting a page beyond the last page returns an
 * empty items list (not an error) — callers can detect this via
 * {@link Page#items()} being empty or via {@code !hasPrev &amp;&amp; page > 1}.</p>
 *
 * <p>Error codes (asserted by scenarios):
 * {@code PAGE_BAD_PARAMS} — page &lt; 1 or size &lt; 1.</p>
 */
public class PaginationService {

    /**
     * Paginate any list.
     *
     * @param source full list to paginate (not modified)
     * @param page   1-based page number
     * @param size   number of items per page
     * @param <T>    element type
     * @return a {@link Page} describing the slice
     * @throws DomainException {@code PAGE_BAD_PARAMS} when page &lt; 1 or size &lt; 1
     */
    public <T> Page<T> paginate(final List<T> source, final int page, final int size) {
        if (page < 1 || size < 1) {
            throw new DomainException("PAGE_BAD_PARAMS",
                    "page and size must both be >= 1, got page=" + page + ", size=" + size);
        }
        final int total = source.size();
        final int totalPages = total == 0 ? 1 : (int) Math.ceil((double) total / size);
        final int fromIndex = (page - 1) * size;
        final List<T> items;
        if (fromIndex >= total) {
            items = List.of();
        } else {
            final int toIndex = Math.min(fromIndex + size, total);
            items = List.copyOf(source.subList(fromIndex, toIndex));
        }
        final boolean hasNext = page < totalPages;
        final boolean hasPrev = page > 1;
        return new Page<>(items, page, size, total, totalPages, hasNext, hasPrev);
    }
}
