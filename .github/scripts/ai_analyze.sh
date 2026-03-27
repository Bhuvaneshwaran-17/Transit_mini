#!/usr/bin/env bash

# File: .github/scripts/ai_analyze.sh
OUT="ai_summary.txt"
LOG_FILE="maven_output.log"

echo "===== AI ROOT CAUSE ANALYSIS (MODELS GATEWAY) =====" > "$OUT"

# 1. DEFINE ERROR CATEGORIES & KEYWORDS
PATTERN_INFRA="OutOfMemoryError|No space left on device|Connection refused|timed out|UnsupportedClassVersionError|major.minor version|Port.*already in use|ConnectorConfigException"
PATTERN_DEP="package .* does not exist|symbol: class|DependencyResolutionException|could not resolve dependencies"
PATTERN_SYNTAX="error: ';' expected|error: not a statement|error: cannot find symbol|error: invalid method declaration"
# IMPROVED: Specifically catches the Surefire/JUnit failure patterns
PATTERN_TEST="Tests run: .* Failures: [1-9]|there are test failures|failed to execute goal .*maven-surefire-plugin|expected: <.*> but was: <.*>"

# 2. PERFORM TRIAGE (PRIORITY-ORDERED)
CATEGORY="GENERAL"
HINT=""

# PRIORITY 1: INFRASTRUCTURE (Environment is broken)
if grep -iE -q "$PATTERN_INFRA" "$LOG_FILE"; then
    CATEGORY="INFRASTRUCTURE"
    HINT=$(grep -iE "$PATTERN_INFRA" "$LOG_FILE" | head -n 3)
    INSTRUCTION="CRITICAL: Environment Failure. Check for Java Version Mismatch or Port conflicts. Fix the Runner or properties."

# PRIORITY 2: SYNTAX (Code is uncompilable)
elif grep -iE -q "$PATTERN_SYNTAX" "$LOG_FILE"; then
    CATEGORY="SYNTAX"
    HINT=$(grep -iE "$PATTERN_SYNTAX" "$LOG_FILE" | head -n 5)
    INSTRUCTION="CRITICAL: Java Syntax Error. Identify the typo. Suggest a one-line code fix."

# PRIORITY 3: DEPENDENCY (Missing libraries)
elif grep -iE -q "$PATTERN_DEP" "$LOG_FILE"; then
    CATEGORY="DEPENDENCY"
    HINT=$(grep -iE "$PATTERN_DEP" "$LOG_FILE" | head -n 3)
    INSTRUCTION="CRITICAL: Dependency Issue. Identify the missing XML dependency for pom.xml."

# PRIORITY 4: TEST FAILURES (Logic is wrong)
elif grep -iE -q "$PATTERN_TEST" "$LOG_FILE"; then
    CATEGORY="TEST"
    # SURGICAL EXTRACTION: Find the exact 'expected/but was' line or the failing test name
    HINT=$(grep -E "expected: <.*> but was: <.*>|[a-zA-Z0-9_]+\.[a-zA-Z0-9_]+:[0-9]+" "$LOG_FILE" | head -n 5)
    INSTRUCTION="CRITICAL: Unit Test Failure. Identify the failed assertion (e.g., expected 400 but was 401). Suggest a logic fix."

else
    HINT=$(tail -n 15 "$LOG_FILE")
    INSTRUCTION="Unknown build failure. Analyze logs and find the most likely root cause."
fi

# 3. SURGICAL EXTRACTION FOR TARGET FILE
# Improved to find Test Classes even if they aren't in the standard error format
RAW_LOC=$(grep -oE "[a-zA-Z0-9/_.-]+\.java:\[[0-9]+,[0-9]+\]|[a-zA-Z0-9_]+Test\.[a-zA-Z0-9_]+:[0-9]+" "$LOG_FILE" | head -n 1)
FILE_PATH=$(echo "$RAW_LOC" | cut -d: -f1 | sed "s|^.*/src/|src/|")
[ -z "$FILE_PATH" ] && FILE_PATH="pom.xml or Infrastructure"

# 4. SLIM LOG CONTEXT
LOG_CONTEXT=$(grep -iE -B 2 -A 8 "ERROR|FAILURE|Exception|expected:" "$LOG_FILE" | head -n 25 | tr -cd '[:alnum:].[:space:]:/-[],')

# 5. THE PRODUCTION CALL
echo "Detected Category: $CATEGORY. Requesting RCA..." >> "$OUT"

PAYLOAD=$(jq -n \
  --arg sys "You are a Senior DevOps and Spring Boot Architect.
              Current Task: $INSTRUCTION
              STRICT FORMAT:
              PATH: [Target File]
              CATEGORY: $CATEGORY
              ERROR: [One sentence technical cause]
              FIX: [One line solution]
              NO PROSE. NO ADVICE." \
  --arg user "Context Logs: $LOG_CONTEXT \nEvidence: $HINT \nTarget: $FILE_PATH" \
  '{model: "gpt-4o", messages: [{role: "system", content: $sys}, {role: "user", content: $user}]}')

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "https://models.inference.ai.azure.com/chat/completions" \
  -H "Authorization: Bearer $GH_TOKEN" \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD")

HTTP_STATUS=$(echo "$RESPONSE" | tail -n 1)
HTTP_BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_STATUS" -eq 200 ]; then
    echo "$HTTP_BODY" | jq -r '.choices[0].message.content' >> "$OUT"
    echo "SUCCESS: $CATEGORY RCA generated."
else
    echo "API FAILURE: $HTTP_STATUS" >> "$OUT"
    echo "$HTTP_BODY" | head -c 100 >> "$OUT"
fi