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

# 2. SURGICAL LOG CAPTURE (Extraction logic)
ERROR_ZONE=$(grep -iE -C 20 "COMPILATION ERROR|Compilation failure|BUILD FAILURE|ERROR" "$LOG_FILE" | head -n 100)
CLEAN_LOGS="${ERROR_ZONE:-$(tail -n 100 "$LOG_FILE")}"

# 3. UPDATED EXECUTE VIA GITHUB COPILOT PROXY
echo "Consulting Copilot API (Proxy Mode)..." >> "$OUT"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "https://api.githubcopilot.com/chat/completions" \
  -H "Authorization: Bearer $GH_TOKEN" \
  -H "Content-Type: application/json" \
  -H "Editor-Version: vscode/1.85.1" \
  -d "{
    \"model\": \"gpt-4o\",
    \"messages\": [
      {
        \"role\": \"system\",
        \"content\": \"You are a DevOps Architect. Provide a 3-bullet RCA: 1. Error Type, 2. File:Line, 3. Suggested Fix.\"
      },
      {
        \"role\": \"user\",
        \"content\": \"Analyze these Maven logs: $CLEAN_LOGS\"
      }
    ]
  }")

# Split response and status code
HTTP_BODY=$(echo "$RESPONSE" | sed '$d')
HTTP_STATUS=$(echo "$RESPONSE" | tail -n 1)

if [ "$HTTP_STATUS" -ne 200 ]; then
    echo "CRITICAL: API returned Status $HTTP_STATUS" >> "$OUT"
    echo "RAW ERROR: $HTTP_BODY" >> "$OUT"
else
    # Extract content safely
    CONTENT=$(echo "$HTTP_BODY" | jq -r '.choices[0].message.content // "No content found in response"')
    echo "$CONTENT" >> "$OUT"
fi