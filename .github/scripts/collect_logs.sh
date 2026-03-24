#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${NAMESPACE:-apps}"
OUTFILE="logs.txt"

echo "==== [CRITICAL] CI/CD BUILD LOGS (MAVEN) ====" > "$OUTFILE"

# 1. Grab the actual compilation error from the GitHub CLI output
if [ -f "maven_output.log" ]; then
    # We take 2000 lines to ensure we don't miss the 'Alphabet Error' stack trace
    tail -n 2000 maven_output.log >> "$OUTFILE"
else
    echo "No maven_output.log found. Error likely at infrastructure level." >> "$OUTFILE"
fi

echo -e "\n==== KUBERNETES POD LOGS (LATEST 5 MINUTES ONLY) ====" >> "$OUTFILE"
# This loop kills the 'Hibernate Hallucination' by ignoring old errors
pods=$(kubectl get pods -n "$NAMESPACE" -o jsonpath='{.items[*].metadata.name}' 2>/dev/null || true)
for p in $pods; do
  echo -e "\n--- Pod Context: $p ---" >> "$OUTFILE"
  # --since=5m is the key. It ignores the Postgres restart from yesterday.
  kubectl logs -n "$NAMESPACE" "$p" --tail=200 --since=5m >> "$OUTFILE" 2>&1 || true
done