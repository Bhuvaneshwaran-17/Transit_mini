#!/usr/bin/env bash

# File: .github/scripts/ai_analyze.sh
OUT="ai_summary.txt"
LOG_FILE="maven_output.log"

echo "===== AI ROOT CAUSE ANALYSIS (MODELS GATEWAY) =====" > "$OUT"

# 1. SURGICAL EXTRACTION (FROM RAW LOGS)
# Maven format is usually: File.java:[21,3]
# We grep for the file and the opening bracket/number
RAW_LOC=$(grep -oE "[a-zA-Z0-9/_.-]+\.java:\[[0-9]+" "$LOG_FILE" | head -n 1)

CODE_CONTEXT="No code context available."
if [ ! -z "$RAW_LOC" ]; then
    # Extract path: everything before the colon
    FILE_PATH=$(echo "$RAW_LOC" | cut -d: -f1)
    # Extract line: only the numbers after the bracket
    LINE_NUM=$(echo "$RAW_LOC" | grep -oE "[0-9]+$")

    echo "DEBUG: Identified error at $FILE_PATH on line $LINE_NUM"

    if [ -f "$FILE_PATH" ]; then
        # Capture context WITH line numbers so the AI doesn't get lost
        CODE_CONTEXT=$(sed -n "$((LINE_NUM-5)),$((LINE_NUM+5))p" "$FILE_PATH" | nl -ba -v $((LINE_NUM-5)))
    fi
fi

# 2. DATA PREP (CLEANING FOR THE AI PAYLOAD)
# We keep the colon and brackets here so the AI sees the standard format
CLEAN_LOGS=$(grep -iE -C 15 "ERROR|FAILURE" "$LOG_FILE" | head -n 80 | tr -cd '[:alnum:].[:space:]:/-[]')

# 3. THE PRODUCTION CALL
echo "Requesting Analysis from GPT-4o with Code Context..." >> "$OUT"

PAYLOAD=$(jq -n \
  --arg sys_prompt "You are a DevOps Architect. Provide a 3-bullet RCA: Error, File:Line, and Fix. Use the provided code context and line numbers to be 100% accurate." \
  --arg user_prompt "Logs: $CLEAN_LOGS \n\nActual Code Context around line $LINE_NUM:\n$CODE_CONTEXT" \
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
fi