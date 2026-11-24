#!/usr/bin/env bash
# =============================================================================
# FILE PURPOSE:
# Automated end-to-end regression test suite verifying all BoundedJVM-Py bounds.
# Executes 5 test scenarios covering normal runs, timeout watchdog enforcement,
# host policy sandboxing, and repeat in-host execution with JSON telemetry metrics.
# Run via: bash tests/run_tests.sh
# =============================================================================

set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

echo "=========================================================================="
echo "          BOUNDEDJVM-PY (FLAVOR #6) VERIFICATION TEST SUITE               "
echo "=========================================================================="
echo "CLI binary: $(which bounded-jython || echo '/usr/local/bin/bounded-jython')"
echo "Java info:  $(java -version 2>&1 | head -n 1)"
echo

echo ">>> TEST 1: Normal Script Run within 2000ms Bound (examples/demo_bounded_jvm.py)"
bounded-jython "${ROOT_DIR}/examples/demo_bounded_jvm.py" --max-ms 2000
echo "Exit code: $?"
echo

echo ">>> TEST 2: Basic Normal Script (tests/test_normal.py)"
bounded-jython "${ROOT_DIR}/tests/test_normal.py" --max-ms 2000
echo "Exit code: $?"
echo

echo ">>> TEST 3: Runaway Infinite Loop under 150ms Bound (tests/test_runaway.py)"
bounded-jython "${ROOT_DIR}/tests/test_runaway.py" --max-ms 150
echo "Exit code: $?"
echo

echo ">>> TEST 4: Host Policy Guard under --strict-host (tests/test_policy_violation.py)"
bounded-jython "${ROOT_DIR}/tests/test_policy_violation.py" --strict-host
echo "Exit code: $?"
echo

echo ">>> TEST 5: Repeated Host Invocations with JSON Telemetry (--repeat 5)"
bounded-jython "${ROOT_DIR}/tests/test_bench_embedded.py" --repeat 5 --telemetry-json
echo "Exit code: $?"
echo

echo "=========================================================================="
echo "                      ALL TEST CASES COMPLETED                             "
echo "=========================================================================="
