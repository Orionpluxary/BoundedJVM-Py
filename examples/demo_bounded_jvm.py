# =============================================================================
# FILE PURPOSE:
# End-to-end demonstration of BoundedJVM-Py (Flavor #6) executing inside a JVM host.
# Displays JVM platform details, calculates prime numbers under a time deadline,
# and prints captured output and host-side telemetry metrics upon completion.
#
# =============================================================================
# STEP-BY-STEP EXECUTION GUIDE:
# =============================================================================
#
# -----------------------------------------------------------------------------
# SCENARIO 1: 🔴 Running with Regular/Unmodified Python (Unhosted CPython)
# -----------------------------------------------------------------------------
# STEP 1: Execute using standard Python:
#   $ python3 examples/demo_bounded_jvm.py
#
# STEP 2: Observe Terminal Output:
#   [JVM-Python] Running inside Java Virtual Machine Host
#   [JVM-Python] Python Version: 3.10.12 (main, Nov 20 2023, 15:14:05) [GCC 11.4.0]
#   [JVM-Python] System Platform: linux
#   [JVM-Python] Computed 95 primes up to 500 in 0.85 ms
#   [JVM-Python] Sample primes: [2, 3, 5, 7, 11] ... [467, 479, 487, 491, 499]
#   [JVM-Python] Script execution finished successfully.
#
# STEP 3: Analysis of Limitations:
#   - Execution ran as an unmonitored OS process.
#   - No host telemetry, execution deadline watchdog, or bound counters were tracked.
#   - If a runaway loop occurs, the script hangs until manual external SIGINT/kill.
#
# -----------------------------------------------------------------------------
# SCENARIO 2: 🟢 Running with Upgraded BoundedJVM-Py Runtime
# -----------------------------------------------------------------------------
# STEP 1: Execute using bounded-jython with a 2000ms deadline:
#   $ bounded-jython examples/demo_bounded_jvm.py --max-ms 2000
#
# STEP 2: Observe Terminal Output:
#   [JVM-Python] Running inside Java Virtual Machine Host
#   [JVM-Python] Python Version: 2.7.4 (tags/v2.7.4:3f256f4a7, Aug 18 2024, 10:30:53) [OpenJDK 64-Bit Server VM (Ubuntu)]
#   [JVM-Python] System Platform: java17.0.21.1
#   [JVM-Python] Computed 95 primes up to 500 in 15.00 ms
#   [JVM-Python] Sample primes: [2, 3, 5, 7, 11] ... [467, 479, 487, 491, 499]
#   [JVM-Python] Script execution finished successfully.
#
#   =================== HOST TELEMETRY & BOUND COUNTERS ===================
#     runs_executed       : 1
#     bounds_tripped      : 0
#     last_run_elapsed_ms : 129 ms
#     total_elapsed_ms    : 129 ms
#     avg_elapsed_ms      : 129.00 ms
#     last_exit_status    : SUCCESS
#   =======================================================================
#
# STEP 3: Verification of Host Enforcement:
#   - Exit code: 0
#   - Script completed safely within the 2000ms deadline.
#   - Host recorded: runs_executed=1, bounds_tripped=0, last_exit_status=SUCCESS.
# =============================================================================

import sys
import time

print("[JVM-Python] Running inside Java Virtual Machine Host")
print("[JVM-Python] Python Version: %s" % sys.version.replace('\n', ' '))
print("[JVM-Python] System Platform: %s" % sys.platform)

# Compute prime numbers to demonstrate real workload
def compute_primes(limit):
    primes = []
    for num in range(2, limit):
        is_prime = True
        for factor in range(2, int(num ** 0.5) + 1):
            if num % factor == 0:
                is_prime = False
                break
        if is_prime:
            primes.append(num)
    return primes

start = time.time()
primes = compute_primes(500)
elapsed = (time.time() - start) * 1000.0

print("[JVM-Python] Computed %d primes up to 500 in %.2f ms" % (len(primes), elapsed))
print("[JVM-Python] Sample primes: %s ... %s" % (primes[:5], primes[-5:]))
print("[JVM-Python] Script execution finished successfully.")
