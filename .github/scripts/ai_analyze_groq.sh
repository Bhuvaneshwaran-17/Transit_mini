#!/usr/bin/env bash

# Use Python to build the JSON payload securely
# Temperature 0.0 forces the AI to be a literal log-reader
generate_payload() {
    python3 -c "import json, sys; print(json.dumps({'model': '$1', 'messages': [{'role': 'system', 'content': sys.argv[1]}, {'role': 'user', 'content': sys.argv[2]}], 'temperature': 0.0}))" "$2" "$3"
}

OUT="ai_summary.txt"
echo "===== AI ROOT CAUSE ANALYSIS (SINGLE-SHOT) =====" > "$OUT"

# STEP 1: VERIFY DATA PRESENCE
if [ ! -s logs.txt ]; then
    echo "CRITICAL ERROR: logs.txt is missing or empty. No evidence to analyze." >> "$OUT"
    exit 0
fi

# STEP 2: SANITIZE LOGS (The "Payload Fixer")
# This removes special characters and backslashes that cause 'AI could not parse' errors
CLEAN_LOGS=$(cat logs.txt | tr -d '\000-\031' | sed 's/\\/\\\\/g' | sed 's/"/\\"/g' | tail -n 2000)

# THE ARCHITECT'S LOCKDOWN PROMPT
SYSTEM_MSG="You are a Senior DevOps Architect.
STRICT RULES:
1. LOOK AT 'CI/CD BUILD LOGS' FIRST.
2. If you see 'COMPILATION ERROR' or 'ERROR: <identifier> expected', that is the ONLY ROOT CAUSE.
3. DO NOT mention Hibernate, PostgreSQL, or Dialects if there is a Java syntax error in the build section.
4. Identify the EXACT file and line number (e.g., AuthController.java:21).
5. Be brutal, direct, and ignore stale Kubernetes logs if the build failed."

echo "Sending full logs to Groq AI..."

# STEP 3: EXECUTE SINGLE API CALL
PAYLOAD=$(generate_payload "llama-3.1-8b-instant" "$SYSTEM_MSG" "$CLEAN_LOGS")

RESPONSE=$(curl -k -sS https://api.groq.com/openai/v1/chat/completions \
    -H "Authorization: Bearer $GROQ_API_KEY" \
    -H "Content-Type: application/json" \
    -d "$PAYLOAD")

# STEP 4: EXTRACT AND VALIDATE
RESULT=$(echo "$RESPONSE" | python3 -c "import json, sys;
try:
    data=json.load(sys.stdin)
    if 'choices' in data:
        print(data['choices'][0]['message']['content'])
    else:
        print('ERROR: Groq API Error - ' + str(data.get('error', 'Unknown Error')))
except Exception as e:
    print('ERROR: Failed to parse JSON response from AI.')
")

echo -e "\n$RESULT" >> "$OUT"

# STEP 5: CLEANUP
# Keep logs.txt for debugging but the AI logic is done.
echo "Analysis complete. Results in $OUT"