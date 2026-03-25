#!/usr/bin/env bash

# File: .github/scripts/ai_analyze.sh
OUT="ai_summary.txt"
LOG_FILE="maven_output.log"

echo "===== AI ROOT CAUSE ANALYSIS (MODELS GATEWAY) =====" > "$OUT"

# 1. DATA PREP: Strip messy characters that break JSON
CLEAN_LOGS=$(grep -iE -C 15 "ERROR|FAILURE" "$LOG_FILE" | head -n 80 | tr -cd '[:alnum:].[:space:]-')

if [ -z "$CLEAN_LOGS" ]; then
    CLEAN_LOGS=$(tail -n 50 "$LOG_FILE" | tr -cd '[:alnum:].[:space:]-')
fi

# 2. THE PRODUCTION CALL
# We use the 'inference' endpoint which is built for PATs
echo "Requesting Analysis from GPT-4o..." >> "$OUT"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "https://models.inference.ai.azure.com/chat/completions" \
  -H "Authorization: Bearer $GH_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"model\": \"gpt-4o\",
    \"messages\": [
      {\"role\": \"system\", \"content\": \"You are a DevOps Architect. Provide a 3-bullet RCA: Error, File:Line, and Fix.\"},
      {\"role\": \"user\", \"content\": \"Analyze these logs: $CLEAN_LOGS\"}
    ]
  }")

# 3. EXTRACTION LOGIC
HTTP_BODY=$(echo "$RESPONSE" | sed '$d')
HTTP_STATUS=$(echo "$RESPONSE" | tail -n 1)

if [ "$HTTP_STATUS" -eq 200 ]; then
    # Extracting the content field using jq
    echo "$HTTP_BODY" | jq -r '.choices[0].message.content' >> "$OUT"
    echo "SUCCESS: AI Summary generated."
else
    echo "CRITICAL ERROR: Status $HTTP_STATUS" >> "$OUT"
    echo "RAW DATA: $HTTP_BODY" >> "$OUT"
    echo "FAILURE: Check ai_summary.txt for debugging."
fi