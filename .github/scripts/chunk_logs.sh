#!/usr/bin/env bash
set -euo pipefail

SRC="${1:-logs.txt}"
CHUNK_BYTES="${CHUNK_BYTES:-500000}"  # ~500 KB
split -b 100k logs.txt chunk_
ls -l chunk_* || true
