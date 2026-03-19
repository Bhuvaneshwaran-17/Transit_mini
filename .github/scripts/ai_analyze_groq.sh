#!/usr/bin/env bash
set -euo pipefail

: "${GROQ_API_KEY:?GROQ_API_KEY is required in env/secrets}"
MODEL="${MODEL:-llama3-8b-8192}"
OUT="${OUT:-ai_summary.txt}"

echo "===== AI ROOT CAUSE ANALYSIS =====" > "$OUT"

# Require jq (GitHub runners usually have it, but check)
if ! command -v jq >/dev/null 2>&1; then
  echo "[info] jq not found; attempting to install..."
  sudo apt-get update -y && sudo apt-get install -y jq
fi

# Analyze each chunk
for f in chunk_*; do
  [ -f "$f" ] || continue
  echo -e "\n--- Analyzing $f ---\n" >> "$OUT"
  # Escape double-quotes for JSON safety
  CHUNK_ESCAPED=$(sed 's/"/\\"/g' "$f")
  RESP=$(curl -sS https://api.groq.com/openai/v1/chat/completions \
    -H "Authorization: Bearer $GROQ_API_KEY" \
    -H "Content-Type: application/json" \
    -d "{
          \"model\": \"$MODEL\",
          \"messages\": [
            {\"role\": \"system\", \"content\": \"You are a senior DevOps/SRE. Analyze Kubernetes/CI logs and return: 1) Root cause (1-3 lines), 2) Failing component/pod, 3) Key error lines, 4) Probable fix steps. Be concise and actionable.\"},
            {\"role\": \"user\", \"content\": \"$CHUNK_ESCAPED\"}
          ],
          \"temperature\": 0.2
        }" \
  | jq -r '.choices[0].message.content // "No content"')
  echo "$RESP" >> "$OUT"
done

# Final merged summary
ALL_ESCAPED=$(sed 's/"/\\"/g' "$OUT")
FINAL=$(curl -sS https://api.groq.com/openai/v1/chat/completions \
  -H "Authorization: Bearer $GROQ_API_KEY" \
  -H "Content-Type: application/json" \
  -d "{
        \"model\": \"$MODEL\",
        \"messages\": [
          {\"role\": \"system\", \"content\": \"Combine partial analyses into ONE final RCA (root cause + fix). Keep under ~12 lines, crisp and specific.\"},
          {\"role\": \"user\", \"content\": \"$ALL_ESCAPED\"}
        ],
        \"temperature\": 0.1
      }" \
| jq -r '.choices[0].message.content // "No content"')

echo -e "\n===== FINAL AI RCA =====\n$FINAL" >> "$OUT"
