// =============================================================================
// FILE PURPOSE:
// Custom unchecked exception representing security policy violations in BoundedPyHost.
// This file exists to signal immediate script rejection when disallowed system patterns,
// forbidden OS primitives, or unauthorized I/O operations are detected in untrusted code.
// Instantiated and thrown internally by HostPolicyGuard during pre-execution validation.
// =============================================================================

package com.boundedjvm;

public class PolicyViolationException extends RuntimeException {
    public PolicyViolationException(String message) {
        super(message);
    }
}
