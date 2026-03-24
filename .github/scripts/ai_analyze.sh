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

# 3. EXECUTE VIA COPILOT (The Non-Breaking Syntax)
echo "Consulting Claude for Root Cause..." >> "$OUT"

# We pass the instruction via -p and the logs via PIPE.
# This avoids shell expansion errors with brackets [ ] and special chars.
echo "$CLEAN_LOGS" | gh copilot explain -p "Analyze these Maven logs for the 'auth' service. Identify the Error Type, File Path with Line Number, and a concise Code Fix. Provide a 3-bullet list only. Text-only response." >> "$OUT" 2>&1

# 4. CLEANUP
# Strip ANSI color codes from Copilot output
sed -i 's/\x1b\[[0-9;]*m//g' "$OUT"

echo -e "\n--------------------------------------------" >> "$OUT"