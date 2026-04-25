#!/bin/bash

# Cleanup GitHub Actions runs - keep only last 2 per workflow
# Usage: ./cleanup-workflow-runs.sh [owner/repo]
#
# If no argument provided, defaults to N0t4R0b0t/BoringOldCRM
# Requires: gh CLI installed and authenticated (gh auth status)
#
# Example:
#   ./scripts/cleanup-workflow-runs.sh
#   ./scripts/cleanup-workflow-runs.sh owner/repo

set -e

REPO="${1:-N0t4R0b0t/BoringOldCRM}"

echo "🧹 Cleaning up all workflow runs for $REPO..."
echo "   Keeping the last 2 runs per workflow"
echo ""

# Verify gh CLI is installed and authenticated
if ! command -v gh &> /dev/null; then
  echo "❌ Error: 'gh' CLI not found. Install from https://cli.github.com"
  exit 1
fi

if ! gh auth status &> /dev/null; then
  echo "❌ Error: Not authenticated with GitHub. Run: gh auth login"
  exit 1
fi

# Get all workflows
WORKFLOWS=$(gh api repos/$REPO/actions/workflows --paginate -q '.workflows[] | "\(.name)|\(.id)"' 2>/dev/null || echo "")

if [ -z "$WORKFLOWS" ]; then
  echo "❌ Error: Could not fetch workflows. Check repo name and permissions."
  exit 1
fi

WORKFLOW_COUNT=0
TOTAL_DELETED=0

# Process each workflow
echo "$WORKFLOWS" | while IFS='|' read -r NAME ID; do
  WORKFLOW_COUNT=$((WORKFLOW_COUNT + 1))
  echo "📋 $NAME"

  # Get all runs for this workflow
  RUNS=$(gh api repos/$REPO/actions/workflows/$ID/runs --paginate -q '.workflow_runs[].id' 2>/dev/null || echo "")

  if [ -z "$RUNS" ]; then
    echo "   └─ No runs found"
    return 0
  fi

  RUN_ARRAY=($RUNS)
  TOTAL=${#RUN_ARRAY[@]}

  if [ $TOTAL -gt 2 ]; then
    TO_DELETE=$((TOTAL - 2))
    echo "   └─ Found $TOTAL runs, deleting $TO_DELETE (keeping last 2)..."

    # Delete all except first 2
    for i in $(seq 2 $((TOTAL - 1))); do
      RUN_ID=${RUN_ARRAY[$i]}
      if gh api repos/$REPO/actions/runs/$RUN_ID -X DELETE 2>/dev/null; then
        TOTAL_DELETED=$((TOTAL_DELETED + 1))
        echo "      ✓ Deleted run $RUN_ID"
      else
        echo "      ⚠ Failed to delete run $RUN_ID"
      fi
    done
  else
    echo "   └─ Found $TOTAL runs (keeping all)"
  fi

  echo ""
done

echo "✅ Cleanup complete!"
echo "   Deleted: $TOTAL_DELETED runs"
echo "   Repository: $REPO"
