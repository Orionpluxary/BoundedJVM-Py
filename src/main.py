# =============================================================================
# FILE PURPOSE:
# Central entrypoint and codebase directory map for BoundedJVM-Py (Flavor #6).
# Contains an exhaustive tree map documenting every source file, script, test,
# and example in the project, followed by a runnable quick-start verification demo.
# Run via: bounded-jython src/main.py --max-ms 2000
# =============================================================================

"""
================================================================================
                    BOUNDEDJVM-PY COMPLETE CODEBASE MAP
================================================================================

.
├── README.md                           # Comprehensive architectural guide, benchmarks, & quickstart
├── COMPARISON.md                       # Side-by-side architectural diffs & technical explanations
├── .gitignore                          # Standard ignore patterns for build artifacts, jars, & caches
│
├── src/                                # Core Java Host & Python Entrypoint Sources
│   ├── main.py                         # Central codebase map & runnable quickstart verification
│   └── com/
│       └── boundedjvm/
│           ├── BoundedPyHost.java      # Main JVM host supervisor, watchdog manager, & CLI entrypoint
│           ├── HostTelemetry.java      # Thread-safe atomic counters (runs, bounds tripped, latencies)
│           ├── HostPolicyGuard.java    # AST/Source policy validator for strict sandbox security
│           ├── ExecutionResult.java    # Structured execution record (status, timings, streams, exit code)
│           └── PolicyViolationException.java # Exception thrown upon host security boundary violations
│
├── scripts/                            # Build, Packaging, and Installation Automation
│   ├── build.sh                        # Compiles Java classes, packages JARs, and builds SDK archive
│   ├── install.sh                      # Installs SDK to /opt/bounded-jvm-py and links /usr/local/bin
│   └── run_benchmark.sh                # Measures 10 OS process spawns vs. 10 warm in-host runs
│
├── tests/                              # Automated Verification & Test Suite
│   ├── run_tests.sh                    # End-to-end test runner executing 5 regression scenarios
│   ├── test_normal.py                  # Healthy script completing within 2000ms deadline
│   ├── test_runaway.py                 # Infinite spin loop testing 150ms timeout watchdog
│   ├── test_policy_violation.py        # Forbidden import test testing --strict-host policy guard
│   └── test_bench_embedded.py          # Micro-benchmark loop measuring per-run invocation overhead
│
└── examples/                           # Runnable Practical Demonstrations
    ├── demo_bounded_jvm.py             # Prime number calculation with telemetry output
    ├── timeout_demo.py                 # Runaway spin loop demonstration under --max-ms bound
    ├── strict_sandbox_demo.py          # Host security isolation demonstration under --strict-host
    └── batch_runner_demo.py            # High-throughput batch execution using --repeat 10

================================================================================
"""

import sys
import time

def run_quickstart():
    print("=" * 70)
    print("          BOUNDEDJVM-PY (FLAVOR #6) QUICKSTART DEMO")
    print("=" * 70)
    
    # 1. Environment Detection
    is_jvm = "java" in sys.platform.lower()
    print("[Host Environment] Detected platform : %s" % sys.platform)
    print("[Host Environment] Python Version    : %s" % sys.version.replace('\n', ' '))
    print("[Host Environment] JVM-Hosted Engine : %s" % ("YES (Jython on OpenJDK)" if is_jvm else "NO (Standard Python)"))
    print()

    # 2. Host Capability Summary
    print("[Bounded Capabilities]")
    print("  1. Execution Deadline Watchdog  : Host-enforced via --max-ms <N>")
    print("  2. Host Telemetry & Counters    : Tracks runs, bounds tripped, latencies")
    print("  3. Security Policy Guard        : Restricts OS/subprocess via --strict-host")
    print("  4. Zero OS Process Overhead     : Reuses warm JVM instance via --repeat <N>")
    print()

    # 3. Workload Verification
    print("[Workload Run]")
    start = time.time()
    total = sum(x * x for x in range(5000))
    elapsed_ms = (time.time() - start) * 1000.0
    print("  Calculated sum of squares (0..5000) = %d" % total)
    print("  Internal script computation time    = %.2f ms" % elapsed_ms)
    print("=" * 70)
    print("Quickstart demo completed successfully!")
    print("=" * 70)

if __name__ == "__main__":
    run_quickstart()
