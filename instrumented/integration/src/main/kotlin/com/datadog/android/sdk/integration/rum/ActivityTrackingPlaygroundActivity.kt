/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sdk.integration.rum

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.datadog.android.Datadog
import com.datadog.android.rum.GlobalRum
import com.datadog.android.rum.RumMonitor
import com.datadog.android.rum.tracking.ActivityViewTrackingStrategy
import com.datadog.android.sdk.integration.R
import com.datadog.android.sdk.integration.RuntimeConfig
import com.datadog.android.sdk.utils.getTrackingConsent

internal class ActivityTrackingPlaygroundActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.wtf("ActivityTrackingPlaygroundActivity", "onCreate()")

        val credentials = RuntimeConfig.credentials()
        // we will use a large long task threshold to make sure we will not have LongTask events
        // noise in our integration tests.
        val config = RuntimeConfig.configBuilder()
            .trackInteractions()
            .trackLongTasks(RuntimeConfig.LONG_TASK_LARGE_THRESHOLD)
            .useViewTrackingStrategy(ActivityViewTrackingStrategy(true))
            .build()
        val trackingConsent = intent.getTrackingConsent()

        Datadog.initialize(this, credentials, config, trackingConsent)
        Datadog.setVerbosity(Log.VERBOSE)

        GlobalRum.registerIfAbsent(RumMonitor.Builder().build())
        setContentView(R.layout.fragment_tracking_layout)
    }

    override fun onRestart() {
        super.onRestart()
        Log.wtf("ActivityTrackingPlaygroundActivity", "onRestart()")
    }

    override fun onStart() {
        super.onStart()
        Log.wtf("ActivityTrackingPlaygroundActivity", "onStart()")
    }

    override fun onResume() {
        super.onResume()
        Log.wtf("ActivityTrackingPlaygroundActivity", "onResume()")
    }

    override fun onPostResume() {
        super.onPostResume()
        Log.wtf("ActivityTrackingPlaygroundActivity", "onPostResume()")
    }

    override fun onPause() {
        super.onPause()
        Log.wtf("ActivityTrackingPlaygroundActivity", "onPause()")
    }

    override fun onStop() {
        super.onStop()
        Log.wtf("ActivityTrackingPlaygroundActivity", "onStop()")
    }
}
