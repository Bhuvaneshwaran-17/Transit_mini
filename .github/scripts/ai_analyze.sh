#!/usr/bin/env bash

# File: .github/scripts/ai_analyze.sh
OUT="ai_summary.txt"
LOG_FILE="maven_output.log"

echo "===== AI ROOT CAUSE ANALYSIS (CLAUDE-POWERED) =====" > "$OUT"

# 1. VERIFY DATA
if [ ! -f "$LOG_FILE" ] || [ ! -s "$LOG_FILE" ]; then
    echo "CRITICAL ERROR: $LOG_FILE is empty or missing." >> "$OUT"
    exit 1
fi

# 2. SURGICAL LOG CAPTURE
# Capture the compilation error specifically
ERROR_ZONE=$(grep -iE -C 50 "COMPILATION ERROR|Compilation failure|BUILD FAILURE|ERROR" "$LOG_FILE" | head -n 300)

if [ -z "$ERROR_ZONE" ]; then
    CLEAN_LOGS=$(tail -n 250 "$LOG_FILE")
    echo "[WARNING] Sending raw tail logs..." >> "$OUT"
else
    CLEAN_LOGS="$ERROR_ZONE"
    echo "[SUCCESS] Found error markers. Analyzing with Claude..." >> "$OUT"
fi

# 3. EXECUTE VIA NATIVE COPILOT (The CLI-Suggested Syntax)
echo "Consulting Copilot for Root Cause..." >> "$OUT"

# 1. We combine the Instruction and the Logs into one variable.
# 2. We use the root 'gh copilot' command with -p.
# 3. We QUOTE the entire prompt to satisfy the CLI's picky parser.

FULL_PROMPT="[RCA MODE] Analyze these Maven logs: $CLEAN_LOGS. Provide a 3-bullet RCA: Error, File:Line, and Fix."

gh copilot -p "$FULL_PROMPT" >> "$OUT" 2>&1

# 4. SAFETY CHECK: If it still returns 'Invalid command', we use the -i flag as a last resort.
if grep -q "Invalid command format" "$OUT"; then
    echo "Falling back to interactive-bypass mode..." >> "$OUT"
    gh copilot -i "explain -p '$FULL_PROMPT'" >> "$OUT" 2>&1
fi

echo -e "\n--------------------------------------------" >> "$OUT"