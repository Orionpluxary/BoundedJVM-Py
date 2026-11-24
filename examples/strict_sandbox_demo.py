# =============================================================================
# FILE PURPOSE:
# Demonstration of host policy guard and sandbox security enforcement.
# Attempts to invoke unauthorized OS system operations and process spawning
# to demonstrate how BoundedPyHost isolates untrusted scripts from the host system.
#
# DUAL RUN EXECUTION STEPS:
# -----------------------------------------------------------------------------
# 🔴 Running with regular/unmodified environment (unrestricted Python):
#    Command: python3 examples/strict_sandbox_demo.py
#    Expected behavior:
#    - The script successfully accesses the underlying OS and prints sensitive host info.
#    - Untrusted code has full access to spawn processes and manipulate files.
#
# 🟢 Running with upgraded BoundedJVM-Py runtime (--strict-host):
#    Command: bounded-jython examples/strict_sandbox_demo.py --strict-host
#    Expected behavior:
#    - HostPolicyGuard detects forbidden 'import os' pattern prior to execution.
#    - Script is blocked immediately without executing:
#      `Status: POLICY_VIOLATION`, `bounds_tripped: 1`, `last_elapsed_ms: ~7ms`.
# =============================================================================

import os

print("[SandboxDemo] Successfully accessed OS module! OS name: %s" % os.name)
