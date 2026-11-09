#!/usr/bin/env bash
# dev / prod のビルドとユニットテスト。実環境 UI テストは scripts/android-live-uitest.sh で行う。
#   使い方: ./scripts/android-verify.sh
# ログ: .logs/android-verify.log
set -euo pipefail
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOG_DIR="$REPO_ROOT/.logs"
LOG="$LOG_DIR/android-verify.log"
mkdir -p "$LOG_DIR"

cd "$REPO_ROOT"
set +e
./gradlew --console=plain :app:assembleDevDebug :app:assembleProdRelease :app:testDevDebugUnitTest >"$LOG" 2>&1
RC=$?
set -e
grep -E "^e: |^w: |FAILED|tests completed|BUILD (SUCCESSFUL|FAILED)" "$LOG" || true
echo "log: $LOG"
exit $RC
