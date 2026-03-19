#!/usr/bin/env bash

# Use Python to build the JSON payload since jq is missing
generate_payload() {
    python3 -c "import json, sys; print(json.dumps({'model': '$1', 'messages': [{'role': 'system', 'content': sys.argv[1]}, {'role': 'user', 'content': sys.argv[2]}], 'temperature': 0.1}))" "$2" "$3"
}

OUT="ai_summary.txt"
echo "===== AI ROOT CAUSE ANALYSIS =====" > "$OUT"

# THE ARCHITECT'S PROMPT: Forces the AI to ignore stale K8s logs if the Build failed.
SYSTEM_MSG="You are a Senior DevOps Architect.
1. Look at 'CI/CD BUILD LOGS' first. If you see 'COMPILATION ERROR' or 'ERROR: <identifier> expected', that is the ONLY ROOT CAUSE.
2. Do NOT blame PostgreSQL, Hibernate, or Database connections if the Build Logs show a Java syntax error.
3. Ignore stale logs older than 5 minutes.
4. Identify the EXACT file and line number (e.g., AuthController.java:21).
5. Be brutal, direct, and brief."

# 10 second sleep to avoid Groq Free Tier Rate Limits
for f in chunk_*; do
    [ -f "$f" ] || continue
    echo "Analyzing $f..."

    CONTENT=$(cat "$f")

    PAYLOAD=$(generate_payload "llama-3.1-8b-instant" "$SYSTEM_MSG" "$CONTENT")

    # Using -k to bypass the SSL revocation error on Windows
    RESPONSE=$(curl -k -sS https://api.groq.com/openai/v1/chat/completions \
        -H "Authorization: Bearer $GROQ_API_KEY" \
        -H "Content-Type: application/json" \
        -d "$PAYLOAD")

    # Extract content using Python
    RESULT=$(echo "$RESPONSE" | python3 -c "import json, sys; data=json.load(sys.stdin); print(data['choices'][0]['message']['content']) if 'choices' in data else print('ERROR: AI response failed. Check logs.')")

    echo -e "\n--- Analysis for $f ---\n$RESULT" >> "$OUT"

    # Keep the 10s sleep to protect your Groq Free Tier quota
    sleep 10
done