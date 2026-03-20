#!/usr/bin/env bash

# Use Python to build the JSON payload since jq is missing
# Temperature 0.0 makes the AI factual and prevents 'creative' guessing
generate_payload() {
    python3 -c "import json, sys; print(json.dumps({'model': '$1', 'messages': [{'role': 'system', 'content': sys.argv[1]}, {'role': 'user', 'content': sys.argv[2]}], 'temperature': 0.0}))" "$2" "$3"
}

OUT="ai_summary.txt"
echo "===== AI ROOT CAUSE ANALYSIS =====" > "$OUT"

# STEP 1: VERIFY DATA PROVENANCE
# If logs.txt doesn't exist or is basically empty, stop immediately.
if [ ! -s logs.txt ] || [ $(wc -l < logs.txt) -lt 5 ]; then
    echo "CRITICAL ERROR: No log data collected. Check 'maven_output.log' artifact." >> "$OUT"
    echo "RCA Status: Failed due to missing evidence."
    exit 0
fi

# THE ARCHITECT'S LOCKDOWN PROMPT
SYSTEM_MSG="You are a Senior DevOps Architect.
STRICT RULES:
1. ONLY analyze the text provided. Do NOT use outside knowledge.
2. If you see 'COMPILATION ERROR' or 'ERROR: <identifier> expected', identify the EXACT file and line number.
3. If the logs are empty or irrelevant, respond ONLY with 'INSUFFICIENT DATA'.
4. DO NOT mention Hibernate, PostgreSQL, or Dialects unless they appear in the CURRENT text.
5. Be brutal, direct, and name the 'Smoking Gun' (the primary error)."

# Iterate through the chunks created by chunk_logs.sh
for f in chunk_*; do
    [ -f "$f" ] || continue

    # Skip chunks that are effectively empty to save API quota
    if [ ! -s "$f" ]; then continue; fi

    echo "Analyzing $f..."
    CONTENT=$(cat "$f")

    PAYLOAD=$(generate_payload "llama-3.1-8b-instant" "$SYSTEM_MSG" "$CONTENT")

    # Using -k to bypass SSL issues on Windows/WSL environments
    RESPONSE=$(curl -k -sS https://api.groq.com/openai/v1/chat/completions \
        -H "Authorization: Bearer $GROQ_API_KEY" \
        -H "Content-Type: application/json" \
        -d "$PAYLOAD")

    # Extract content using Python (Safe extraction)
    RESULT=$(echo "$RESPONSE" | python3 -c "import json, sys; data=json.load(sys.stdin); print(data['choices'][0]['message']['content']) if 'choices' in data and data['choices'] else print('ERROR: AI could not parse this chunk or API limit hit.')")

    # Append to summary only if it's not 'INSUFFICIENT DATA'
    if [[ "$RESULT" != *"INSUFFICIENT DATA"* ]]; then
        echo -e "\n--- Evidence in $f ---\n$RESULT" >> "$OUT"
    fi

    # Protect Groq Free Tier Rate Limits
    sleep 10
done