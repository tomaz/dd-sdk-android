/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sample.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.datadog.android.log.Logger
import com.datadog.android.rum.GlobalRum
import com.datadog.android.rum.RumActionType
import com.datadog.android.rum.RumErrorSource
import com.datadog.android.rum.RumResourceKind
import com.datadog.android.sample.BuildConfig
import com.datadog.android.sample.R
import java.io.IOException
import java.lang.IllegalStateException
import java.security.SecureRandom

class LogsForegroundService : Service() {

    private val random = SecureRandom()

    private val logger: Logger by lazy {
        Logger.Builder()
            .setLoggerName("foreground_service")
            .setLogcatLogsEnabled(true)
            .build()
            .apply {
                addTag("flavor", BuildConfig.FLAVOR)
                addTag("build_type", BuildConfig.BUILD_TYPE)
            }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            STOP_SERVICE_ACTION -> {
                stopForegroundService()
            }
            SEND_RUM_ERROR -> {
                GlobalRum.get().addError(
                    BACKGROUND_ERROR_KEY,
                    randomErrorSource(),
                    randomThrowable(),
                    emptyMap()
                )
            }
            SEND_RUM_ACTION -> {
                GlobalRum.get().addUserAction(
                    RumActionType.CUSTOM,
                    BACKGROUND_ACTION_KEY,
                    emptyMap()
                )
            }
            START_RUM_RESOURCE -> {
                GlobalRum.get().startResource(
                    BACKGROUND_RESOURCE_URL,
                    "GET",
                    BACKGROUND_RESOURCE_URL,
                    emptyMap()
                )
            }
            STOP_RUM_RESOURCE -> {
                GlobalRum.get().stopResource(
                    BACKGROUND_RESOURCE_URL,
                    200,
                    200,
                    RumResourceKind.IMAGE,
                    emptyMap()
                )
            }
            else -> {
                startForegroundService()
            }
        }
        return START_NOT_STICKY
    }

    private fun stopForegroundService() {
        stopForeground(true)
        stopSelf()
    }

    private fun startForegroundService() {
        createNotificationChannel()
        val stopServicePendingIntent = createServiceIntent(STOP_SERVICE_ACTION)
        val sendRumErrorPendingIntent = createServiceIntent(SEND_RUM_ERROR)
        val sendRumActionPendingIntent = createServiceIntent(SEND_RUM_ACTION)
        val startRumResourcePendingIntent = createServiceIntent(START_RUM_RESOURCE)
        val stopRumResourcePendingIntent = createServiceIntent(STOP_RUM_RESOURCE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle())
            .setSmallIcon(R.drawable.ic_logs_black_24dp)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Service",
                stopServicePendingIntent
            )
            .addAction(
                R.drawable.ic_baseline_error_24,
                "Send RUM Error",
                sendRumErrorPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_call,
                "Send RUM Action",
                sendRumActionPendingIntent
            )
            .addAction(
                android.R.drawable.ic_media_play,
                "Start RUM Resource",
                startRumResourcePendingIntent
            )
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop RUM Resource",
                stopRumResourcePendingIntent
            )
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createServiceIntent(action: String): PendingIntent {
        val serviceIntent = Intent(this, LogsForegroundService::class.java)
        serviceIntent.action = action
        return PendingIntent.getService(
            this,
            0,
            serviceIntent,
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) 0 else PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "SampleApp Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun randomErrorSource(): RumErrorSource {
        return when (random.nextInt() % RumErrorSource.values().size) {
            0 -> RumErrorSource.NETWORK
            1 -> RumErrorSource.SOURCE
            2 -> RumErrorSource.AGENT
            3 -> RumErrorSource.CONSOLE
            4 -> RumErrorSource.LOGGER
            else -> RumErrorSource.WEBVIEW
        }
    }

    private fun randomThrowable(): Throwable {
        return when (random.nextInt() % 4) {
            0 -> IllegalArgumentException()
            1 -> IllegalStateException()
            2 -> SecurityException()
            else -> IOException()
        }
    }

    companion object {
        const val CHANNEL_ID = "LogsServiceChannel"
        const val NOTIFICATION_ID = 1
        const val STOP_SERVICE_ACTION = "STOP_SERVICE"
        const val SEND_RUM_ERROR = "SEND_RUM_ERROR"
        const val SEND_RUM_ACTION = "SEND_RUM_ACTION"
        const val START_RUM_RESOURCE = "START_RUM_RESOURCE"
        const val STOP_RUM_RESOURCE = "STOP_RUM_RESOURCE"
        const val BACKGROUND_RESOURCE_URL = "background/background-resource/1"
        const val BACKGROUND_ACTION_KEY = "BackgroundAction"
        const val BACKGROUND_ERROR_KEY = "BackgroundError"
    }
}
