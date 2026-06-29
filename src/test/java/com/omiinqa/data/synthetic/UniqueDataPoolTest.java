package com.omiinqa.data.synthetic;

import com.omiinqa.data.synthetic.SyntheticDataGenerator.UniqueDataPool;
import com.omiinqa.exceptions.DataException;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Offline unit tests for {@link UniqueDataPool}.
 *
 * <p>Verifies uniqueness guarantees, size tracking, reset behaviour, and
 * the guard-rail that throws when a unique value cannot be found.</p>
 *
 * <p>Test groups: {@code data}, {@code unit}.</p>
 */
public class UniqueDataPoolTest {

    @Test(groups = {"data", "unit"},
          description = "UniqueDataPool.next() never returns the same value twice within a run")
    public void nextReturnsUniqueValues() {
        SyntheticDataGenerator gen = new SyntheticDataGenerator(999L);
        UniqueDataPool<String> pool = new UniqueDataPool<>(gen.faker()::randomEmail);

        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            String value = pool.next();
            assertThat(seen.add(value))
                    .as("Value '%s' at iteration %d was already seen", value, i)
                    .isTrue();
        }
    }

    @Test(groups = {"data", "unit"},
          description = "UniqueDataPool.size() increments after each call to next()")
    public void sizeIncrementsAfterEachNext() {
        UniqueDataPool<Integer> pool = new UniqueDataPool<>(new AtomicInteger(0)::getAndIncrement);
        assertThat(pool.size()).isZero();
        pool.next();
        assertThat(pool.size()).isEqualTo(1);
        pool.next();
        assertThat(pool.size()).isEqualTo(2);
    }

    @Test(groups = {"data", "unit"},
          description = "UniqueDataPool.reset() clears the seen set, allowing previously-returned values again")
    public void resetClearsSeenSet() {
        AtomicInteger counter = new AtomicInteger(0);
        UniqueDataPool<Integer> pool = new UniqueDataPool<>(counter::getAndIncrement);
        pool.next(); // 0
        pool.next(); // 1
        assertThat(pool.size()).isEqualTo(2);

        pool.reset();
        assertThat(pool.size()).isZero();
    }

    @Test(groups = {"data", "unit"},
          description = "UniqueDataPool throws DataException when supplier cannot produce a unique value within MAX_RETRIES")
    public void nextThrowsDataExceptionWhenUniqueValueExhausted() {
        // A supplier that always returns the same value — will exhaust retries
        UniqueDataPool<String> pool = new UniqueDataPool<>(() -> "constant");
        pool.next(); // first call succeeds

        // Second call must fail — "constant" is already seen and supplier never varies
        assertThatThrownBy(pool::next)
                .isInstanceOf(DataException.class)
                .hasMessageContaining("unique");
    }
}
