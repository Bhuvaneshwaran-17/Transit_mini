#!/usr/bin/env bash

# File: .github/scripts/ai_analyze.sh
# Purpose: AI Root Cause Analysis using Native GitHub Copilot CLI

OUT="ai_summary.txt"
echo "===== AI ROOT CAUSE ANALYSIS (COPILOT-POWERED) =====" > "$OUT"

# 1. VERIFY DATA (SRE Guardrail)
# Ensure the logs were actually downloaded from the artifact step
if [ ! -f logs.txt ] || [ ! -s logs.txt ]; then
    echo "CRITICAL ERROR: logs.txt is empty or missing." >> "$OUT"
    echo "Check the 'gh run download' step in your workflow." >> "$OUT"
    exit 1
fi

# 2. SURGICAL LOG CAPTURE
# We extract the 'COMPILATION ERROR' block + 25 lines of context
# This keeps the prompt clean and avoids Hibernate/JDBC noise
# 1. Use -i for case-insensitive
# 2. Use -E to search for MULTIPLE keywords (Compilation OR Failure OR Error)
ERROR_ZONE=$(grep -iE -C 30 "COMPILATION ERROR|Compilation failure|BUILD FAILURE|ERROR" logs.txt | head -n 200)

if [ -z "$ERROR_ZONE" ]; then
    # If it still finds nothing, the file 'logs.txt' is likely EMPTY
    CLEAN_LOGS=$(tail -n 150 logs.txt)
    echo "[WARNING] No specific error markers found. Sending raw tail logs to Copilot..." >> "$OUT"
else
    CLEAN_LOGS="$ERROR_ZONE"
    echo "[SUCCESS] Found error markers. Sending context to Copilot..." >> "$OUT"
fi

# 3. EXECUTE VIA NATIVE COPILOT
# We pipe the logs directly into 'gh copilot explain'
# The --prompt-file or -p flag is used for the instruction
echo "Consulting Copilot for Root Cause..."

PROMPT="Analyze these Maven build logs.
1. Identify the EXACT file and line number of the failure.
2. Explain why the build failed (e.g., Syntax Error, Missing Dependency).
3. Ignore all database, Hibernate, and JDBC connection noise.
4. Be brief. Maximum 3 bullet points."

echo "$CLEAN_LOGS" | gh copilot explain "$PROMPT" >> "$OUT" 2>&1# 4. FINAL VERDICT
echo -e "\n--------------------------------------------" >> "$OUT"
echo "Analysis complete. Output saved to $OUT"