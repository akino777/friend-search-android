#!/usr/bin/env bash
# 実アプリから dev 環境へ接続する UI テストを、専用アカウントで実行する（接続中のエミュレータ・端末が1台必要）。
# アカウントは friend-search-infra の scripts/create-uitest-users.sh が用意する（パスワードは作成時だけ設定し、以後は同じものを使う）。
#   使い方: aws sso login --profile friend-dev && ./scripts/android-live-uitest.sh [infraリポジトリのパス]
# ログ: .logs/android-live-uitest.log
set -euo pipefail
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
INFRA="${1:-$REPO_ROOT/../friend-search-infra}"
LOG_DIR="$REPO_ROOT/.logs"
LOG="$LOG_DIR/android-live-uitest.log"
mkdir -p "$LOG_DIR"

if ! adb get-state >/dev/null 2>&1; then
  echo "ERROR: 接続中のエミュレータ・端末がありません（例: emulator -avd Pixel_10 &）"
  exit 1
fi

"$INFRA/scripts/create-uitest-users.sh"
ACCOUNTS_FILE="$INFRA/.logs/uitest-accounts.json"

# インストルメンテーション引数は adb のコマンド行を通るため、JSON を Base64 にして渡す
ACCOUNTS_B64="$(base64 <"$ACCOUNTS_FILE" | tr -d '\n')"
echo "device: $(adb get-serialno) (log: $LOG)"
cd "$REPO_ROOT"
set +e
./gradlew --console=plain :app:connectedDevDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.friend_search.LiveUiTests \
  -Pandroid.testInstrumentationRunnerArguments.liveAccounts="$ACCOUNTS_B64" >"$LOG" 2>&1
RC=$?
set -e
grep -E "LiveUiTests|tests completed|FAILED|BUILD (SUCCESSFUL|FAILED)" "$LOG" | cut -c1-240 || true
exit $RC
