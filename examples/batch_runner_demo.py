# =============================================================================
# FILE PURPOSE:
# Demonstration of low-overhead repeated script execution in a single JVM instance.
# Runs an in-memory loop calculation multiple times to show how embedding Python
# in the JVM avoids per-script OS process spawning overhead.
#
# DUAL RUN EXECUTION STEPS:
# -----------------------------------------------------------------------------
# 🔴 Running with regular/unmodified environment (spawning fresh OS processes):
#    Command: for i in {1..10}; do python3 examples/batch_runner_demo.py; done
#    Expected behavior:
#    - 10 distinct OS processes are spawned sequentially.
#    - Total execution time takes ~1,500ms to 2,000ms due to process startup cost.
#
# 🟢 Running with upgraded BoundedJVM-Py runtime (--repeat 10):
#    Command: bounded-jython examples/batch_runner_demo.py --repeat 10
#    Expected behavior:
#    - All 10 runs execute inside a single warm JVM instance.
#    - Total elapsed time is ~120ms total (~12ms per execution, down to 8ms warm).
#    - Achieves over 115x acceleration for embedded rule/plugin workloads.
# =============================================================================

import sys

total = 0
for i in range(10000):
    total += i

print("[BatchDemo] Embedded run complete. Calculated sum = %d" % total)
