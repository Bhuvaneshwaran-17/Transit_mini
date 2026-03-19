#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${NAMESPACE:-apps}"
OUTFILE="logs.txt"

echo "==== [CRITICAL] CI/CD BUILD LOGS (MAVEN) ====" > "$OUTFILE"
if [ -f "maven_output.log" ]; then
    # Grab the last 1000 lines of the actual build failure fetched via GH CLI
    tail -n 1000 maven_output.log >> "$OUTFILE"
else
    echo "No maven_output.log found. Error likely happened at the K8s level." >> "$OUTFILE"
fi

echo -e "\n==== KUBERNETES POD STATUS ====" >> "$OUTFILE"
kubectl get pods -n "$NAMESPACE" >> "$OUTFILE" 2>&1 || true

echo -e "\n==== KUBERNETES POD LOGS (FRESH ONLY) ====" >> "$OUTFILE"
pods=$(kubectl get pods -n "$NAMESPACE" -o jsonpath='{.items[*].metadata.name}' 2>/dev/null || true)
for p in $pods; do
  echo -e "\n--- Pod: $p ---" >> "$OUTFILE"
  # --since=5m KILLS THE POSTGRES HALLUCINATION
  kubectl logs -n "$NAMESPACE" "$p" --tail=500 --since=5m >> "$OUTFILE" 2>&1 || true
done