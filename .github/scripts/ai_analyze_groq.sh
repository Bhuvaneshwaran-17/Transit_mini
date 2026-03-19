#!/usr/bin/env bash

# Use Python to build the JSON payload since jq is missing
generate_payload() {
    python3 -c "import json, sys; print(json.dumps({'model': '$1', 'messages': [{'role': 'system', 'content': sys.argv[1]}, {'role': 'user', 'content': sys.argv[2]}], 'temperature': 0.2}))" "$2" "$3"
}

OUT="ai_summary.txt"
echo "===== AI ROOT CAUSE ANALYSIS =====" > "$OUT"

# 10 second sleep to avoid Groq Free Tier Rate Limits
for f in chunk_*; do
    [ -f "$f" ] || continue
    echo "Analyzing $f..."
    
    CONTENT=$(cat "$f")
    SYSTEM_MSG="You are a Senior DevOps Engineer. Analyze these logs and find the ROOT CAUSE of the Exit Code 1. Be brief."
    
    PAYLOAD=$(generate_payload "llama-3.1-8b-instant" "$SYSTEM_MSG" "$CONTENT")
    
    # Using -k to bypass the SSL revocation error on Windows
    RESPONSE=$(curl -k -sS https://api.groq.com/openai/v1/chat/completions \
        -H "Authorization: Bearer $GROQ_API_KEY" \
        -H "Content-Type: application/json" \
        -d "$PAYLOAD")

    # Extract content using Python
    RESULT=$(echo "$RESPONSE" | python3 -c "import json, sys; data=json.load(sys.stdin); print(data['choices'][0]['message']['content']) if 'choices' in data else print('ERROR: ' + str(data))")
    
    echo -e "\n--- $f ---\n$RESULT" >> "$OUT"
    sleep 10
done
