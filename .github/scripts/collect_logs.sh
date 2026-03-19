#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${NAMESPACE:-apps}"
OUTFILE="${OUTFILE:-logs.txt}"

echo "==== POD LIST ====" > "$OUTFILE"
kubectl get pods -n "$NAMESPACE" >> "$OUTFILE" 2>&1 || true

echo -e "\n==== POD LOGS ====" >> "$OUTFILE"
pods=$(kubectl get pods -n "$NAMESPACE" -o jsonpath='{.items[*].metadata.name}' 2>/dev/null || true)
for p in $pods; do
  echo -e "\n--- $p ---" >> "$OUTFILE"
  # Current logs
  kubectl logs -n "$NAMESPACE" "$p" --tail=2000 >> "$OUTFILE" 2>&1 || true
  # If in CrashLoopBackOff, try previous container logs too
  if kubectl get pod -n "$NAMESPACE" "$p" -o jsonpath='{.status.containerStatuses[0].state.waiting.reason}' 2>/dev/null | grep -q 'CrashLoopBackOff'; then
    echo -e "\n[previous logs]" >> "$OUTFILE"
    kubectl logs -n "$NAMESPACE" "$p" --previous --tail=2000 >> "$OUTFILE" 2>&1 || true
  fi
done

echo -e "\n==== EVENTS (sorted) ====" >> "$OUTFILE"
kubectl get events -n "$NAMESPACE" --sort-by='.metadata.creationTimestamp' >> "$OUTFILE" 2>&1 || true
