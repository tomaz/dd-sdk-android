/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.rum.webview

import com.datadog.android.core.internal.persistence.DataWriter
import com.datadog.android.core.internal.time.TimeProvider
import com.datadog.android.core.internal.utils.sdkLogger
import com.datadog.android.rum.internal.domain.RumContext
import com.datadog.android.rum.model.ActionEvent
import com.datadog.android.rum.model.ErrorEvent
import com.datadog.android.rum.model.LongTaskEvent
import com.datadog.android.rum.model.ResourceEvent
import com.datadog.android.rum.model.ViewEvent
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import java.util.Locale.US
import kotlin.collections.LinkedHashMap

internal class WebRumEventConsumer(
    private val dataWriter: DataWriter<Any>,
    private val timeProvider: TimeProvider,
    private val webRumEventMapper: WebRumEventMapper = WebRumEventMapper(),
    private val contextProvider: WebRumEventContextProvider = WebRumEventContextProvider()
) {

    internal val offsets: LinkedHashMap<String, Long> = LinkedHashMap()
    fun consume(event: JsonObject, eventType: String) {
        val rumContext = contextProvider.getRumContext()
        if (rumContext != null) {
            val mappedEvent = map(event, eventType, rumContext)
            if (mappedEvent != null) {
                dataWriter.write(mappedEvent)
            }
        } else {
            dataWriter.write(event)
        }
    }

    private fun map(
        event: JsonObject,
        eventType: String,
        rumContext: RumContext
    ): JsonObject? {
        return try {
            when (eventType) {
                VIEW_EVENT_TYPE -> {
                    val parsedViewEvent = ViewEvent.fromJson(event.toString())
                    webRumEventMapper.mapViewEvent(
                        parsedViewEvent,
                        rumContext,
                        getOffset(parsedViewEvent.view.id)
                    ).toJson().asJsonObject
                }
                ACTION_EVENT_TYPE -> {
                    val parsedActionEvent = ActionEvent.fromJson(event.toString())
                    webRumEventMapper.mapActionEvent(
                        parsedActionEvent,
                        rumContext,
                        getOffset(parsedActionEvent.view.id)
                    ).toJson().asJsonObject
                }
                RESOURCE_EVENT_TYPE -> {
                    val parsedResourceEvent = ResourceEvent.fromJson(event.toString())
                    webRumEventMapper.mapResourceEvent(
                        parsedResourceEvent,
                        rumContext,
                        getOffset(parsedResourceEvent.view.id)
                    ).toJson().asJsonObject
                }
                ERROR_EVENT_TYPE -> {
                    val parsedErrorEvent = ErrorEvent.fromJson(event.toString())
                    webRumEventMapper.mapErrorEvent(
                        parsedErrorEvent,
                        rumContext,
                        getOffset(parsedErrorEvent.view.id)
                    ).toJson().asJsonObject
                }
                LONG_TASK_EVENT_TYPE -> {
                    val parsedLongTaskEvent = LongTaskEvent.fromJson(event.toString())
                    webRumEventMapper.mapLongTaskEvent(
                        parsedLongTaskEvent,
                        rumContext,
                        getOffset(parsedLongTaskEvent.view.id)
                    ).toJson().asJsonObject
                }
                else -> {
                    sdkLogger.e(WRONG_EVENT_TYPE_ERROR_MESSAGE.format(US, eventType))
                    null
                }
            }
        } catch (e: JsonParseException) {
            sdkLogger.e(JSON_PARSING_ERROR_MESSAGE, e)
            null
        }
    }

    private fun getOffset(viewId: String): Long {
        var offset = offsets[viewId]
        if (offset == null) {
            offset = timeProvider.getServerOffsetMillis()
            offsets[viewId] = offset
        }
        purgeOffsets()
        return offset
    }

    private fun purgeOffsets() {
        while (offsets.entries.size > MAX_VIEW_TIME_OFFSETS_RETAIN) {
            val viewId = offsets.entries.first()
            offsets.remove(viewId.key)
        }
    }

    companion object {
        const val MAX_VIEW_TIME_OFFSETS_RETAIN = 3
        const val VIEW_EVENT_TYPE = "view"
        const val ACTION_EVENT_TYPE = "action"
        const val RESOURCE_EVENT_TYPE = "resource"
        const val ERROR_EVENT_TYPE = "error"
        const val LONG_TASK_EVENT_TYPE = "long_task"
        const val JSON_PARSING_ERROR_MESSAGE = "The bundled web RUM event could not be deserialized"
        const val WRONG_EVENT_TYPE_ERROR_MESSAGE = "The event type %s for the bundled" +
            " web RUM event is unknown."
    }
}