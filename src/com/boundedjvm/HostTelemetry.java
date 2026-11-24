// =============================================================================
// FILE PURPOSE:
// Thread-safe telemetry accumulator for host execution bounds, run counts, and latencies.
// This file tracks cumulative statistics (total runs, tripped limits, average execution time)
// across repeated invocations inside the same JVM host instance without process restart.
// Exported as formatted ASCII tables or JSON payloads for observability platforms.
// =============================================================================

package com.boundedjvm;

import java.util.concurrent.atomic.AtomicLong;

public class HostTelemetry {
    private final AtomicLong runsExecuted = new AtomicLong(0);
    private final AtomicLong boundsTripped = new AtomicLong(0);
    private final AtomicLong totalElapsedMs = new AtomicLong(0);
    private volatile String lastStatus = "NONE";
    private volatile long lastElapsedMs = 0;

    /*
     * -------------------------------------------------------------------------
     * SIDE-BY-SIDE ARCHITECTURAL COMPARISON:
     * -------------------------------------------------------------------------
     * ORIGINAL UNHOSTED EXECUTION (Standalone CPython / CLI Jython):
     *   - No host-side telemetry counters exist.
     *   - The OS process exits and discards all memory state.
     *   - Metrics require external wrappers like /usr/bin/time or systemd cgroups.
     *
     * MODIFIED JVM-HOSTED TELEMETRY (BoundedJVM-Py):
     *   - Host keeps atomic state across all script invocations in the JVM lifecycle.
     *   - Direct visibility into bounds tripped, run latencies, and violation rates.
     * -------------------------------------------------------------------------
     */
    public void recordRun(String status, long elapsedMs, boolean tripped) {
        runsExecuted.incrementAndGet();
        if (tripped) {
            boundsTripped.incrementAndGet();
        }
        totalElapsedMs.addAndGet(elapsedMs);
        lastStatus = status;
        lastElapsedMs = elapsedMs;
    }

    public long getRunsExecuted() { return runsExecuted.get(); }
    public long getBoundsTripped() { return boundsTripped.get(); }
    public long getTotalElapsedMs() { return totalElapsedMs.get(); }
    public String getLastStatus() { return lastStatus; }
    public long getLastElapsedMs() { return lastElapsedMs; }

    public double getAverageElapsedMs() {
        long runs = runsExecuted.get();
        return runs == 0 ? 0.0 : (double) totalElapsedMs.get() / runs;
    }

    public String formatSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=================== HOST TELEMETRY & BOUND COUNTERS ===================\n");
        sb.append(String.format("  runs_executed       : %d\n", runsExecuted.get()));
        sb.append(String.format("  bounds_tripped      : %d\n", boundsTripped.get()));
        sb.append(String.format("  last_run_elapsed_ms : %d ms\n", lastElapsedMs));
        sb.append(String.format("  total_elapsed_ms    : %d ms\n", totalElapsedMs.get()));
        sb.append(String.format("  avg_elapsed_ms      : %.2f ms\n", getAverageElapsedMs()));
        sb.append(String.format("  last_exit_status    : %s\n", lastStatus));
        sb.append("=======================================================================");
        return sb.toString();
    }

    public String toJson() {
        return String.format(
            "{\"runs_executed\": %d, \"bounds_tripped\": %d, \"last_elapsed_ms\": %d, \"total_elapsed_ms\": %d, \"avg_elapsed_ms\": %.2f, \"last_status\": \"%s\"}",
            runsExecuted.get(), boundsTripped.get(), lastElapsedMs, totalElapsedMs.get(), getAverageElapsedMs(), lastStatus
        );
    }
}
