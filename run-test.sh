#!/bin/bash
# Quick test runner for MultiphaseSpellTurretTemporalContextTest
# Usage: ./run-test.sh

cd "$(dirname "$0")"

echo "Running MultiphaseSpellTurretTemporalContextTest..."
echo "This will start a GameTest server, run the test, and exit."
echo ""

# Clear previous debug log
rm -f /Users/mathiassiignorregaard/Documents/Projects/Mods/.cursor/debug.log

# Run the test with filter
./gradlew runGameTestServer -Dfilter=MultiphaseSpellTurretTemporalContextTest 2>&1 | tee test-output.log

echo ""
echo "Test completed. Check test-output.log for results."
echo "Debug log: /Users/mathiassiignorregaard/Documents/Projects/Mods/.cursor/debug.log"
