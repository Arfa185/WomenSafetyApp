#!/usr/bin/env bash
# Run this once inside your GitHub Codespace terminal to set up Android build tooling.
# Usage: bash setup-codespace.sh
set -e

echo "== Installing JDK 17 (if not already present) =="
if ! java -version 2>&1 | grep -q "17\."; then
    sudo apt-get update -y
    sudo apt-get install -y openjdk-17-jdk
fi

echo "== Downloading Android command-line tools =="
export ANDROID_HOME="$HOME/android-sdk"
mkdir -p "$ANDROID_HOME/cmdline-tools"
cd "$ANDROID_HOME/cmdline-tools"
if [ ! -d "latest" ]; then
    curl -sSL -o cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
    unzip -q cmdline-tools.zip
    mv cmdline-tools latest
    rm cmdline-tools.zip
fi

export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

echo "== Accepting licenses and installing SDK packages =="
yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses > /dev/null
sdkmanager --sdk_root="$ANDROID_HOME" "platform-tools" "platforms;android-34" "build-tools;34.0.0"

echo "== Writing local.properties =="
cd -
echo "sdk.dir=$ANDROID_HOME" > local.properties

echo "== Persisting env vars for future shells =="
{
  echo "export ANDROID_HOME=$ANDROID_HOME"
  echo "export PATH=\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$PATH"
} >> ~/.bashrc

echo ""
echo "Setup done. Now run:"
echo "  gradle wrapper --gradle-version 8.4   # generates gradlew (one-time)"
echo "  ./gradlew assembleDebug                # builds the APK"
echo ""
echo "Built APK will be at: app/build/outputs/apk/debug/app-debug.apk"
