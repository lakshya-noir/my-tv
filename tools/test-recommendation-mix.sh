#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TEST_OUT="$(mktemp -d)"
trap 'rm -rf "$TEST_OUT"' EXIT
"${JAVA_HOME:+$JAVA_HOME/bin/}javac" -source 8 -target 8 -Xlint:-options -d "$TEST_OUT" \
  app/src/io/freemytv/RecommendationMix.java app/src/io/freemytv/SportsPolicy.java tests/RecommendationMixTest.java tests/SportsPolicyTest.java
"${JAVA_HOME:+$JAVA_HOME/bin/}java" -cp "$TEST_OUT" io.freemytv.RecommendationMixTest
"${JAVA_HOME:+$JAVA_HOME/bin/}java" -cp "$TEST_OUT" io.freemytv.SportsPolicyTest
