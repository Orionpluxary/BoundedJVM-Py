// =============================================================================
// FILE PURPOSE:
// Immutable structured result record representing the outcome of a JVM script execution.
// This file captures execution status, wall-clock timing, bounds tripped flags, exit codes,
// and captured standard output/error streams for both programmatic and CLI callers.
// Returned by BoundedPyHost.execute() and queried to verify bound compliance.
// =============================================================================

package com.boundedjvm;

public class ExecutionResult {
    private final String status;          // "SUCCESS", "TIMEOUT", "POLICY_VIOLATION", "ERROR"
    private final boolean boundTripped;
    private final long elapsedMs;
    private final int exitCode;
    private final String output;
    private final String error;
    private final String message;

    public ExecutionResult(String status, boolean boundTripped, long elapsedMs, int exitCode, String output, String error, String message) {
        this.status = status;
        this.boundTripped = boundTripped;
        this.elapsedMs = elapsedMs;
        this.exitCode = exitCode;
        this.output = output;
        this.error = error;
        this.message = message;
    }

    public String getStatus() { return status; }
    public boolean isBoundTripped() { return boundTripped; }
    public long getElapsedMs() { return elapsedMs; }
    public int getExitCode() { return exitCode; }
    public String getOutput() { return output; }
    public String getError() { return error; }
    public String getMessage() { return message; }

    @Override
    public String toString() {
        return String.format("ExecutionResult[status=%s, bounds_tripped=%s, elapsed_ms=%d, exitCode=%d, message='%s']",
                status, boundTripped ? "1" : "0", elapsedMs, exitCode, message);
    }
}
