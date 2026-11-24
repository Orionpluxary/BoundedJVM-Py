#!/usr/bin/env bash
# =============================================================================
# FILE PURPOSE:
# System installation script for BoundedJVM-Py SDK.
# Unpacks the distributable SDK tarball to /opt/bounded-jvm-py and registers
# the global executable symlink at /usr/local/bin/bounded-jython.
# Run via: bash scripts/install.sh (or sudo bash scripts/install.sh)
# =============================================================================

set -euo pipefail

DEST_DIR="/opt/bounded-jvm-py"
BIN_LINK="/usr/local/bin/bounded-jython"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

ARCHIVE="${ROOT_DIR}/dist/jvm-bounded-py-sdk.tar.gz"
if [ ! -f "${ARCHIVE}" ]; then
    ARCHIVE="${ROOT_DIR}/jvm-bounded-py-sdk.tar.gz"
fi

if [ ! -f "${ARCHIVE}" ]; then
    echo "Archive not found. Running build script first..."
    bash "${SCRIPT_DIR}/build.sh"
    ARCHIVE="${ROOT_DIR}/dist/jvm-bounded-py-sdk.tar.gz"
fi

echo "=========================================================="
echo "Installing BoundedJVM-Py (Flavor #6) to ${DEST_DIR}"
echo "=========================================================="

sudo rm -rf "${DEST_DIR}"
sudo mkdir -p "${DEST_DIR}"
sudo tar -xzf "${ARCHIVE}" -C "${DEST_DIR}" --strip-components=1

sudo chmod +x "${DEST_DIR}/bin/bounded-jython"
sudo ln -sf "${DEST_DIR}/bin/bounded-jython" "${BIN_LINK}"

echo "Installation complete!"
echo "SDK Location : ${DEST_DIR}"
echo "CLI Command  : ${BIN_LINK}"
echo
echo "Verifying launcher:"
"${BIN_LINK}" --help
