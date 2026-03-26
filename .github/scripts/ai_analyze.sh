#!/usr/bin/env bash

# File: .github/scripts/ai_analyze.sh
OUT="ai_summary.txt"
LOG_FILE="maven_output.log"

echo "===== AI ROOT CAUSE ANALYSIS (MODELS GATEWAY) =====" > "$OUT"

# 1. SURGICAL EXTRACTION (FROM RAW LOGS)
# We find the file and the first part of the line number: File.java:[21
RAW_LOC=$(grep -oE "[a-zA-Z0-9/_.-]+\.java:\[[0-9]+" "$LOG_FILE" | head -n 1)

CODE_CONTEXT="No code context available."
LINE_NUM="Unknown"

if [ ! -z "$RAW_LOC" ]; then
    RAW_PATH=$(echo "$RAW_LOC" | cut -d: -f1)
    LINE_NUM=$(echo "$RAW_LOC" | grep -oE "[0-9]+$")

    # PATH NORMALIZATION:
    # If the path is absolute, strip the GitHub Runner prefix to make it local
    # This turns /home/runner/work/Repo/Repo/src/... into src/...
    FILE_PATH=$(echo "$RAW_PATH" | sed "s|^.*/src/|src/|")

    echo "DEBUG: Raw Path: $RAW_PATH"
    echo "DEBUG: Normalized Path: $FILE_PATH"
    echo "DEBUG: Target Line: $LINE_NUM"

    if [ -f "$FILE_PATH" ]; then
        # Capture context with line numbers
        CODE_CONTEXT=$(sed -n "$((LINE_NUM-5)),$((LINE_NUM+5))p" "$FILE_PATH" | nl -ba -v $((LINE_NUM-5)))
        echo "DEBUG: Code context captured successfully."
    else
        echo "DEBUG: CRITICAL - File not found at $FILE_PATH"
        # Fallback: search for the file by name if the path is still broken
        ALT_PATH=$(find . -name "$(basename "$FILE_PATH")" | head -n 1)
        if [ ! -z "$ALT_PATH" ]; then
             CODE_CONTEXT=$(sed -n "$((LINE_NUM-5)),$((LINE_NUM+5))p" "$ALT_PATH" | nl -ba -v $((LINE_NUM-5)))
             FILE_PATH=$ALT_PATH
             echo "DEBUG: Found file via fallback search: $FILE_PATH"
        fi
    fi
fi

# 2. DATA PREP (CLEANING BUT KEEPING THE COMMA)
# Added ',' to the tr command so [21,3] stays [21,3]
CLEAN_LOGS=$(grep -iE -C 10 "ERROR|FAILURE" "$LOG_FILE" | head -n 50 | tr -cd '[:alnum:].[:space:]:/-[],')

# 3. THE PRODUCTION CALL
echo "Requesting Analysis from GPT-4o..." >> "$OUT"

# We tell the AI EXPLICITLY to ignore the mangled log number if it sees one
PAYLOAD=$(jq -n \
  --arg sys "You are a DevOps Architect. Provide a 3-bullet RCA. NOTE: The logs may contain column numbers like [21,3] which look like 213. IGNORE THAT. Rely ONLY on the Code Context provided which is centered around line $LINE_NUM." \
  --arg user "Logs from Build: $CLEAN_LOGS \n\nTarget File: $FILE_PATH \nTarget Line: $LINE_NUM \n\nCode Context:\n$CODE_CONTEXT" \
  '{model: "gpt-4o", messages: [{role: "system", content: $sys}, {role: "user", content: $user}]}')

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "https://models.inference.ai.azure.com/chat/completions" \
  -H "Authorization: Bearer $GH_TOKEN" \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD")

HTTP_STATUS=$(echo "$RESPONSE" | tail -n 1)
HTTP_BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_STATUS" -eq 200 ]; then
    echo "$HTTP_BODY" | jq -r '.choices[0].message.content' >> "$OUT"
    echo "SUCCESS: Precision RCA generated."
else
    echo "CRITICAL ERROR: $HTTP_STATUS" >> "$OUT"
    echo "$HTTP_BODY" >> "$OUT"
fi