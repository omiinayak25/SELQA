package com.omiinqa.observability;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ExecutionTimeline} — verifies phase recording, ordering,
 * snapshot immutability, and render output. Fully offline.
 */
@Test(groups = {"observability", "unit"})
public class ExecutionTimelineTest {

    @AfterMethod(alwaysRun = true)
    public void cleanup() {
        ExecutionTimeline.clear();
    }

    // -------------------------------------------------------------------------
    // Basic phase recording
    // -------------------------------------------------------------------------

    @Test(groups = {"observability", "unit"})
    public void snapshotIsEmptyBeforeAnyPhase() {
        assertThat(ExecutionTimeline.snapshot()).isEmpty();
    }

    @Test(groups = {"observability", "unit"})
    public void completedPhaseAppearsInSnapshot() {
        ExecutionTimeline.startPhase("login");
        ExecutionTimeline.stopPhase("login");

        final List<ExecutionTimeline.PhaseEntry> snap = ExecutionTimeline.snapshot();
        assertThat(snap).hasSize(1);
        assertThat(snap.get(0).getName()).isEqualTo("login");
    }

    @Test(groups = {"observability", "unit"})
    public void elapsedNanosIsPositive() {
        ExecutionTimeline.startPhase("page-load");
        ExecutionTimeline.stopPhase("page-load");

        final long nanos = ExecutionTimeline.snapshot().get(0).getElapsedNanos();
        assertThat(nanos).isPositive();
    }

    @Test(groups = {"observability", "unit"})
    public void elapsedMillisDerivedFromNanos() {
        ExecutionTimeline.startPhase("action");
        ExecutionTimeline.stopPhase("action");

        final ExecutionTimeline.PhaseEntry entry = ExecutionTimeline.snapshot().get(0);
        assertThat(entry.getElapsedMillis()).isEqualTo(entry.getElapsedNanos() / 1_000_000L);
    }

    @Test(groups = {"observability", "unit"})
    public void multiplePhasesMaintainInsertionOrder() {
        ExecutionTimeline.startPhase("alpha");
        ExecutionTimeline.stopPhase("alpha");
        ExecutionTimeline.startPhase("beta");
        ExecutionTimeline.stopPhase("beta");
        ExecutionTimeline.startPhase("gamma");
        ExecutionTimeline.stopPhase("gamma");

        final List<ExecutionTimeline.PhaseEntry> snap = ExecutionTimeline.snapshot();
        assertThat(snap).extracting(ExecutionTimeline.PhaseEntry::getName)
                .containsExactly("alpha", "beta", "gamma");
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test(groups = {"observability", "unit"})
    public void stopPhaseWithoutStartIsIgnored() {
        // Must not throw
        ExecutionTimeline.stopPhase("never-started");
        assertThat(ExecutionTimeline.snapshot()).isEmpty();
    }

    @Test(groups = {"observability", "unit"})
    public void snapshotIsImmutable() {
        ExecutionTimeline.startPhase("x");
        ExecutionTimeline.stopPhase("x");
        ExecutionTimeline.startPhase("y");
        ExecutionTimeline.stopPhase("y");

        final List<ExecutionTimeline.PhaseEntry> snap1 = ExecutionTimeline.snapshot();
        final int sizeBefore = snap1.size(); // should be 2

        // Start a new phase AFTER taking the snapshot — it must not appear in the old snapshot
        ExecutionTimeline.startPhase("late");
        ExecutionTimeline.stopPhase("late");

        // The first snapshot is frozen — size must not have changed
        assertThat(snap1).hasSize(sizeBefore);
        // A new snapshot captures all three phases
        assertThat(ExecutionTimeline.snapshot()).hasSize(3);
    }

    @Test(groups = {"observability", "unit"})
    public void clearResetsAllPhases() {
        ExecutionTimeline.startPhase("foo");
        ExecutionTimeline.stopPhase("foo");
        ExecutionTimeline.clear();
        assertThat(ExecutionTimeline.snapshot()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Render tests
    // -------------------------------------------------------------------------

    @Test(groups = {"observability", "unit"})
    public void renderContainsTimeline() {
        ExecutionTimeline.startPhase("driver-init");
        ExecutionTimeline.stopPhase("driver-init");

        final String rendered = ExecutionTimeline.render();
        assertThat(rendered).contains("ExecutionTimeline");
        assertThat(rendered).contains("driver-init");
    }

    @Test(groups = {"observability", "unit"})
    public void renderEmptyTimeline() {
        final String rendered = ExecutionTimeline.render();
        assertThat(rendered).containsIgnoringCase("empty");
    }

    @Test(groups = {"observability", "unit"})
    public void phaseEntryToStringIncludesNameAndMs() {
        ExecutionTimeline.startPhase("my-phase");
        ExecutionTimeline.stopPhase("my-phase");

        final String str = ExecutionTimeline.snapshot().get(0).toString();
        assertThat(str).contains("my-phase").contains("ms");
    }
}
