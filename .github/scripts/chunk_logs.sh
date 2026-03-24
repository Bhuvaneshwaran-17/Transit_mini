#!/usr/bin/env bash
set -euo pipefail

# We don't want to split a 50KB log file into 10 pieces.
# Only split if the file is massive (>100KB).
FILE_SIZE=$(stat -c%s "logs.txt")

if [ "$FILE_SIZE" -gt 100000 ]; then
    echo "Log file is large ($FILE_SIZE bytes). Splitting for analysis..."
    split -b 50k logs.txt chunk_
else
    echo "Log file is optimal size. No chunking needed."
    cp logs.txt chunk_aa # Create a single 'chunk' so the next step doesn't fail
fi