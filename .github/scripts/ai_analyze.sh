#!/usr/bin/env bash

# File: .github/scripts/ai_analyze.sh
OUT="ai_summary.txt"
LOG_FILE="maven_output.log"

echo "===== AI ROOT CAUSE ANALYSIS (MODELS GATEWAY) =====" > "$OUT"

# 1. SURGICAL LOG EXTRACTION (Privacy-First: No Source Code)
RAW_LOC=$(grep -oE "[a-zA-Z0-9/_.-]+\.java:\[[0-9]+,[0-9]+\]" "$LOG_FILE" | head -n 1)
FILE_PATH="Unknown"

if [ ! -z "$RAW_LOC" ]; then
    FILE_PATH=$(echo "$RAW_LOC" | cut -d: -f1 | sed "s|^.*/src/|src/|")
fi

# 2. THE DEPENDENCY SENSOR (The Hallucination Killer)
# We hunt for signatures of missing libraries in the logs
DEP_ERROR=$(grep -iE "package .* does not exist|symbol: class|DependencyResolutionException" "$LOG_FILE" | head -n 5)

if [ ! -z "$DEP_ERROR" ]; then
    INSTRUCTION="CRITICAL: This is a MISSING DEPENDENCY. Do NOT suggest Java code changes. Identify the missing Maven <dependency> (e.g., spring-boot-starter-web) based on the package name."
else
    INSTRUCTION="This is a Syntax or Logic error within the file. Suggest a specific code fix based on the log message."
fi

# 3. LOG CONTEXT PREP
# We grab 30 lines of logs around the primary error to give the AI "Eyes"
LOG_CONTEXT=$(grep -iE -C 15 "ERROR|FAILURE" "$LOG_FILE" | head -n 50 | tr -cd '[:alnum:].[:space:]:/-[],')

# 4. THE PRODUCTION CALL
echo "Requesting High-Precision RCA..." >> "$OUT"

PAYLOAD=$(jq -n \
  --arg sys "You are a Senior DevOps Architect. $INSTRUCTION Use exactly this format:
PATH: [File Path]
ERROR: [One sentence technical cause]
FIX: [One line code or XML fix]
NO PROSE. NO ADVICE." \
  --arg user "Target: $FILE_PATH \nLogs: $LOG_CONTEXT \nSpecific Failure: $DEP_ERROR" \
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
    echo "ERROR: $HTTP_STATUS" >> "$OUT"
fi