# =============================================================================
# FILE PURPOSE:
# Test case verifying watchdog deadline enforcement against runaway infinite loops.
# Executes a non-yielding 'while True' spin loop under a 150ms host deadline,
# confirming the JVM host terminates the thread and reports bounds_tripped=1.
# Run via: bounded-jython tests/test_runaway.py --max-ms 150
# =============================================================================

import time

print("Starting runaway infinite spin loop...")
i = 0
while True:
    i += 1
