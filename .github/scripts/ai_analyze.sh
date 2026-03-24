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

# 3. EXECUTE VIA GITHUB COPILOT API (The Stable Way)
echo "Consulting Copilot API for Root Cause..." >> "$OUT"

# We use the GH_TOKEN you added to your secrets
# The prompt is structured for a 3-bullet response
RESPONSE=$(curl -s -X POST "https://api.github.com/copilot/chat/completions" \
  -H "Authorization: Bearer $GH_TOKEN" \
  -H "Content-Type: application/json" \
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

# 4. EXTRACT CONTENT (Parsing the JSON response)
# We use 'jq' if available, otherwise raw sed to pull the message content
if command -v jq >/dev/null 2>&1; then
    echo "$RESPONSE" | jq -r '.choices[0].message.content' >> "$OUT"
else
    echo "$RESPONSE" | sed -n 's/.*"content": "\(.*\)".*/\1/p' >> "$OUT"
fi

echo -e "\n--------------------------------------------" >> "$OUT"