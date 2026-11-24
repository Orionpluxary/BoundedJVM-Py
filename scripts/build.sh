#!/usr/bin/env bash
# =============================================================================
# FILE PURPOSE:
# Automated build and packaging tool for BoundedJVM-Py (Flavor #6).
# Compiles Java host classes, packages bounded-host.jar, verifies runtime JARs,
# and bundles the distributable SDK archive into dist/jvm-bounded-py-sdk.tar.gz.
# Run via: bash scripts/build.sh
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
BUILD_DIR="${ROOT_DIR}/build"
DIST_DIR="${ROOT_DIR}/dist"
RUNTIME_DIR="${ROOT_DIR}/runtime"

echo "=== Building BoundedJVM-Py Host Runtime ==="

mkdir -p "${BUILD_DIR}/classes" "${DIST_DIR}" "${RUNTIME_DIR}"

JYTHON_JAR="${RUNTIME_DIR}/jython-standalone.jar"
if [ ! -f "${JYTHON_JAR}" ]; then
    echo "Downloading Jython standalone artifact..."
    curl -fSL https://repo1.maven.org/maven2/org/python/jython-standalone/2.7.4/jython-standalone-2.7.4.jar -o "${JYTHON_JAR}" || \
    curl -fSL https://repo1.maven.org/maven2/org/python/jython-standalone/2.7.3/jython-standalone-2.7.3.jar -o "${JYTHON_JAR}"
fi

echo "Compiling Java host classes..."
javac -cp "${JYTHON_JAR}" -d "${BUILD_DIR}/classes" "${ROOT_DIR}/src/com/boundedjvm/"*.java

echo "Packaging bounded-host.jar..."
jar cfe "${BUILD_DIR}/bounded-host.jar" com.boundedjvm.BoundedPyHost -C "${BUILD_DIR}/classes" .

echo "Staging SDK layout..."
STAGING_DIR="${BUILD_DIR}/staging/bounded-jvm-py"
rm -rf "${BUILD_DIR}/staging"
mkdir -p "${STAGING_DIR}/bin" "${STAGING_DIR}/lib"

cp "${BUILD_DIR}/bounded-host.jar" "${STAGING_DIR}/lib/"
cp "${JYTHON_JAR}" "${STAGING_DIR}/lib/"

cat << 'EOF' > "${STAGING_DIR}/bin/bounded-jython"
#!/usr/bin/env bash
set -e

INSTALL_DIR="/opt/bounded-jvm-py"
LIB_DIR="${INSTALL_DIR}/lib"
HOST_JAR="${LIB_DIR}/bounded-host.jar"
JYTHON_JAR="${LIB_DIR}/jython-standalone.jar"

if [ ! -f "${HOST_JAR}" ] || [ ! -f "${JYTHON_JAR}" ]; then
    echo "Error: BoundedJVM-Py installation missing or corrupted in ${INSTALL_DIR}" >&2
    exit 1
fi

JAVA_BIN="java"
if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
    JAVA_BIN="${JAVA_HOME}/bin/java"
fi

exec "${JAVA_BIN}" -cp "${HOST_JAR}:${JYTHON_JAR}" com.boundedjvm.BoundedPyHost "$@"
EOF
chmod +x "${STAGING_DIR}/bin/bounded-jython"

echo "Creating distributable tarball in dist/jvm-bounded-py-sdk.tar.gz..."
tar -czf "${DIST_DIR}/jvm-bounded-py-sdk.tar.gz" -C "${BUILD_DIR}/staging" bounded-jvm-py

echo "Build completed successfully!"
ls -lh "${DIST_DIR}/jvm-bounded-py-sdk.tar.gz"
