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
# Claude can handle more context, so we expand the window to 50 lines
ERROR_ZONE=$(grep -iE -C 50 "COMPILATION ERROR|Compilation failure|BUILD FAILURE|ERROR" "$LOG_FILE" | head -n 300)

if [ -z "$ERROR_ZONE" ]; then
    CLEAN_LOGS=$(tail -n 250 "$LOG_FILE")
    echo "[WARNING] Sending raw tail logs to Claude..." >> "$OUT"
else
    CLEAN_LOGS="$ERROR_ZONE"
    echo "[SUCCESS] Found error markers. Analyzing with Claude..." >> "$OUT"
fi

# 3. EXECUTE VIA NATIVE COPILOT (Claude Optimized)
# We use the -p flag directly on the root command to avoid 'Invalid command format'
echo "Consulting Claude for Root Cause..." >> "$OUT"

# Updated Prompt for Claude's advanced reasoning
PROMPT="[STRICT RCA MODE] You are a Senior DevOps Architect. Analyze the following Maven logs for the 'auth' service.
1. Identify the exact Error Type.
2. Provide the File Path and Line Number.
3. Provide a concise Code Fix.
Format the response as a clean 3-bullet list. Do NOT include conversational filler."

echo "$CLEAN_LOGS" | gh copilot -p "$PROMPT" >> "$OUT" 2>&1

echo -e "\n--------------------------------------------" >> "$OUT"