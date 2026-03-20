#!/usr/bin/env bash

# Secure JSON payload generator
generate_payload() {
    python3 -c "import json, sys; print(json.dumps({'model': '$1', 'messages': [{'role': 'system', 'content': sys.argv[1]}, {'role': 'user', 'content': sys.argv[2]}], 'temperature': 0.0}))" "$2" "$3"
}

OUT="ai_summary.txt"
echo "===== AI ROOT CAUSE ANALYSIS (SURGICAL) =====" > "$OUT"

# 1. VERIFY DATA
if [ ! -s logs.txt ]; then
    echo "CRITICAL ERROR: logs.txt is empty. Check your artifact download step." >> "$OUT"
    exit 0
fi

# STEP 2: ARCHITECT'S PRECISION FILTER
# 1. Look for the "COMPILATION ERROR" marker and grab 20 lines after it.
# 2. Also grab the "BUILD FAILURE" summary.
# 3. This ensures the AI ONLY sees the 'Alphabet Error' and NOT the Hibernate noise.
ERROR_ZONE=$(grep -A 20 "COMPILATION ERROR" logs.txt)
SUMMARY_ZONE=$(grep -A 10 "BUILD FAILURE" logs.txt)

# Combine them and sanitize
CLEAN_LOGS=$(echo -e "$ERROR_ZONE\n$SUMMARY_ZONE" | tr -d '\000-\031' | sed 's/\\/\\\\/g' | sed 's/"/\\"/g' | head -c 4000)

# IF THE LOGS ARE EMPTY (No compilation error found), fallback to the last 50 lines
if [ -z "$ERROR_ZONE" ]; then
    CLEAN_LOGS=$(tail -n 50 logs.txt | tr -d '\000-\031' | sed 's/\\/\\\\/g' | sed 's/"/\\"/g')
fi

# 3. LOCKDOWN PROMPT
SYSTEM_MSG="You are a Senior SRE.
1. Identify the EXACT file and line number of the COMPILATION ERROR.
2. Ignore database/hibernate issues if a Java syntax error exists.
3. Be brief. Maximum 3 bullet points."

echo "Sending surgical logs to Groq (Tokens: ~1500/6000)..."

# 4. EXECUTE
PAYLOAD=$(generate_payload "llama-3.1-8b-instant" "$SYSTEM_MSG" "$CLEAN_LOGS")

RESPONSE=$(curl -k -sS https://api.groq.com/openai/v1/chat/completions \
    -H "Authorization: Bearer $GROQ_API_KEY" \
    -H "Content-Type: application/json" \
    -d "$PAYLOAD")

# 5. PARSE RESPONSE
RESULT=$(echo "$RESPONSE" | python3 -c "import json, sys;
try:
    data=json.load(sys.stdin)
    if 'choices' in data:
        print(data['choices'][0]['message']['content'])
    else:
        # Prints the actual Groq error if limit hit
        print('GROQ API REJECTION: ' + str(data.get('error', {}).get('message', 'Unknown Error')))
except:
    print('ERROR: Response was not valid JSON.')
")

echo -e "\n$RESULT" >> "$OUT"
echo "Analysis complete. Output saved to $OUT"