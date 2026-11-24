# =============================================================================
# FILE PURPOSE:
# Demonstration of runaway loop execution under host-enforced deadline watchdogs.
# Emulates an infinite compute loop (such as bad recursion or unyielding while loop)
# to prove the JVM host terminates execution safely without hanging the host process.
#
# DUAL RUN EXECUTION STEPS:
# -----------------------------------------------------------------------------
# 🔴 Running with regular/unmodified environment (unbounded Python):
#    Command: python3 examples/timeout_demo.py
#    Expected behavior:
#    - The process spins indefinitely at 100% CPU utilization.
#    - Process never terminates; requires external kill -9 or Ctrl+C.
#
# 🟢 Running with upgraded BoundedJVM-Py runtime:
#    Command: bounded-jython examples/timeout_demo.py --max-ms 150
#    Expected behavior:
#    - Host watchdog triggers at 150ms and forcibly halts worker thread.
#    - Exits with status TIMEOUT, exit code 124, and telemetry:
#      `bounds_tripped: 1`, `last_status: TIMEOUT`, `last_run_elapsed_ms: ~190ms`.
# =============================================================================

import sys

print("[TimeoutDemo] Commencing runaway spin loop...")
counter = 0
while True:
    counter += 1
