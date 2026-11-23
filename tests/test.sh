#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p tests/out
javac -encoding UTF-8 -d tests/out app/src/main/java/com/example/froggypilot/RestaurantVision.java tests/VisionTest.java
java -Djava.awt.headless=true -cp tests/out VisionTest
