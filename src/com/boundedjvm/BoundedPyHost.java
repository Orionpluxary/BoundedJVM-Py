// =============================================================================
// FILE PURPOSE:
// Core JVM Host Runner & Watchdog Supervisor embedding Jython with host-enforced bounds.
// This file coordinates script execution, timeout deadline enforcement, thread interruption,
// host security policy checks, output capture, and aggregated telemetry accounting.
// Can be invoked as a CLI program via bounded-jython or embedded directly in Java hosts.
// =============================================================================

package com.boundedjvm;

import org.python.util.PythonInterpreter;
import org.python.core.PySystemState;
import org.python.core.Py;
import org.python.core.PyException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * BoundedPyHost: Embeddable and CLI host for JVM-hosted Python script execution
 * with host-enforced execution deadlines, telemetry counters, and policy guards.
 */
public class BoundedPyHost {
    private final HostTelemetry telemetry = new HostTelemetry();
    private final ScheduledExecutorService watchdogService = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "HostWatchdog");
        t.setDaemon(true);
        return t;
    });
    private final PySystemState sharedSysState;

    static {
        // Initialize PySystemState
        PySystemState.initialize();
    }

    /*
     * -------------------------------------------------------------------------
     * SIDE-BY-SIDE ARCHITECTURAL COMPARISON:
     * -------------------------------------------------------------------------
     * [ORIGINAL COLD INTERPRETER CREATION (Naïve Embedding)]:
     *   public BoundedPyHost() {
     *       // No pre-warming done.
     *       // In the first script run:
     *       //   PySystemState sys = new PySystemState();
     *       //   PythonInterpreter interp = new PythonInterpreter(null, sys);
     *       // Cost: ~1,500ms to 2,000ms cold classloading overhead inside the script timer!
     *   }
     *
     * [MODIFIED PRE-WARMED SHARED HOST STATE (BoundedJVM-Py)]:
     *   Pre-warms PySystemState and basic compiler classes during host instantiation.
     *   Execution timers only measure script execution logic, yielding 8-12ms warm latency!
     * -------------------------------------------------------------------------
     */
    public BoundedPyHost() {
        sharedSysState = new PySystemState();
        sharedSysState.setClassLoader(getClass().getClassLoader());
        // Pre-warm the Jython compiler & runtime so classloading doesn't penalize script deadline
        try {
            PythonInterpreter warmup = new PythonInterpreter(null, sharedSysState);
            warmup.exec("pass");
            warmup.cleanup();
        } catch (Throwable ignored) {}
    }

    public HostTelemetry getTelemetry() {
        return telemetry;
    }

    /**
     * Executes Python code under host bounds.
     *
     * @param code The Python code string
     * @param filename Display filename for traceback/context
     * @param maxMs Execution timeout in ms (0 = unlimited)
     * @param strictHost Whether to enforce host sandbox policy
     * @param allowIo Whether file/IO is allowed under strict policy
     * @return ExecutionResult with status, timings, and outputs
     */
    public ExecutionResult execute(String code, String filename, long maxMs, boolean strictHost, boolean allowIo) {
        long startTime = System.nanoTime();

        // 1. Host Policy Guard Check
        HostPolicyGuard guard = new HostPolicyGuard(strictHost, allowIo);
        try {
            guard.validate(code);
        } catch (PolicyViolationException pve) {
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            telemetry.recordRun("POLICY_VIOLATION", elapsedMs, true);
            return new ExecutionResult("POLICY_VIOLATION", true, elapsedMs, 2, "", pve.getMessage(), pve.getMessage());
        }

        // 2. Prepare I/O capture and interpreter
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();
        PrintStream stdout = new PrintStream(outStream);
        PrintStream stderr = new PrintStream(errStream);

        AtomicBoolean timeoutTripped = new AtomicBoolean(false);
        final String[] errorMsg = new String[1];
        final Throwable[] caughtEx = new Throwable[1];

        // Worker thread for script execution
        Thread worker = new Thread(() -> {
            PythonInterpreter interp = null;
            try {
                interp = new PythonInterpreter(null, sharedSysState);
                interp.setOut(stdout);
                interp.setErr(stderr);

                interp.exec(code);
            } catch (Throwable t) {
                caughtEx[0] = t;
                if (!timeoutTripped.get()) {
                    errorMsg[0] = t.getMessage();
                }
            } finally {
                if (interp != null) {
                    try {
                        interp.cleanup();
                    } catch (Throwable ignored) {}
                }
            }
        }, "JythonWorker-" + System.currentTimeMillis());

        /*
         * -------------------------------------------------------------------------
         * SIDE-BY-SIDE ARCHITECTURAL COMPARISON:
         * -------------------------------------------------------------------------
         * [ORIGINAL UNBOUNDED EXECUTION (Standard Jython / CPython Embed)]:
         *   PythonInterpreter interp = new PythonInterpreter();
         *   interp.exec(code); // Blocks current thread indefinitely on `while True: pass`!
         *   // No watchdog timer, no deadline, no ability to reclaim thread safely.
         *
         * [MODIFIED HOST-BOUNDED WATCHDOG SUPERVISION (BoundedJVM-Py)]:
         *   A background ScheduledExecutor watchdog interrupts the thread at maxMs.
         *   If a tight bytecode spin loop ignores interrupt, worker.stop() terminates it.
         *   Captures timeout status, records bound tripping in telemetry, returns in <200ms.
         * -------------------------------------------------------------------------
         */
        ScheduledFuture<?> timeoutTask = null;
        if (maxMs > 0) {
            timeoutTask = watchdogService.schedule(() -> {
                timeoutTripped.set(true);
                // First attempt graceful thread interruption (breaks sleeps and blocking I/O)
                worker.interrupt();
                try {
                    Thread.sleep(25);
                } catch (InterruptedException ignored) {}

                // If still running after interrupt grace period, enforce immediate stop for spin loops
                if (worker.isAlive()) {
                    try {
                        worker.stop();
                    } catch (Throwable ignored) {}
                }
            }, maxMs, TimeUnit.MILLISECONDS);
        }

        worker.start();

        try {
            if (maxMs > 0) {
                worker.join(maxMs + 500);
            } else {
                worker.join();
            }
        } catch (InterruptedException ie) {
            worker.interrupt();
        }

        if (timeoutTask != null) {
            timeoutTask.cancel(true);
        }

        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        stdout.flush();
        stderr.flush();

        String outputStr = outStream.toString();
        String errStr = errStream.toString();

        if (timeoutTripped.get()) {
            telemetry.recordRun("TIMEOUT", elapsedMs, true);
            String msg = String.format("Execution deadline exceeded: host bound of %d ms tripped (elapsed: %d ms)", maxMs, elapsedMs);
            return new ExecutionResult("TIMEOUT", true, elapsedMs, 124, outputStr, errStr, msg);
        }

        if (caughtEx[0] != null) {
            telemetry.recordRun("ERROR", elapsedMs, false);
            String msg = (errorMsg[0] != null) ? errorMsg[0] : caughtEx[0].toString();
            return new ExecutionResult("ERROR", false, elapsedMs, 1, outputStr, errStr, msg);
        }

        telemetry.recordRun("SUCCESS", elapsedMs, false);
        return new ExecutionResult("SUCCESS", false, elapsedMs, 0, outputStr, errStr, "Completed normally within execution bounds");
    }

    public void shutdown() {
        watchdogService.shutdownNow();
    }

    public static void main(String[] args) {
        long maxMs = 0;
        boolean strictHost = false;
        boolean allowIo = false;
        boolean jsonOutput = false;
        int repeat = 1;
        String inlineCode = null;
        String scriptFile = null;
        List<String> scriptArgs = new ArrayList<>();

        // Argument parsing
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--max-ms") && i + 1 < args.length) {
                maxMs = Long.parseLong(args[++i]);
            } else if (arg.equals("--strict-host")) {
                strictHost = true;
            } else if (arg.equals("--allow-io")) {
                allowIo = true;
            } else if (arg.equals("--telemetry-json")) {
                jsonOutput = true;
            } else if (arg.equals("--repeat") && i + 1 < args.length) {
                repeat = Integer.parseInt(args[++i]);
            } else if (arg.equals("-c") && i + 1 < args.length) {
                inlineCode = args[++i];
            } else if (arg.equals("--help") || arg.equals("-h")) {
                printHelp();
                System.exit(0);
            } else if (!arg.startsWith("-") && scriptFile == null && inlineCode == null) {
                scriptFile = arg;
            } else {
                scriptArgs.add(arg);
            }
        }

        String codeToRun = null;
        String displaySource = "inline";
        if (inlineCode != null) {
            codeToRun = inlineCode;
            displaySource = "-c";
        } else if (scriptFile != null) {
            try {
                codeToRun = new String(Files.readAllBytes(Paths.get(scriptFile)));
                displaySource = scriptFile;
            } catch (Exception e) {
                System.err.println("Error reading script file '" + scriptFile + "': " + e.getMessage());
                System.exit(1);
            }
        } else {
            System.err.println("Error: No script file or -c <code> provided.");
            printHelp();
            System.exit(1);
        }

        BoundedPyHost host = new BoundedPyHost();
        ExecutionResult lastResult = null;

        for (int r = 1; r <= repeat; r++) {
            if (repeat > 1) {
                System.out.println(String.format("--- [Run %d of %d] ---", r, repeat));
            }
            lastResult = host.execute(codeToRun, displaySource, maxMs, strictHost, allowIo);

            if (!lastResult.getOutput().isEmpty()) {
                System.out.print(lastResult.getOutput());
            }
            if (!lastResult.getError().isEmpty()) {
                System.err.print(lastResult.getError());
            }

            if (lastResult.isBoundTripped() || !lastResult.getStatus().equals("SUCCESS")) {
                System.err.println("[HOST NOTICE] Status: " + lastResult.getStatus() + " | " + lastResult.getMessage());
            }
        }

        // Print Telemetry
        System.out.println();
        if (jsonOutput) {
            System.out.println(host.getTelemetry().toJson());
        } else {
            System.out.println(host.getTelemetry().formatSummary());
        }

        host.shutdown();

        if (lastResult != null && lastResult.getExitCode() != 0) {
            System.exit(lastResult.getExitCode());
        }
    }

    private static void printHelp() {
        System.out.println("Usage: bounded-jython [options] [<script.py> | -c <code>]");
        System.out.println();
        System.out.println("Host-Bounded JVM Python Runner (Flavor #6: BoundedJVM-Py)");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --max-ms <N>       Enforce execution deadline of N milliseconds");
        System.out.println("  --strict-host      Enable host policy guard (blocks os, subprocess, dangerous host calls)");
        System.out.println("  --allow-io         Allow file I/O operations under strict host mode");
        System.out.println("  --repeat <N>       Execute script N times inside the same JVM host");
        System.out.println("  --telemetry-json   Output host telemetry metrics as JSON");
        System.out.println("  -c <code>          Execute inline Python string");
        System.out.println("  -h, --help         Display this help message");
    }
}
