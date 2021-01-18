#!/bin/sh

set -x

kvm-ok

# start emulator
$ANDROID_HOME/emulator/emulator -avd $1 -noaudio -no-boot-anim -gpu off &

echo "Waiting for emulator to be ready"
timeout 10 adb wait-for-any-device || exit 1
echo "Emulator is ready"

# uninstall previous version
adb uninstall com.datadog.android.sdk.integration
adb uninstall com.datadog.android.sdk.integration.test

# Build the APKs
./gradlew :instrumented:integration:assembleDebug :instrumented:integration:assembleDebugAndroidTest --stacktrace

# Install the instrumentation app
adb install instrumented/integration/build/outputs/apk/debug/integration-debug.apk
# Install the instrumentation test app
adb install instrumented/integration/build/outputs/apk/androidTest/debug/integration-debug-androidTest.apk

# Run the instrumentation
adb shell am instrument -w com.datadog.android.sdk.integration.test/androidx.test.runner.AndroidJUnitRunner
