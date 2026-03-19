#!/usr/bin/env bash
set -euo pipefail

: "${GROQ_API_KEY:?GROQ_API_KEY is required in env/secrets}"
MODEL="${MODEL:-llama-3.1-8b-instant}"
OUT="${OUT:-ai_summary.txt}"

echo "===== AI ROOT CAUSE ANALYSIS =====" > "$OUT"

# Function to build and send JSON properly using jq
analyze_logs() {
    local content="$1"
    local system_prompt="$2"

    # Use jq to build the JSON safely (handles newlines and quotes)
    local payload=$(jq -n \
        --arg sys "$system_prompt" \
        --arg user "$content" \
        --arg model "$MODEL" \
        '{
            model: $model,
            messages: [
                {role: "system", content: $sys},
                {role: "user", content: $user}
            ],
            temperature: 0.2
        }')

    curl -sS https://api.groq.com/openai/v1/chat/completions \
        -H "Authorization: Bearer $GROQ_API_KEY" \
        -H "Content-Type: application/json" \
        -d "$payload"
}

# 1. Analyze each chunk
for f in chunk_*; do
    if [ -f "$f" ]; then
        echo -e "\n--- Analyzing $f ---\n" >> "$OUT"
        RAW_LOGS=$(cat "$f")
        SYSTEM_MSG="You are a senior DevOps/SRE. Analyze Kubernetes/CI logs and return: 1) Root cause (1-3 lines), 2) Failing component/pod, 3) Key error lines, 4) Probable fix steps. Be concise and actionable."
        
        # Capture API response and parse it
        API_RESPONSE=$(analyze_logs "$RAW_LOGS" "$SYSTEM_MSG")
        RESP=$(echo "$API_RESPONSE" | jq -r '.choices[0].message.content // "Error: API returned empty or invalid response. Check raw logs."')
        
        echo "$RESP" >> "$OUT"
    fi
done

# 2. Generate Final Merged Summary
echo -e "\n===== FINAL AI RCA =====\n" >> "$OUT"
SUMMARY_INPUT=$(cat "$OUT")
FINAL_SYSTEM_MSG="Combine partial analyses into ONE final RCA (root cause + fix). Keep under ~12 lines, crisp and specific."

FINAL_API_RESPONSE=$(analyze_logs "$SUMMARY_INPUT" "$FINAL_SYSTEM_MSG")
FINAL=$(echo "$FINAL_API_RESPONSE" | jq -r '.choices[0].message.content // "Final analysis failed. Check API response."')

echo "$FINAL" >> "$OUT"
