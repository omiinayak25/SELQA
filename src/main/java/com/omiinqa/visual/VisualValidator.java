package com.omiinqa.visual;

import com.omiinqa.exceptions.FrameworkException;
import com.omiinqa.utils.ScreenshotUtils;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Lightweight visual-regression engine: capture a screenshot, compare it pixel
 * by pixel against a stored baseline within a tolerance, and emit a diff image.
 *
 * <p>No external service required — uses {@code javax.imageio}. Dynamic regions
 * (clocks, ads, random data) can be masked via ignore-rectangles so they don't
 * produce false diffs. On first run for a key the current screenshot becomes the
 * baseline and the comparison passes (baseline-created), matching how visual
 * tools bootstrap.</p>
 *
 * <p>Baselines live under {@code src/test/resources/visual/baseline}; actuals and
 * diffs under {@code visual-output/} (git-ignored).</p>
 */
public final class VisualValidator {

    private static final Logger LOG = LoggerFactory.getLogger(VisualValidator.class);

    private static final Path BASELINE_DIR =
            Paths.get("src/test/resources/visual/baseline");
    private static final Path OUTPUT_DIR = Paths.get("visual-output");

    /** Per-pixel RGB distance under which two pixels are considered equal. */
    private static final int PIXEL_TOLERANCE = 30;

    private final String key;
    private final List<Rectangle> ignoreRegions = new ArrayList<>();
    private double allowedDiffRatio = 0.005; // 0.5% of pixels may differ

    private VisualValidator(final String key) {
        this.key = key.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public static VisualValidator forKey(final String key) {
        return new VisualValidator(key);
    }

    public VisualValidator ignoreRegion(final int x, final int y, final int w, final int h) {
        ignoreRegions.add(new Rectangle(x, y, w, h));
        return this;
    }

    public VisualValidator tolerance(final double ratio) {
        this.allowedDiffRatio = ratio;
        return this;
    }

    /** Capture the current page and compare against the baseline for this key. */
    public VisualComparisonResult capture(final WebDriver driver) {
        final byte[] png = ScreenshotUtils.captureBytes(driver);
        if (png.length == 0) {
            throw new FrameworkException("Visual capture failed: empty screenshot for " + key);
        }
        try {
            final BufferedImage actual = ImageIO.read(new ByteArrayInputStream(png));
            final Path baselineFile = BASELINE_DIR.resolve(key + ".png");

            if (!Files.exists(baselineFile)) {
                Files.createDirectories(BASELINE_DIR);
                ImageIO.write(actual, "png", baselineFile.toFile());
                LOG.info("Visual baseline created for '{}' at {}", key, baselineFile);
                return new VisualComparisonResult(true, true, 0.0, null);
            }

            final BufferedImage baseline = ImageIO.read(baselineFile.toFile());
            return compare(baseline, actual);
        } catch (final IOException e) {
            throw new FrameworkException("Visual comparison I/O failure for " + key, e);
        }
    }

    private VisualComparisonResult compare(final BufferedImage baseline,
                                           final BufferedImage actual) throws IOException {
        final int w = Math.min(baseline.getWidth(), actual.getWidth());
        final int h = Math.min(baseline.getHeight(), actual.getHeight());
        final BufferedImage diff = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        long different = 0;
        final long total = (long) w * h;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (isIgnored(x, y)) {
                    diff.setRGB(x, y, baseline.getRGB(x, y));
                    continue;
                }
                if (pixelsDiffer(baseline.getRGB(x, y), actual.getRGB(x, y))) {
                    different++;
                    diff.setRGB(x, y, 0xFF0000);
                } else {
                    diff.setRGB(x, y, baseline.getRGB(x, y));
                }
            }
        }

        final double ratio = total == 0 ? 1.0 : (double) different / total;
        final boolean matched = ratio <= allowedDiffRatio;

        Path diffPath = null;
        if (!matched) {
            Files.createDirectories(OUTPUT_DIR);
            diffPath = OUTPUT_DIR.resolve(key + "-diff.png");
            ImageIO.write(diff, "png", diffPath.toFile());
            LOG.warn("Visual diff for '{}': {}% pixels differ (>{}%) -> {}",
                    key, String.format("%.3f", ratio * 100),
                    String.format("%.3f", allowedDiffRatio * 100), diffPath);
        }
        return new VisualComparisonResult(false, matched, ratio, diffPath);
    }

    private boolean isIgnored(final int x, final int y) {
        for (final Rectangle r : ignoreRegions) {
            if (r.contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    private boolean pixelsDiffer(final int rgb1, final int rgb2) {
        final int dr = Math.abs(((rgb1 >> 16) & 0xFF) - ((rgb2 >> 16) & 0xFF));
        final int dg = Math.abs(((rgb1 >> 8) & 0xFF) - ((rgb2 >> 8) & 0xFF));
        final int db = Math.abs((rgb1 & 0xFF) - (rgb2 & 0xFF));
        return (dr + dg + db) > PIXEL_TOLERANCE;
    }
}
