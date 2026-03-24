#!/usr/bin/env bash

# File: .github/scripts/ai_analyze.sh
OUT="ai_summary.txt"
# This must match what you name the log file in your YAML ('maven_output.log')
LOG_FILE="maven_output.log"

echo "===== AI ROOT CAUSE ANALYSIS (COPILOT-POWERED) =====" > "$OUT"

# 1. VERIFY DATA (SRE Guardrail)
# Use the $LOG_FILE variable here, NOT logs.txt
if [ ! -f "$LOG_FILE" ] || [ ! -s "$LOG_FILE" ]; then
    echo "CRITICAL ERROR: $LOG_FILE is empty or missing." >> "$OUT"
    echo "Check the 'gh run view' step in your workflow." >> "$OUT"
    exit 1
fi

# 2. SURGICAL LOG CAPTURE
# Again, reference $LOG_FILE
ERROR_ZONE=$(grep -iE -C 30 "COMPILATION ERROR|Compilation failure|BUILD FAILURE|ERROR" "$LOG_FILE" | head -n 200)

if [ -z "$ERROR_ZONE" ]; then
    CLEAN_LOGS=$(tail -n 150 "$LOG_FILE")
    echo "[WARNING] No specific error markers found. Sending raw tail logs to Copilot..." >> "$OUT"
else
    CLEAN_LOGS="$ERROR_ZONE"
    echo "[SUCCESS] Found error markers. Sending context to Copilot..." >> "$OUT"
fi

# 3. EXECUTE VIA NATIVE COPILOT
echo "Consulting Copilot for Root Cause..."

PROMPT="[RCA MODE] Analyze the provided text ONLY. Do NOT use external tools or scan the filesystem. Identify the Maven/Java build failure for the 'auth' service. Provide a 3-bullet point RCA: Error Type, File:Line, and Recommended Fix."

# The pipe finally has data because $CLEAN_LOGS is now correctly populated
echo "$CLEAN_LOGS" | gh copilot -p "$PROMPT" >> "$OUT" 2>&1

echo -e "\n--------------------------------------------" >> "$OUT"
echo "Analysis complete. Output saved to $OUT"