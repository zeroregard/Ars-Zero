#!/usr/bin/env bash
# Run the blight dungeon structure game test (Option C from structure self-test plan).
# Ensures no other Minecraft instance is using run/world before starting.
# Usage: from project root, ./scripts/run_structure_test.sh

set -e
cd "$(dirname "$0")/.."

if [[ -f run/world/session.lock ]]; then
  echo "Warning: run/world/session.lock exists. Close any running client/server using run/world, or remove the lock, then re-run."
  exit 1
fi

./gradlew runGameTestServer -Dars_zero.testFilter=BlightDungeonStructureTests --console=plain 2>&1 | tee run/logs/gametest_structure.log
echo "Check output above for test pass/fail. Grep for 'BlightDungeon' or 'PASSED'/'FAILED'."
