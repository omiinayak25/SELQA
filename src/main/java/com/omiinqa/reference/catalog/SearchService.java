package com.omiinqa.reference.catalog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Full-text search over the in-memory product catalog.
 *
 * <p>Searches are case-insensitive over {@code name} and {@code tags}.
 * Relevance ranking: name-match scores 2, tag-match scores 1; products are
 * returned highest-score-first, ties broken by id (ascending).</p>
 *
 * <p>Design choice: an empty query string returns ALL products ordered by id
 * (no error). This is a deliberate product decision — callers that want to
 * reject empty queries should validate before calling. The error code
 * {@code SEARCH_EMPTY_QUERY} is <em>not</em> raised here; it is available
 * for callers that choose to enforce it (documented for scenario coverage).</p>
 *
 * <p>Error codes: none raised internally. Callers may raise
 * {@code SEARCH_EMPTY_QUERY} if they choose to restrict empty queries.</p>
 */
public class SearchService {

    private final CatalogService catalog;

    public SearchService(final CatalogService catalog) {
        this.catalog = catalog;
    }

    /**
     * Search products by query string. Case-insensitive over name and tags.
     * An empty/blank query returns all products ordered by id.
     *
     * @param query search term (blank = return all)
     * @return ranked list — never null, may be empty
     */
    public List<Product> search(final String query) {
        if (query == null || query.isBlank()) {
            final List<Product> all = new ArrayList<>(catalog.all());
            all.sort(Comparator.comparingLong(Product::getId));
            return all;
        }
        final String term = query.strip().toLowerCase();
        final List<ScoredProduct> scored = new ArrayList<>();
        for (final Product p : catalog.all()) {
            int score = 0;
            if (p.getName() != null && p.getName().toLowerCase().contains(term)) {
                score += 2;
            }
            for (final String tag : p.getTags()) {
                if (tag.toLowerCase().contains(term)) {
                    score += 1;
                    break; // count each product's tag-match once
                }
            }
            if (score > 0) {
                scored.add(new ScoredProduct(p, score));
            }
        }
        scored.sort(Comparator
                .comparingInt(ScoredProduct::score).reversed()
                .thenComparingLong(sp -> sp.product().getId()));
        final List<Product> results = new ArrayList<>(scored.size());
        for (final ScoredProduct sp : scored) {
            results.add(sp.product());
        }
        return results;
    }

    private record ScoredProduct(Product product, int score) { }
}
