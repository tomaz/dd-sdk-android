/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.nightly.rum

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.datadog.android.nightly.rules.NightlyTestRule
import com.datadog.android.nightly.utils.aResourceErrorMessage
import com.datadog.android.nightly.utils.aResourceKey
import com.datadog.android.nightly.utils.aResourceMethod
import com.datadog.android.nightly.utils.aViewKey
import com.datadog.android.nightly.utils.aViewName
import com.datadog.android.nightly.utils.anActionName
import com.datadog.android.nightly.utils.anErrorMessage
import com.datadog.android.nightly.utils.defaultTestAttributes
import com.datadog.android.nightly.utils.executeInsideView
import com.datadog.android.nightly.utils.exhaustiveAttributes
import com.datadog.android.nightly.utils.initializeSdk
import com.datadog.android.nightly.utils.measure
import com.datadog.android.nightly.utils.sendRandomActionOutcomeEvent
import com.datadog.android.rum.GlobalRum
import com.datadog.android.rum.RumActionType
import com.datadog.android.rum.RumErrorSource
import com.datadog.android.rum.RumResourceKind
import com.datadog.tools.unit.forge.aThrowable
import fr.xgouchet.elmyr.junit4.ForgeRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class RumMonitorE2ETests {
    @get:Rule
    val forge = ForgeRule()

    @get:Rule
    val nightlyTestRule = NightlyTestRule()

    /**
     * apiMethodSignature: com.datadog.android.Datadog#fun initialize(android.content.Context, com.datadog.android.core.configuration.Credentials, com.datadog.android.core.configuration.Configuration, com.datadog.android.privacy.TrackingConsent)
     * apiMethodSignature: com.datadog.android.core.configuration.Configuration$Builder#fun build(): Configuration
     * apiMethodSignature: com.datadog.android.core.configuration.Configuration$Builder#constructor(Boolean, Boolean, Boolean, Boolean)
     * apiMethodSignature: com.datadog.android.rum.GlobalRum#fun get(): RumMonitor
     * apiMethodSignature: com.datadog.android.rum.GlobalRum#fun isRegistered(): Boolean
     * apiMethodSignature: com.datadog.android.rum.GlobalRum#fun registerIfAbsent(RumMonitor): Boolean
     */
    @Before
    fun setUp() {
        initializeSdk(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    // region View

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun startView(Any, String, Map<String, Any?> = emptyMap())
     * apiMethodSignature: com.datadog.android.rum.GlobalRum#fun get(): RumMonitor
     */
    @Test
    fun rum_rummonitor_start_view() {
        val testMethodName = "rum_rummonitor_start_view"
        val viewKey = forge.aViewKey()
        val viewName = forge.aViewName()
        measure(testMethodName) {
            GlobalRum.get().startView(
                viewKey,
                viewName,
                defaultTestAttributes(testMethodName)
            )
        }
        GlobalRum.get().stopView(viewKey)
    }

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun stopView(Any, Map<String, Any?> = emptyMap())
     */
    @Test
    fun rum_rummonitor_stop_view() {
        val testMethodName = "rum_rummonitor_stop_view"
        val viewKey = forge.aViewKey()
        val viewName = forge.aViewName()
        GlobalRum.get().startView(
            viewKey,
            viewName,
            defaultTestAttributes(testMethodName)
        )
        measure(testMethodName) {
            GlobalRum.get().stopView(viewKey)
        }
    }

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun stopView(Any, Map<String, Any?> = emptyMap())
     */
    @Test
    fun rum_rummonitor_stop_view_with_pending_resource() {
        val testMethodName = "rum_rummonitor_stop_view_with_pending_resource"
        val viewKey = forge.aViewKey()
        val viewName = forge.aViewName()
        val resourceKey = forge.aResourceKey()
        GlobalRum.get().startView(
            viewKey,
            viewName,
            defaultTestAttributes(testMethodName)
        )
        GlobalRum.get().startResource(
            resourceKey,
            forge.aResourceMethod(),
            resourceKey,
            attributes = defaultTestAttributes(testMethodName)
        )
        measure(testMethodName) {
            GlobalRum.get().stopView(viewKey)
        }
        GlobalRum.get().stopResource(
            resourceKey,
            forge.aNullable { forge.anInt(min = 200, max = 500) },
            forge.aNullable { forge.aLong(min = 1) },
            forge.aValueFrom(RumResourceKind::class.java),
            defaultTestAttributes(testMethodName)
        )
    }

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun stopView(Any, Map<String, Any?> = emptyMap())
     */
    @Test
    fun rum_rummonitor_stop_view_with_pending_action() {
        val testMethodName = "rum_rummonitor_stop_view_with_pending_action"
        val viewKey = forge.aViewKey()
        val viewName = forge.aViewName()
        val actionName = forge.anAlphabeticalString()
        val actionType = forge.aValueFrom(RumActionType::class.java)
        GlobalRum.get().startView(
            viewKey,
            viewName,
            defaultTestAttributes(testMethodName)
        )
        GlobalRum.get().startUserAction(
            actionType,
            actionName,
            attributes = defaultTestAttributes(testMethodName)
        )
        measure(testMethodName) {
            GlobalRum.get().stopView(viewKey)
        }
        GlobalRum.get().stopUserAction(
            actionType,
            actionName,
            defaultTestAttributes(testMethodName)
        )
    }

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun addTiming(String)
     */
    @Test
    fun rum_rummonitor_add_timing() {
        val testMethodName = "rum_rummonitor_add_timing"
        val viewKey = forge.aViewKey()
        val viewName = forge.aViewName()
        val timing = forge.aLong(200, 700)
        executeInsideView(viewKey, viewName, testMethodName) {
            Thread.sleep(timing)
            measure(testMethodName) {
                GlobalRum.get().addTiming(RUM_TIMING_NAME)
            }
        }
    }

    // endregion

    // region Action

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun startUserAction(RumActionType, String, Map<String, Any?>)
     */
    @Test
    fun rum_rummonitor_start_non_custom_action_with_no_outcome() {
        val testMethodName = "rum_rummonitor_start_non_custom_action_with_no_outcome"
        val viewKey = forge.aViewKey()
        val viewName = forge.aViewName()
        val actionName = forge.anAlphabeticalString()
        executeInsideView(viewKey, viewName, testMethodName) {
            measure(testMethodName) {
                GlobalRum.get().startUserAction(
                    forge.aValueFrom(
                        RumActionType::class.java,
                        exclude = listOf(RumActionType.CUSTOM)
                    ),
                    actionName,
                    attributes = defaultTestAttributes(testMethodName)
                )
            }
            // wait for the action to be inactive
            Thread.sleep(ACTION_INACTIVITY_THRESHOLD_MS)
        }
    }

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun startUserAction(RumActionType, String, Map<String, Any?>)
     */
    @Test
    fun rum_rummonitor_start_custom_action_with_no_outcome() {
        val testMethodName = "rum_rummonitor_start_custom_action_with_no_outcome"
        val viewKey = forge.aViewKey()
        val viewName = forge.aViewName()
        val actionName = forge.anActionName()
        executeInsideView(viewKey, viewName, testMethodName) {
            measure(testMethodName) {
                GlobalRum.get().startUserAction(
                    RumActionType.CUSTOM,
                    actionName,
                    attributes = defaultTestAttributes(testMethodName)
                )
            }
        }
    }

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun startUserAction(RumActionType, String, Map<String, Any?>)
     */
    @Test
    fun rum_rummonitor_start_action_with_outcome() {
        val testMethodName = "rum_rummonitor_start_action_with_outcome"
        val viewKey = forge.aViewKey()
        val viewName = forge.aViewName()
        val actionName = forge.anActionName()
        executeInsideView(viewKey, viewName, testMethodName) {
            measure(testMethodName) {
                GlobalRum.get().startUserAction(
                    forge.aValueFrom(RumActionType::class.java),
                    actionName,
                    attributes = defaultTestAttributes(testMethodName)
                )
            }
            sendRandomActionOutcomeEvent(forge)
            // wait for the action to be inactive
            Thread.sleep(ACTION_INACTIVITY_THRESHOLD_MS)
        }
    }

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun stopUserAction(RumActionType, String, Map<String, Any?> = emptyMap())
     */
    @Test
    fun rum_rummonitor_stop_non_custom_action_with_no_outcome() {
        val testMethodName = "rum_rummonitor_stop_non_custom_action_with_no_outcome"
        val viewKey = forge.aViewKey()
        val viewName = forge.aViewName()
        val actionName = forge.anActionName()
        val type = forge.aValueFrom(
            RumActionType::class.java,
            exclude = listOf(RumActionType.CUSTOM)
        )
        executeInsideView(viewKey, viewName, testMethodName) {
            GlobalRum.get().startUserAction(
                type,
                actionName,
                attributes = defaultTestAttributes(testMethodName)
            )
            measure(testMethodName) {
                GlobalRum.get().stopUserAction(type, actionName, forge.exhaustiveAttributes())
            }
            // wait for the action to be inactive
            Thread.sleep(ACTION_INACTIVITY_THRESHOLD_MS)
        }
    }

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun stopUserAction(RumActionType, String, Map<String, Any?> = emptyMap())
     */
    @Test
    fun rum_rummonitor_stop_custom_action_with_no_outcome() {
        val testMethodName = "rum_rummonitor_stop_custom_action_with_no_outcome"
        val viewKey = forge.aViewKey()
        val viewName = forge.aViewName()
        val actionName = forge.anActionName()
        executeInsideView(viewKey, viewName, testMethodName) {
            GlobalRum.get().startUserAction(
                RumActionType.CUSTOM,
                actionName,
                attributes = defaultTestAttributes(testMethodName)
            )
            measure(testMethodName) {
                GlobalRum.get().stopUserAction(
                    RumActionType.CUSTOM,
                    actionName,
                    forge.exhaustiveAttributes()
                )
            }
            // wait for the action to be inactive
            Thread.sleep(ACTION_INACTIVITY_THRESHOLD_MS)
        }
    }

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun stopUserAction(RumActionType, String, Map<String, Any?> = emptyMap())
     */
    @Test
    fun rum_rummonitor_stop_action_with_outcome() {
        val testMethodName = "rum_rummonitor_stop_action_with_outcome"
        val viewKey = forge.aViewKey()
        val viewName = forge.aViewName()
        val actionName = forge.anActionName()
        val type = forge.aValueFrom(RumActionType::class.java)
        executeInsideView(viewKey, viewName, testMethodName) {
            GlobalRum.get().startUserAction(
                type,
                actionName,
                attributes = defaultTestAttributes(testMethodName)
            )
            sendRandomActionOutcomeEvent(forge)
            measure(testMethodName) {
                GlobalRum.get().stopUserAction(type, actionName, forge.exhaustiveAttributes())
            }
            // wait for the action to be inactive
            Thread.sleep(ACTION_INACTIVITY_THRESHOLD_MS)
        }
    }

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun addUserAction(RumActionType, String, Map<String, Any?>)
     */
    @Test
    fun rum_rummonitor_add_non_custom_action_with_no_outcome() {
        val testMethodName = "rum_rummonitor_add_non_custom_action_with_no_outcome"
        val viewKey = forge.aViewKey()
        val viewName = forge.aViewName()
        val actionName = forge.anActionName()
        executeInsideView(viewKey, viewName, testMethodName) {
            measure(testMethodName) {
                GlobalRum.get().addUserAction(
                    forge.aValueFrom(
                        RumActionType::class.java,
                        exclude = listOf(RumActionType.CUSTOM)
                    ),
                    actionName,
                    attributes = defaultTestAttributes(testMethodName)
                )
            }
            // wait for the action to be inactive
            Thread.sleep(ACTION_INACTIVITY_THRESHOLD_MS)
        }
    }

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun addUserAction(RumActionType, String, Map<String, Any?>)
     */
    @Test
    fun rum_rummonitor_add_custom_action_with_no_outcome() {
        val testMethodName = "rum_rummonitor_add_custom_action_with_no_outcome"
        val viewKey = forge.aViewKey()
        val viewName = forge.aViewName()
        val actionName = forge.anActionName()
        executeInsideView(viewKey, viewName, testMethodName) {
            measure(testMethodName) {
                GlobalRum.get().addUserAction(
                    RumActionType.CUSTOM,
                    actionName,
                    attributes = defaultTestAttributes(testMethodName)
                )
            }
            // wait for the action to be inactive
            Thread.sleep(ACTION_INACTIVITY_THRESHOLD_MS)
        }
    }

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun addUserAction(RumActionType, String, Map<String, Any?>)
     */
    @Test
    fun rum_rummonitor_add_action_with_outcome() {
        val testMethodName = "rum_rummonitor_add_action_with_outcome"
        val viewKey = forge.aViewKey()
        val viewName = forge.aViewName()
        val actionName = forge.anActionName()
        executeInsideView(viewKey, viewName, testMethodName) {
            measure(testMethodName) {
                GlobalRum.get().addUserAction(
                    forge.aValueFrom(RumActionType::class.java),
                    actionName,
                    attributes = defaultTestAttributes(testMethodName)
                )
            }
            sendRandomActionOutcomeEvent(forge)
            // wait for the action to be inactive
            Thread.sleep(ACTION_INACTIVITY_THRESHOLD_MS)
        }
    }

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun addUserAction(RumActionType, String, Map<String, Any?>)
     */
    @Test
    fun rum_rummonitor_add_custom_action_while_active_action() {
        val testMethodName = "rum_rummonitor_add_custom_action_while_active_action"
        val viewKey = forge.aViewKey()
        val viewName = forge.aViewName()
        val activeActionName = forge.anActionName(prefix = "rumActiveAction")
        val customActionName = forge.anActionName()
        val type = forge.aValueFrom(RumActionType::class.java)
        executeInsideView(viewKey, viewName, testMethodName) {
            GlobalRum.get().startUserAction(
                type,
                activeActionName,
                attributes = defaultTestAttributes(testMethodName)
            )
            measure(testMethodName) {
                GlobalRum.get().addUserAction(
                    RumActionType.CUSTOM,
                    customActionName,
                    attributes = defaultTestAttributes(testMethodName)
                )
            }
            sendRandomActionOutcomeEvent(forge)
            // wait for the action to be inactive
            Thread.sleep(ACTION_INACTIVITY_THRESHOLD_MS)
            GlobalRum.get().stopUserAction(type, activeActionName, forge.exhaustiveAttributes())
        }
    }

    // endregion

    // region Background Action

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun stopUserAction(RumActionType, String, Map<String, Any?> = emptyMap())
     */
    @Test
    fun rum_rummonitor_ignore_stop_background_action_with_outcome() {
        val testMethodName = "rum_rummonitor_ignore_stop_background_action_with_outcome"
        val actionName = forge.anActionName()
        val type = forge.aValueFrom(RumActionType::class.java)
        GlobalRum.get().startUserAction(
            type,
            actionName,
            attributes = defaultTestAttributes(testMethodName)
        )
        // In this case the `sendRandomActionEvent`
        // will mark the event valid to be sent, then we wait to make the event inactive and then
        // we stop it. In this moment everything is set for the event to be sent but it still needs
        // another upcoming event (start/stop view, resource, action, error) to trigger
        // the `sendAction`
        sendRandomActionOutcomeEvent(forge)
        // wait for the action to be inactive
        Thread.sleep(ACTION_INACTIVITY_THRESHOLD_MS)
        measure(testMethodName) {
            GlobalRum.get().stopUserAction(type, actionName, forge.exhaustiveAttributes())
        }
    }

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun addUserAction(RumActionType, String, Map<String, Any?>)
     */
    @Test
    fun rum_rummonitor_ignore_add_background_non_custom_action_with_no_outcome() {
        val testMethodName =
            "rum_rummonitor_ignore_add_background_non_custom_action_with_no_outcome"
        val actionName = forge.anActionName()
        measure(testMethodName) {
            GlobalRum.get().addUserAction(
                forge.aValueFrom(
                    RumActionType::class.java,
                    exclude = listOf(RumActionType.CUSTOM)
                ),
                actionName,
                attributes = defaultTestAttributes(testMethodName)
            )
        }
    }

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun addUserAction(RumActionType, String, Map<String, Any?>)
     */
    @Test
    fun rum_rummonitor_ignore_add_background_custom_action_with_no_outcome() {
        val testMethodName = "rum_rummonitor_ignore_add_background_custom_action_with_no_outcome"
        val actionName = forge.anActionName()
        measure(testMethodName) {
            GlobalRum.get().addUserAction(
                RumActionType.CUSTOM,
                actionName,
                attributes = defaultTestAttributes(testMethodName)
            )
        }
        // wait for the action to be inactive
        Thread.sleep(ACTION_INACTIVITY_THRESHOLD_MS)
    }

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun addUserAction(RumActionType, String, Map<String, Any?>)
     */
    @Test
    fun rum_rummonitor_ignore_add_background_custom_action_with_outcome() {
        val testMethodName = "rum_rummonitor_ignore_add_background_custom_action_with_outcome"
        val actionName = forge.anActionName()
        measure(testMethodName) {
            GlobalRum.get().addUserAction(
                RumActionType.CUSTOM,
                actionName,
                attributes = defaultTestAttributes(testMethodName)
            )
        }
        // wait for the action to be inactive
        Thread.sleep(ACTION_INACTIVITY_THRESHOLD_MS)
        // send a random action outcome event which will trigger the `sendAction` function.
        // as this is a custom action it will skip the `sideEffects` verification and it will be
        // sent immediately.
        sendRandomActionOutcomeEvent(forge)
    }

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun addUserAction(RumActionType, String, Map<String, Any?>)
     */
    @Test
    fun rum_rummonitor_ignore_add_background_non_custom_action_with_outcome() {
        val testMethodName = "rum_rummonitor_ignore_add_background_non_custom_action_with_outcome"
        val actionName = forge.anActionName()
        measure(testMethodName) {
            GlobalRum.get().addUserAction(
                forge.aValueFrom(
                    RumActionType::class.java,
                    exclude = listOf(RumActionType.CUSTOM)
                ),
                actionName,
                attributes = defaultTestAttributes(testMethodName)
            )
        }
        // wait for the action to be inactive
        Thread.sleep(ACTION_INACTIVITY_THRESHOLD_MS)
        // send a random action outcome event which will increment the resource/error count making
        // this action event valid for being sent. Although the action event is valid it will not
        // be sent in this case because there is no other event to after to trigger the `sendAction`
        // function.
        sendRandomActionOutcomeEvent(forge)
    }

    // endregion

    // region Resource

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun startResource(String, String, String, Map<String, Any?> = emptyMap())
     */
    @Test
    fun rum_rummonitor_start_resource() {
        val testMethodName = "rum_rummonitor_start_resource"
        val viewKey = forge.aViewKey()
        val viewName = forge.aViewName()
        val resourceKey = forge.aResourceKey()
        executeInsideView(viewKey, viewName, testMethodName) {
            measure(testMethodName) {
                GlobalRum.get().startResource(
                    resourceKey,
                    forge.aResourceMethod(),
                    resourceKey,
                    attributes = defaultTestAttributes(testMethodName)
                )
            }
        }
    }

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun stopResource(String, Int?, Long?, RumResourceKind, Map<String, Any?>)
     */
    @Test
    fun rum_rummonitor_stop_resource() {
        val testMethodName = "rum_rummonitor_stop_resource"
        val viewKey = forge.aViewKey()
        val viewName = forge.aViewName()
        val resourceKey = forge.aResourceKey()
        executeInsideView(viewKey, viewName, testMethodName) {
            GlobalRum.get().startResource(
                resourceKey,
                forge.aResourceMethod(),
                resourceKey,
                attributes = defaultTestAttributes(testMethodName)
            )
            measure(testMethodName) {
                GlobalRum.get().stopResource(
                    resourceKey,
                    200,
                    forge.aLong(min = 1),
                    forge.aValueFrom(RumResourceKind::class.java),
                    defaultTestAttributes(testMethodName)
                )
            }
        }
    }

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun stopResourceWithError(String, Int?, String, RumErrorSource, Throwable, Map<String, Any?> = emptyMap())
     */
    @Test
    fun rum_rummonitor_stop_resource_with_error() {
        val testMethodName = "rum_rummonitor_stop_resource_with_error"
        val viewKey = forge.aViewKey()
        val viewName = forge.aViewName()
        val resourceKey = forge.aResourceKey()
        executeInsideView(viewKey, viewName, testMethodName) {
            GlobalRum.get().startResource(
                resourceKey,
                forge.aResourceMethod(),
                resourceKey,
                attributes = defaultTestAttributes(testMethodName)
            )
            measure(testMethodName) {
                GlobalRum.get().stopResourceWithError(
                    resourceKey,
                    forge.anInt(min = 400, max = 511),
                    forge.aResourceErrorMessage(),
                    forge.aValueFrom(RumErrorSource::class.java),
                    forge.aThrowable(),
                    defaultTestAttributes(testMethodName)
                )
            }
        }
    }

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun stopResourceWithError(String, Int?, String, RumErrorSource, Throwable, Map<String, Any?> = emptyMap())
     */
    @Test
    fun rum_rummonitor_stop_resource_with_error_without_status_code() {
        val testMethodName = "rum_rummonitor_stop_resource_with_error_without_status_code"
        val viewKey = forge.aViewKey()
        val viewName = forge.aViewName()
        val resourceKey = forge.aResourceKey()
        executeInsideView(viewKey, viewName, testMethodName) {
            GlobalRum.get().startResource(
                resourceKey,
                forge.aResourceMethod(),
                resourceKey,
                attributes = defaultTestAttributes(testMethodName)
            )
            measure(testMethodName) {
                GlobalRum.get().stopResourceWithError(
                    resourceKey,
                    null,
                    forge.aResourceErrorMessage(),
                    forge.aValueFrom(RumErrorSource::class.java),
                    forge.aThrowable(),
                    defaultTestAttributes(testMethodName)
                )
            }
        }
    }

    // endregion

    // region Background Resource

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun stopResource(String, Int?, Long?, RumResourceKind, Map<String, Any?>)
     */
    @Test
    fun rum_rummonitor_ignore_stop_background_resource() {
        val testMethodName = "rum_rummonitor_ignore_stop_background_resource"
        val resourceKey = forge.aResourceKey()
        GlobalRum.get().startResource(
            resourceKey,
            forge.aResourceMethod(),
            resourceKey,
            attributes = defaultTestAttributes(testMethodName)
        )
        measure(testMethodName) {
            GlobalRum.get().stopResource(
                resourceKey,
                200,
                forge.aLong(min = 1),
                forge.aValueFrom(RumResourceKind::class.java),
                defaultTestAttributes(testMethodName)
            )
        }
    }

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun stopResourceWithError(String, Int?, String, RumErrorSource, Throwable, Map<String, Any?> = emptyMap())
     */
    @Test
    fun rum_rummonitor_ignore_stop_background_resource_with_error() {
        val testMethodName = "rum_rummonitor_ignore_stop_background_resource_with_error"
        val resourceKey = forge.aResourceKey()
        GlobalRum.get().startResource(
            resourceKey,
            forge.aResourceMethod(),
            resourceKey,
            attributes = defaultTestAttributes(testMethodName)
        )
        measure(testMethodName) {
            GlobalRum.get().stopResourceWithError(
                resourceKey,
                forge.anInt(min = 400, max = 511),
                forge.aResourceErrorMessage(),
                forge.aValueFrom(RumErrorSource::class.java),
                forge.aThrowable(),
                defaultTestAttributes(testMethodName)
            )
        }
    }

    // endregion

    // region Error

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun addError(String, RumErrorSource, Throwable?, Map<String, Any?>)
     */
    @Test
    fun rum_rummonitor_add_error() {
        val testMethodName = "rum_rummonitor_add_error"
        val viewKey = forge.aViewKey()
        val viewName = forge.aViewName()
        val errorMessage = forge.anErrorMessage()
        executeInsideView(viewKey, viewName, testMethodName) {
            measure(testMethodName) {
                GlobalRum.get().addError(
                    errorMessage,
                    forge.aValueFrom(RumErrorSource::class.java),
                    forge.aNullable { forge.aThrowable() },
                    defaultTestAttributes(testMethodName)
                )
            }
        }
    }

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun addErrorWithStacktrace(String, RumErrorSource, String?, Map<String, Any?>)
     */
    @Test
    fun rum_rummonitor_add_error_with_stacktrace() {
        val testMethodName = "rum_rummonitor_add_error_with_stacktrace"
        val viewKey = forge.aViewKey()
        val viewName = forge.aViewName()
        val errorMessage = forge.anErrorMessage()
        executeInsideView(viewKey, viewName, testMethodName) {
            measure(testMethodName) {
                GlobalRum.get().addErrorWithStacktrace(
                    errorMessage,
                    forge.aValueFrom(RumErrorSource::class.java),
                    forge.aNullable { forge.aThrowable().stackTraceToString() },
                    defaultTestAttributes(testMethodName)
                )
            }
        }
    }

    // endregion

    // region Background Error

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun addError(String, RumErrorSource, Throwable?, Map<String, Any?>)
     */
    @Test
    fun rum_rummonitor_ignore_add_background_error() {
        val testMethodName = "rum_rummonitor_ignore_add_background_error"
        val errorMessage = forge.anErrorMessage()
        measure(testMethodName) {
            GlobalRum.get().addError(
                errorMessage,
                forge.aValueFrom(RumErrorSource::class.java),
                forge.aNullable { forge.aThrowable() },
                defaultTestAttributes(testMethodName)
            )
        }
    }

    /**
     * apiMethodSignature: com.datadog.android.rum.RumMonitor#fun addErrorWithStacktrace(String, RumErrorSource, String?, Map<String, Any?>)
     */
    @Test
    fun rum_rummonitor_ignore_add_background_error_with_stacktrace() {
        val testMethodName = "rum_rummonitor_ignore_add_background_error_with_stacktrace"
        val errorMessage = forge.anErrorMessage()
        measure(testMethodName) {
            GlobalRum.get().addErrorWithStacktrace(
                errorMessage,
                forge.aValueFrom(RumErrorSource::class.java),
                forge.aNullable { forge.aThrowable().stackTraceToString() },
                defaultTestAttributes(testMethodName)
            )
        }
    }

    // endregion
}
