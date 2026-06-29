package com.omiinqa.visual;

import java.nio.file.Path;

/**
 * Outcome of a visual baseline comparison.
 *
 * @param baselineCreated true when this run created a missing baseline (first run)
 * @param matched         true when the diff ratio was within tolerance
 * @param diffRatio       fraction of compared pixels that differed (0.0–1.0)
 * @param diffImage       path to the highlighted diff image (null if not generated)
 */
public record VisualComparisonResult(boolean baselineCreated,
                                     boolean matched,
                                     double diffRatio,
                                     Path diffImage) {
}
