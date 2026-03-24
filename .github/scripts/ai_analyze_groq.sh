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

# 1. Define a strict prompt for the standard model
PROMPT="ACT AS A LOG ANALYZER. Analyze the provided text for a Maven/Java build failure. Identify the exact file, line, and fix. Provide a 3-sentence RCA. DO NOT scan the local filesystem."

# 2. Force the standard model (gpt-4o) and use the --no-context flag
# We use -m to specify the model if your CLI version supports it,
# or we simply rely on the default by removing any model overrides.
echo "$CLEAN_LOGS" | gh copilot explain -p "$PROMPT" --no-context >> "$OUT" 2>&1
echo -e "\n--------------------------------------------" >> "$OUT"
echo "Analysis complete. Output saved to $OUT"