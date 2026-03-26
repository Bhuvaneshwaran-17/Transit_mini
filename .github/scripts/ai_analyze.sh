#!/usr/bin/env bash

# File: .github/scripts/ai_analyze.sh
OUT="ai_summary.txt"
LOG_FILE="maven_output.log"

echo "===== AI ROOT CAUSE ANALYSIS (MODELS GATEWAY) =====" > "$OUT"

# 1. DATA PREP: Keep colons and slashes so paths and line numbers survive
# We keep [:] for line:col separation and [/] for file paths
CLEAN_LOGS=$(grep -iE -C 15 "ERROR|FAILURE" "$LOG_FILE" | head -n 80 | tr -cd '[:alnum:].[:space:]:/-')

if [ -z "$CLEAN_LOGS" ]; then
    CLEAN_LOGS=$(tail -n 50 "$LOG_FILE" | tr -cd '[:alnum:].[:space:]:/-')
fi

# 2. SURGICAL CODE CAPTURE
# Extract the first Java file and line number mentioned in the error
ERROR_LOC=$(echo "$CLEAN_LOGS" | grep -oE "[a-zA-Z0-9/_.-]+\.java:[0-9]+" | head -n 1)

CODE_CONTEXT="No code context available."
if [ ! -z "$ERROR_LOC" ]; then
    FILE_PATH=$(echo "$ERROR_LOC" | cut -d: -f1)
    LINE_NUM=$(echo "$ERROR_LOC" | cut -d: -f2)

    echo "DEBUG: Identified error at $FILE_PATH on line $LINE_NUM"

    # If the file exists, grab 5 lines of context
    if [ -f "$FILE_PATH" ]; then
        CODE_CONTEXT=$(sed -n "$((LINE_NUM-5)),$((LINE_NUM+5))p" "$FILE_PATH")
    fi
fi

# 3. THE PRODUCTION CALL (Using jq to safely encode the payload)
echo "Requesting Analysis from GPT-4o with Code Context..." >> "$OUT"

# We use jq --arg to safely inject variables into the JSON string
PAYLOAD=$(jq -n \
  --arg sys_prompt "You are a DevOps Architect. Provide a 3-bullet RCA: Error, File:Line, and Fix. Use the provided code context to be 100% accurate." \
  --arg user_prompt "Logs: $CLEAN_LOGS \n\nActual Code at Failure: \n$CODE_CONTEXT" \
  '{
    model: "gpt-4o",
    messages: [
      {role: "system", content: $sys_prompt},
      {role: "user", content: $user_prompt}
    ]
  }')

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "https://models.inference.ai.azure.com/chat/completions" \
  -H "Authorization: Bearer $GH_TOKEN" \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD")

# 4. EXTRACTION LOGIC
HTTP_BODY=$(echo "$RESPONSE" | sed '$d')
HTTP_STATUS=$(echo "$RESPONSE" | tail -n 1)

if [ "$HTTP_STATUS" -eq 200 ]; then
    echo "$HTTP_BODY" | jq -r '.choices[0].message.content' >> "$OUT"
    echo "SUCCESS: High-precision RCA generated."
else
    echo "CRITICAL ERROR: Status $HTTP_STATUS" >> "$OUT"
    echo "RAW DATA: $HTTP_BODY" >> "$OUT"
    echo "FAILURE: Check ai_summary.txt for debugging."
fi