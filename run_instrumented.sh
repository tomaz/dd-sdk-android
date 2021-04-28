#!/bin/sh

# uninstall previous version
adb uninstall com.datadog.android.sdk.integration
adb uninstall com.datadog.android.sdk.integration.test

# Build the APKs
./gradlew clean :instrumented:integration:assembleDebug :instrumented:integration:assembleDebugAndroidTest

# Install the Shoppist app
adb install instrumented/integration/build/outputs/apk/debug/integration-debug.apk
# Install the instrumentation app
adb install instrumented/integration/build/outputs/apk/androidTest/debug/integration-debug-androidTest.apk

# Run the instrumentation
adb shell am instrument -w com.datadog.android.sdk.integration.test/androidx.test.runner.AndroidJUnitRunner
