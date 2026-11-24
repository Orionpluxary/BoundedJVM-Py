# =============================================================================
# FILE PURPOSE:
# End-to-end demonstration of BoundedJVM-Py (Flavor #6) executing inside a JVM host.
# Displays JVM platform details, calculates prime numbers under a time deadline,
# and prints captured output and host-side telemetry metrics upon completion.
#
# DUAL RUN EXECUTION STEPS:
# -----------------------------------------------------------------------------
# 🔴 Running with regular/unmodified environment (e.g. standalone CPython / Jython without host):
#    Command: python3 examples/demo_bounded_jvm.py
#    Expected behavior:
#    - Python executes as an unmonitored OS process.
#    - No host telemetry, execution deadline watchdog, or bound counters are tracked.
#    - If a runaway loop occurs, the script hangs until manual SIGINT/kill.
#
# 🟢 Running with upgraded BoundedJVM-Py runtime:
#    Command: bounded-jython examples/demo_bounded_jvm.py --max-ms 2000
#    Expected behavior:
#    - Script executes inside the JVM with a strict 2000ms deadline.
#    - Host captures stdout/stderr and prints comprehensive telemetry:
#      `runs_executed: 1`, `bounds_tripped: 0`, `last_run_elapsed_ms: ~120ms`, `status: SUCCESS`.
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
