# =============================================================================
# FILE PURPOSE:
# Micro-benchmark test script used to evaluate repeated in-host invocation latency.
# Performs an in-memory loop calculation to measure per-run execution overhead
# without confounding network or filesystem I/O.
# Run via: bounded-jython tests/test_bench_embedded.py --repeat 10
# =============================================================================

total = 0
for x in range(10000):
    total += x
print("Embedded iteration result: sum=%d" % total)
