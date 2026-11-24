# =============================================================================
# FILE PURPOSE:
# Test case validating normal, healthy script completion under BoundedJVM-Py.
# Verifies that standard Python code runs successfully without false timeout triggers,
# reporting bounds_tripped=0 and exit code 0.
# Run via: bounded-jython tests/test_normal.py --max-ms 2000
# =============================================================================

import sys

# Validate running environment
print("Normal script executed successfully. sys.platform=%s" % sys.platform)
