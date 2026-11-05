#!/bin/bash
# ══════════════════════════════════════════════════
#  Одна команда — собирает margyt.uifix.mtp
#  Нужно: JDK 17+, Android SDK (ANDROID_HOME задан),
#         plugin-api.jar в папке рядом
# ══════════════════════════════════════════════════
set -e

PLUGIN_ID="margyt.uifix"
SRC="java/cat/narezany/margyt/uifix/UiFix.java"
ANDROID_JAR="$ANDROID_HOME/platforms/android-34/android.jar"
D8="$ANDROID_HOME/build-tools/34.0.0/d8"

rm -rf _build && mkdir -p _build/classes _build/dex

echo "[1/3] javac..."
javac --release 8 \
  -cp "$ANDROID_JAR:plugin-api.jar" \
  -d _build/classes "$SRC"

echo "[2/3] d8 -> dex..."
"$D8" --min-api 26 \
  --lib "$ANDROID_JAR" \
  --classpath plugin-api.jar \
  --output _build/dex \
  $(find _build/classes -name '*.class')

echo "[3/3] zip -> .mtp..."
cp manifest.json _build/dex/
(cd _build/dex && zip -j "../../${PLUGIN_ID}.mtp" manifest.json classes.dex)

rm -rf _build
echo "✅ Готово: ${PLUGIN_ID}.mtp"
