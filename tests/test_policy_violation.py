# =============================================================================
# FILE PURPOSE:
# Test case verifying host security policy guard enforcement under --strict-host.
# Attempts an unauthorized 'import os' primitive to confirm the host policy guard
# intercepts and rejects the execution before code evaluation begins.
# Run via: bounded-jython tests/test_policy_violation.py --strict-host
# =============================================================================

import os

print("Attempting OS access: %s" % os.name)
