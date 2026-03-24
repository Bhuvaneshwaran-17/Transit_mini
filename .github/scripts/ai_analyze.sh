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
ERROR_ZONE=$(grep -iE -C 50 "COMPILATION ERROR|Compilation failure|BUILD FAILURE|ERROR" "$LOG_FILE" | head -n 300)

if [ -z "$ERROR_ZONE" ]; then
    CLEAN_LOGS=$(tail -n 250 "$LOG_FILE")
    echo "[WARNING] Sending raw tail logs to Copilot..." >> "$OUT"
else
    CLEAN_LOGS="$ERROR_ZONE"
    echo "[SUCCESS] Found error markers. Analyzing with Copilot..." >> "$OUT"
fi

# 3. EXECUTE VIA COPILOT
echo "Consulting Copilot for Root Cause..." >> "$OUT"

# Write prompt + logs to a temp file to avoid shell argument corruption
# Maven logs contain special chars ([, ], <, >, `) that break inline string args
TMP_PROMPT=$(mktemp)
cat > "$TMP_PROMPT" <<'PROMPT_EOF'
[STRICT RCA MODE] You are a Senior DevOps Architect. Analyze the following Maven logs for the 'auth' service.
1. Identify the exact Error Type.
2. Provide the File Path and Line Number.
3. Provide a concise Code Fix.
Format the response as a clean 3-bullet list. Do NOT include conversational filler.
PROMPT_EOF

echo "" >> "$TMP_PROMPT"
echo "$CLEAN_LOGS" >> "$TMP_PROMPT"

# GH_PROMPT_DISABLED=1 forces non-interactive mode on the runner (no TTY)
GH_PROMPT_DISABLED=1 gh copilot explain "$(cat "$TMP_PROMPT")" >> "$OUT" 2>&1

# Cleanup temp file
rm -f "$TMP_PROMPT"

# Strip ANSI color codes from Copilot output
sed -i 's/\x1b\[[0-9;]*m//g' "$OUT"

# Verify Copilot actually returned something useful
if grep -q "Welcome to GitHub Copilot\|not available\|command not found\|authentication" "$OUT"; then
    echo "[WARNING] Copilot may not have returned a valid RCA. Check ai_summary.txt manually." >> "$OUT"
fi

echo -e "\n--------------------------------------------" >> "$OUT"
echo "Analysis complete. Output saved to $OUT"
