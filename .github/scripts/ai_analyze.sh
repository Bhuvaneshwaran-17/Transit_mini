#!/usr/bin/env bash

# File: .github/scripts/ai_analyze.sh
OUT="ai_summary.txt"
LOG_FILE="maven_output.log"

echo "===== AI ROOT CAUSE ANALYSIS (MODELS GATEWAY) =====" > "$OUT"

# 1. SURGICAL EXTRACTION
RAW_LOC=$(grep -oE "[a-zA-Z0-9/_.-]+\.java:\[[0-9]+" "$LOG_FILE" | head -n 1)

CODE_CONTEXT="No code context available."
FILE_PATH="Unknown"
LINE_NUM="Unknown"

if [ ! -z "$RAW_LOC" ]; then
    RAW_PATH=$(echo "$RAW_LOC" | cut -d: -f1)
    LINE_NUM=$(echo "$RAW_LOC" | grep -oE "[0-9]+$")

    # Path Normalization: Try local path first, then fallback to filename search
    FILE_PATH=$(echo "$RAW_PATH" | sed "s|^.*/src/|src/|")

    if [ ! -f "$FILE_PATH" ]; then
        FILE_PATH=$(find . -name "$(basename "$RAW_PATH")" | head -n 1)
    fi

    if [ -f "$FILE_PATH" ]; then
        CODE_CONTEXT=$(sed -n "$((LINE_NUM-3)),$((LINE_NUM+3))p" "$FILE_PATH" | nl -ba -v $((LINE_NUM-3)))
    fi
fi

# 2. THE PRODUCTION CALL (Strict Formatting)
echo "Requesting High-Precision RCA..." >> "$OUT"

# We use a 'STRICT' system prompt to stop the AI from being 'wordy'
PAYLOAD=$(jq -n \
  --arg sys "You are a Senior DevOps Architect. Be extremely concise. Use exactly this format:
PATH: [File Path]
ERROR: [One sentence technical error]
FIX: [One line code correction]
NO PROSE. NO ADVICE. NO INTRODUCTIONS." \
  --arg user "Target: $FILE_PATH at Line $LINE_NUM \nContext:\n$CODE_CONTEXT" \
  '{model: "gpt-4o", messages: [{role: "system", content: $sys}, {role: "user", content: $user}]}')

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "https://models.inference.ai.azure.com/chat/completions" \
  -H "Authorization: Bearer $GH_TOKEN" \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD")

HTTP_STATUS=$(echo "$RESPONSE" | tail -n 1)
HTTP_BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_STATUS" -eq 200 ]; then
    echo "$HTTP_BODY" | jq -r '.choices[0].message.content' >> "$OUT"
    echo "SUCCESS: Crisp RCA generated."
else
    echo "ERROR: $HTTP_STATUS" >> "$OUT"
fi