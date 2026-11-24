# =============================================================================
# FILE PURPOSE:
# Demonstration of host policy guard and sandbox security enforcement.
# Attempts to invoke unauthorized OS system operations and process spawning
# to demonstrate how BoundedPyHost isolates untrusted scripts from the host system.
#
# =============================================================================
# STEP-BY-STEP EXECUTION GUIDE:
# =============================================================================
#
# -----------------------------------------------------------------------------
# SCENARIO 1: 🔴 Running with Regular/Unmodified Python (Unrestricted Python)
# -----------------------------------------------------------------------------
# STEP 1: Execute using standard Python:
#   $ python3 examples/strict_sandbox_demo.py
#
# STEP 2: Observe Terminal Output:
#   [SandboxDemo] Successfully accessed OS module! OS name: posix
#
# STEP 3: Analysis of Security Risk:
#   - Untrusted code has unrestricted access to the underlying host OS.
#   - A malicious script could execute `os.system("rm -rf /")`, read environment
#     variables, spawn shell processes, or call `sys.exit(0)` to crash the host.
#
# -----------------------------------------------------------------------------
# SCENARIO 2: 🟢 Running with Upgraded BoundedJVM-Py Runtime (--strict-host)
# -----------------------------------------------------------------------------
# STEP 1: Execute using bounded-jython with strict host sandboxing enabled:
#   $ bounded-jython examples/strict_sandbox_demo.py --strict-host
#
# STEP 2: Observe Terminal Output:
#   Host policy guard violation: disallowed system operation pattern detected -> 'import os'[HOST NOTICE] Status: POLICY_VIOLATION | Host policy guard violation: disallowed system operation pattern detected -> 'import os'
#
#   =================== HOST TELEMETRY & BOUND COUNTERS ===================
#     runs_executed       : 1
#     bounds_tripped      : 1
#     last_run_elapsed_ms : 7 ms
#     total_elapsed_ms    : 7 ms
#     avg_elapsed_ms      : 7.00 ms
#     last_exit_status    : POLICY_VIOLATION
#   =======================================================================
#
# STEP 3: Verification of Security Enforcement:
#   - Exit code: 2 (policy violation exit code)
#   - HostPolicyGuard rejected the script during pre-execution screening in 7 ms.
#   - The unauthorized code was NEVER executed by the interpreter.
#   - Host recorded: bounds_tripped=1, last_exit_status=POLICY_VIOLATION.
# =============================================================================

import os

print("[SandboxDemo] Successfully accessed OS module! OS name: %s" % os.name)
