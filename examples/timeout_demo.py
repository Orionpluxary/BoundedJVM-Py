# =============================================================================
# FILE PURPOSE:
# Demonstration of runaway loop execution under host-enforced deadline watchdogs.
# Emulates an infinite compute loop (such as bad recursion or unyielding while loop)
# to prove the JVM host terminates execution safely without hanging the host process.
#
# =============================================================================
# STEP-BY-STEP EXECUTION GUIDE:
# =============================================================================
#
# -----------------------------------------------------------------------------
# SCENARIO 1: 🔴 Running with Regular/Unmodified Python (Unhosted CPython)
# -----------------------------------------------------------------------------
# STEP 1: Execute using standard Python:
#   $ python3 examples/timeout_demo.py
#
# STEP 2: Observe Terminal Output:
#   [TimeoutDemo] Commencing runaway spin loop...
#   (Process hangs indefinitely at 100% CPU utilization...)
#   ^C
#   Traceback (most recent call last):
#     File "examples/timeout_demo.py", line 28, in <module>
#       counter += 1
#   KeyboardInterrupt
#
# STEP 3: Analysis of Limitations:
#   - Process never terminates voluntarily.
#   - In a production service or serverless worker, this hangs the worker thread
#     forever and causes memory/CPU starvation until hard killed by the OS.
#
# -----------------------------------------------------------------------------
# SCENARIO 2: 🟢 Running with Upgraded BoundedJVM-Py Runtime
# -----------------------------------------------------------------------------
# STEP 1: Execute using bounded-jython with a strict 150ms deadline:
#   $ bounded-jython examples/timeout_demo.py --max-ms 150
#
# STEP 2: Observe Terminal Output:
#   [TimeoutDemo] Commencing runaway spin loop...
#   [HOST NOTICE] Status: TIMEOUT | Execution deadline exceeded: host bound of 150 ms tripped (elapsed: 196 ms)
#
#   =================== HOST TELEMETRY & BOUND COUNTERS ===================
#     runs_executed       : 1
#     bounds_tripped      : 1
#     last_run_elapsed_ms : 196 ms
#     total_elapsed_ms    : 196 ms
#     avg_elapsed_ms      : 196.00 ms
#     last_exit_status    : TIMEOUT
#   =======================================================================
#
# STEP 3: Verification of Host Enforcement:
#   - Exit code: 124 (standard Linux timeout exit code)
#   - Host watchdog interrupted and aborted the runaway thread at ~150ms.
#   - Host recorded: bounds_tripped=1, last_exit_status=TIMEOUT.
#   - The parent JVM host remains completely healthy and ready for new scripts!
# =============================================================================

import sys

print("[TimeoutDemo] Commencing runaway spin loop...")
counter = 0
while True:
    counter += 1
