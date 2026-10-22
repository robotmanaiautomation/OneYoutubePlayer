#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
: "${ONEPLAYER_JDK17:=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
: "${ONEPLAYER_JDK25:=/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$ROOT/.local/gradle}"
# Regeneration deliberately replaces only the marked generated copy.
python3 tools/prepare_smarttube.py
JAVA_HOME="$ONEPLAYER_JDK17" .generated/smarttube/gradlew -p .generated/smarttube \
    -I "$ROOT/tools/export_smarttube.gradle" exportOnePlayerRuntime --console=plain
JAVA_HOME="$ONEPLAYER_JDK25" ./gradlew :app:assembleDebug :playback-core:test --console=plain
python3 -m unittest discover -s tools -p 'test_prepare_smarttube.py' -v
