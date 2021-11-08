/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.rum.webview

import android.webkit.JavascriptInterface
import com.datadog.android.core.internal.CoreFeature
import com.datadog.android.rum.internal.RumFeature
import com.google.gson.JsonArray

/**
 * This [JavascriptInterface] is used to intercept all the Datadog events produced by
 * the displayed web content when there is already a Datadog browser-sdk attached.
 * The goal is to make those events part of the mobile session.
 * Please note that the WebView will not be tracked unless you add the host name in the hosts
 * lists in the SDK configuration.
 * @see [com.datadog.android.core.configuration.Configuration.Builder]
 */
class DatadogEventBridge {

    private val webEventConsumer: WebEventConsumer = WebEventConsumer(
        WebRumEventConsumer(
            RumFeature.persistenceStrategy.getWriter(),
            CoreFeature.timeProvider
        ),
        WebLogEventConsumer()
    )

    // region Bridge

    /**
     * Called from the browser-sdk side whenever there is a new RUM/LOG event
     * available related with the tracked WebView.
     * @param event as the bundled web event as a Json string
     */
    @JavascriptInterface
    fun send(event: String) {
        webEventConsumer.consume(event)
    }

    /**
     * Called from the browser-sdk to get the list of hosts for which the WebView tracking is
     * allowed.
     * @return the list of hosts as a String JsonArray
     */
    @JavascriptInterface
    fun getAllowedWebViewHosts(): String {
        // We need to use a JsonArray here otherwise it cannot be parsed on the JS side
        val origins = JsonArray()
        CoreFeature.webViewTrackingHosts.forEach {
            origins.add(it)
        }
        return origins.toString()
    }

    // endregion
}