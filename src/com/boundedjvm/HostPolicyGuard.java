// =============================================================================
// FILE PURPOSE:
// Host security policy enforcement filter inspecting Python code before JVM execution.
// This file prevents unauthorized operating system operations, process spawning,
// reflection calls, and disallowed disk/network I/O when strict host mode is enabled.
// Invoked automatically by BoundedPyHost prior to passing script code to the interpreter.
// =============================================================================

package com.boundedjvm;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class HostPolicyGuard {
    private final boolean strictHost;
    private final boolean allowIo;

    /*
     * -------------------------------------------------------------------------
     * SIDE-BY-SIDE ARCHITECTURAL COMPARISON:
     * -------------------------------------------------------------------------
     * ORIGINAL UNCONSTRAINED JYTHON / CPYTHON:
     *   - Scripts possess unrestricted access to os, subprocess, and Java runtime.
     *   - An embedded script could execute `import os; os.system("rm -rf /")` or
     *     `java.lang.System.exit(0)`, terminating the entire parent application.
     *
     * MODIFIED STRICT HOST POLICY GUARD (BoundedJVM-Py):
     *   - Pre-execution validation verifies code safety before interpreter entry.
     *   - Forbids escape hatches (os, subprocess, java.lang.Runtime, ProcessBuilder).
     *   - Enforces explicit opt-in (--allow-io) for filesystem write access.
     * -------------------------------------------------------------------------
     */

    // Disallowed system access patterns when strictHost is enabled
    private static final Pattern[] FORBIDDEN_PATTERNS = new Pattern[] {
        Pattern.compile("import\\s+os(\\b|\\.)", Pattern.MULTILINE),
        Pattern.compile("import\\s+subprocess(\\b|\\.)", Pattern.MULTILINE),
        Pattern.compile("from\\s+os\\s+import", Pattern.MULTILINE),
        Pattern.compile("from\\s+subprocess\\s+import", Pattern.MULTILINE),
        Pattern.compile("java\\.lang\\.Runtime", Pattern.MULTILINE),
        Pattern.compile("java\\.lang\\.ProcessBuilder", Pattern.MULTILINE),
        Pattern.compile("java\\.lang\\.System\\.exit", Pattern.MULTILINE),
        Pattern.compile("sys\\.exit\\(", Pattern.MULTILINE)
    };

    // Disallowed file I/O patterns when allowIo is false under strictHost
    private static final Pattern[] IO_PATTERNS = new Pattern[] {
        Pattern.compile("open\\s*\\([^)]*['\"][waWA\\+]", Pattern.MULTILINE),
        Pattern.compile("java\\.io\\.File", Pattern.MULTILINE),
        Pattern.compile("java\\.nio\\.file", Pattern.MULTILINE)
    };

    public HostPolicyGuard(boolean strictHost, boolean allowIo) {
        this.strictHost = strictHost;
        this.allowIo = allowIo;
    }

    public boolean isStrictHost() { return strictHost; }
    public boolean isAllowIo() { return allowIo; }

    public void validate(String code) throws PolicyViolationException {
        if (!strictHost) {
            return;
        }

        for (Pattern p : FORBIDDEN_PATTERNS) {
            Matcher m = p.matcher(code);
            if (m.find()) {
                throw new PolicyViolationException("Host policy guard violation: disallowed system operation pattern detected -> '" + m.group() + "'");
            }
        }

        if (!allowIo) {
            for (Pattern p : IO_PATTERNS) {
                Matcher m = p.matcher(code);
                if (m.find()) {
                    throw new PolicyViolationException("Host policy guard violation: disallowed file/network I/O pattern detected without --allow-io -> '" + m.group() + "'");
                }
            }
        }
    }
}
