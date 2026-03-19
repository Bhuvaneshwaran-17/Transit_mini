#!/usr/bin/env bash
set -euo pipefail

: "${GROQ_API_KEY:?GROQ_API_KEY is required}"
MODEL="llama-3.1-8b-instant"
OUT="ai_summary.txt"

echo "===== AI ROOT CAUSE ANALYSIS =====" > "$OUT"

analyze_logs() {
    local content="$1"
    local system_prompt="$2"
    local payload=$(jq -n --arg sys "$system_prompt" --arg user "$content" --arg model "$MODEL" \
        '{model: $model, messages: [{role: "system", content: $sys}, {role: "user", content: $user}], temperature: 0.2}')

    local response=$(curl -sS https://api.groq.com/openai/v1/chat/completions \
        -H "Authorization: Bearer $GROQ_API_KEY" \
        -H "Content-Type: application/json" \
        -d "$payload")

    if echo "$response" | jq -e '.choices[0].message.content' >/dev/null 2>&1; then
        echo "$response" | jq -r '.choices[0].message.content'
    else
        echo "API_ERROR: $(echo "$response" | jq -c '.error // .')" >&2
        return 1
    fi
}

for f in chunk_*; do
    [ -f "$f" ] || continue
    echo -e "\n--- Analyzing $f ---\n" >> "$OUT"
    RAW_LOGS=$(cat "$f")
    SYSTEM_MSG="You are a senior DevOps/SRE. Analyze these logs for the ROOT CAUSE only. Be extremely brief."
    
    if RESP=$(analyze_logs "$RAW_LOGS" "$SYSTEM_MSG"); then
        echo "$RESP" >> "$OUT"
    else
        echo "Skipping $f due to API error." >> "$OUT"
    fi
done

echo -e "\n===== FINAL AI RCA =====\n" >> "$OUT"
SUMMARY_INPUT=$(cat "$OUT")
FINAL_SYSTEM_MSG="Summarize the findings into ONE final RCA (root cause + fix) under 10 lines."
analyze_logs "$SUMMARY_INPUT" "$FINAL_SYSTEM_MSG" >> "$OUT" || echo "Final summary failed." >> "$OUT"
