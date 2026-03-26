#!/usr/bin/env bash

# File: .github/scripts/ai_analyze.sh
OUT="ai_summary.txt"
LOG_FILE="maven_output.log"

echo "===== AI ROOT CAUSE ANALYSIS (MODELS GATEWAY) =====" > "$OUT"

# 1. SURGICAL LOG EXTRACTION
# Finds the first Java file failure with line and column numbers
RAW_LOC=$(grep -oE "[a-zA-Z0-9/_.-]+\.java:\[[0-9]+,[0-9]+\]" "$LOG_FILE" | head -n 1)
FILE_PATH="Unknown"

if [ ! -z "$RAW_LOC" ]; then
    FILE_PATH=$(echo "$RAW_LOC" | cut -d: -f1 | sed "s|^.*/src/|src/|")
fi

# 2. THE DEPENDENCY SENSOR (Hallucination Killer)
# Only grabs the core error message to keep the payload small
DEP_ERROR=$(grep -iE "package .* does not exist|symbol: class|DependencyResolutionException" "$LOG_FILE" | head -n 3)

if [ ! -z "$DEP_ERROR" ]; then
    INSTRUCTION="CRITICAL: This is a MISSING DEPENDENCY. Suggest the missing Maven <dependency> XML. Do NOT suggest Java code changes."
else
    INSTRUCTION="This is a Syntax or Logic error. Suggest a one-line code fix."
fi

# 3. SLIM LOG CONTEXT (Privacy-First & Lightweight)
# Instead of 50 lines, we grab the 10 most relevant lines around the error
LOG_CONTEXT=$(grep -iE -B 2 -A 7 "ERROR|FAILURE|Exception" "$LOG_FILE" | head -n 15 | tr -cd '[:alnum:].[:space:]:/-[],')

# 4. THE PRODUCTION CALL (With Enhanced Error Reporting)
echo "Requesting High-Precision RCA..." >> "$OUT"

PAYLOAD=$(jq -n \
  --arg sys "You are a Senior DevOps Architect. $INSTRUCTION Use exactly this format:
PATH: [File Path]
ERROR: [One sentence technical cause]
FIX: [One line fix]
NO PROSE. NO ADVICE." \
  --arg user "Target: $FILE_PATH \nLogs: $LOG_CONTEXT \nEvidence: $DEP_ERROR" \
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
    # Better debugging: If it fails, we see the status and a snippet of the reason
    echo "API FAILURE: HTTP $HTTP_STATUS" >> "$OUT"
    echo "$HTTP_BODY" | head -c 100 >> "$OUT"
fi