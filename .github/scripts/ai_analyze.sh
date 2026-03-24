#!/usr/bin/env bash

# File: .github/scripts/ai_analyze.sh
OUT="ai_summary.txt"
LOG_FILE="maven_output.log"

echo "===== AI ROOT CAUSE ANALYSIS (COPILOT-POWERED) =====" > "$OUT"

# 1. VERIFY DATA
if [ ! -f "$LOG_FILE" ] || [ ! -s "$LOG_FILE" ]; then
    echo "CRITICAL ERROR: $LOG_FILE is empty or missing." >> "$OUT"
    exit 1
fi

# 2. SURGICAL LOG CAPTURE
# Capture 30 lines before/after error markers
ERROR_ZONE=$(grep -iE -C 30 "COMPILATION ERROR|Compilation failure|BUILD FAILURE|ERROR" "$LOG_FILE" | head -n 200)

if [ -z "$ERROR_ZONE" ]; then
    CLEAN_LOGS=$(tail -n 150 "$LOG_FILE")
    echo "[WARNING] No specific error markers found. Sending raw tail logs..." >> "$OUT"
else
    CLEAN_LOGS="$ERROR_ZONE"
    echo "[SUCCESS] Found error markers. Sending context to Copilot..." >> "$OUT"
fi

# 3. EXECUTE VIA NATIVE COPILOT
# 3. EXECUTE VIA NATIVE COPILOT
echo "Consulting Copilot for Root Cause..." >> "$OUT"

# We use 'explain' with the '-p' flag to handle the piped $CLEAN_LOGS
echo "$CLEAN_LOGS" | gh copilot explain -p "[RCA MODE] Analyze the provided text ONLY. Identify the Maven/Java build failure for the 'auth' service. Provide a 3-bullet point RCA: Error Type, File:Line, and Recommended Fix." >> "$OUT" 2>&1

echo -e "\n--------------------------------------------" >> "$OUT"