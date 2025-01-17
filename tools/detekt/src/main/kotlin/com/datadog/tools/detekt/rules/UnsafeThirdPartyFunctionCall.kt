/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.tools.detekt.rules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.config
import io.gitlab.arturbosch.detekt.api.internal.RequiresTypeResolution
import java.util.Stack
import org.jetbrains.kotlin.descriptors.buildPossiblyInnerType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassQualifier
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.Receiver
import org.jetbrains.kotlin.types.FlexibleType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.lowerIfFlexible
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.supertypes

/**
 * This rule will report any call to a "third party" function that is considered unsafe, that is,
 * which could throw an exception.
 *
 * Third party functions are detected based on an internal package prefix: any method which has a
 * package name with this prefix is considered first party, anything else is third party.
 */
@RequiresTypeResolution
class UnsafeThirdPartyFunctionCall(
    config: Config
) : Rule(config) {

    private val internalPackagePrefix: String by config(defaultValue = "")
    private val treatUnknownFunctionAsThrowing: Boolean by config(defaultValue = true)
    private val treatUnknownConstructorAsThrowing: Boolean by config(defaultValue = false)
    private val knownThrowingCalls: List<String> by config(defaultValue = emptyList())
    private val knownSafeCalls: List<String> by config(defaultValue = emptyList())

    private val knownThrowingCallsMap: Map<String, List<String>> by lazy {
        knownThrowingCalls.map {
            val splitColon = it.split(':')
            val key = splitColon.first()
            val exceptions = splitColon[1].split(',').toList()
            key to exceptions
        }.toMap()
    }
    private val imports = mutableMapOf<String, String>()

    private val caughtExceptions = Stack<List<String>>()

    // region Rule

    override val issue: Issue = Issue(
        javaClass.simpleName,
        Severity.Defect,
        "This rule reports when a call to an unsafe third party method is made " +
            "(i.e. a function throwing an uncaught exception).",
        Debt.TWENTY_MINS
    )

    override fun visitKtFile(file: KtFile) {
        if (bindingContext == BindingContext.EMPTY) {
            println("Missing BindingContext when checking file:${file.virtualFilePath}")
        }
        imports.clear()
        super.visitKtFile(file)
    }

    override fun visitImportDirective(importDirective: KtImportDirective) {
        super.visitImportDirective(importDirective)
        val path = importDirective.importPath?.pathStr ?: return
        val alias = importDirective.alias?.toString() ?: path.substringAfterLast('.')

        imports[alias] = path
    }

    override fun visitTryExpression(expression: KtTryExpression) {
        val caughtTypes = expression.catchClauses
            .mapNotNull {
                val typeReference = it.catchParameter?.typeReference
                bindingContext.get(BindingContext.TYPE, typeReference)?.fullType()
            }
        caughtExceptions.push(caughtTypes)
        super.visitTryExpression(expression)
        caughtExceptions.pop()
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (bindingContext == BindingContext.EMPTY) {
            return
        }
        val resolvedCall = expression.getResolvedCall(bindingContext) ?: return
        val call = resolvedCall.call
        val returnType = expression.getType(bindingContext) ?: return

        val receiverType = call.explicitReceiver?.type() ?: call.dispatchReceiver?.type()

        if (receiverType == null) {
            val arguments = resolvedCall.valueArguments
                .map { it.key.type.lowerIfFlexible().fullType() }
            val fullType = imports[returnType.toString()] ?: returnType.toString()
            checkConstructorCall(expression, "$fullType.constructor(${arguments.joinToString()})")
        } else {
            val receiverFullType = receiverType.fullType()
            val arguments = resolvedCall.valueArguments
                .map { it.key.type.lowerIfFlexible().fullType() }
            val calleeExpression = call.calleeExpression?.node?.text
            checkFunctionCall(
                expression,
                "$receiverFullType.$calleeExpression(${arguments.joinToString()})",
                calleeExpression
            )
        }
    }

    // endregion

    // region Internal

    private fun checkConstructorCall(expression: KtCallExpression, call: String) {
        if (internalPackagePrefix.isNotEmpty() && call.startsWith(internalPackagePrefix)) {
            return
        }

        if (knownThrowingCallsMap.containsKey(call)) {
            val knownThrowables = knownThrowingCallsMap[call] ?: emptyList()
            checkCallThrowingExceptions(expression, call, knownThrowables)
        } else if (treatUnknownConstructorAsThrowing) {
            val message = "Calling $call could throw exceptions, but this constructor is unknown"
            reportUnsafeCall(expression, message)
        }
    }

    private fun checkFunctionCall(
        expression: KtCallExpression,
        call: String,
        functionName: String?
    ) {
        if (internalPackagePrefix.isNotEmpty() && call.startsWith(internalPackagePrefix)) {
            return
        }

        if (functionName in kotlinHelperMethods) {
            return
        }

        if (knownThrowingCallsMap.containsKey(call)) {
            val knownThrowables = knownThrowingCallsMap[call] ?: emptyList()
            checkCallThrowingExceptions(expression, call, knownThrowables)
        } else if (treatUnknownFunctionAsThrowing && !knownSafeCalls.contains(call)) {
            val message = "Calling $call could throw exceptions, but this method is unknown"
            reportUnsafeCall(expression, message)
        }
    }

    private fun checkCallThrowingExceptions(
        expression: KtCallExpression,
        call: String,
        exceptions: List<String>
    ) {
        val catchesAnyException = caughtExceptions.any { list ->
            list.any { e -> e in topLevelExceptions }
        }
        val catchesAnyError = caughtExceptions.any { list ->
            list.any { e -> e in topLevelErrors }
        }
        val uncaught = exceptions.filter { exception ->
            caughtExceptions.none { it.contains(exception) }
        }
            .filter {
                val isUncaughtException = it.endsWith("Exception") && !catchesAnyException
                val isUncaughtError = it.endsWith("Error") && !catchesAnyError
                isUncaughtException || isUncaughtError
            }

        if (uncaught.isEmpty()) {
            return
        }

        val msg = "Calling $call can throw the following exceptions: ${exceptions.joinToString()}."
        reportUnsafeCall(expression, msg)
    }

    private fun reportUnsafeCall(
        expression: KtCallExpression,
        message: String
    ) {
        report(CodeSmell(issue, Entity.from(expression), message = message))
    }

    private fun Receiver?.type(): String? {
        if (this == null) return null
        if (this is ExpressionReceiver) {
            val resolvedType = expression.getType(bindingContext)
            if (resolvedType is FlexibleType) {
                // types from java are flexible because the Kotlin Compiler doesn't know whether
                // they're nullable or not.Using the lowerBound makes it nonNullable
                return resolvedType.lowerIfFlexible().fullType()
            } else {
                // Convert all types to nonNullable
                return resolvedType?.makeNotNullable()?.fullType()
            }
        } else if (this is ClassQualifier) {
            return descriptor.fqNameOrNull()?.toString()
        } else {
            println("DD: Unknown receiver type ${this.javaClass}")
            return null
        }
    }

    private fun KotlinType.fullType(): String {
        val descriptor = if (toString() == "T" || toString() == "C") {
            // Treat generic types as their closest supertype (usually Any)
            supertypes().firstOrNull()?.buildPossiblyInnerType()?.classifierDescriptor
        } else {
            buildPossiblyInnerType()?.classifierDescriptor
        }
        val fqName = descriptor?.fqNameOrNull()
        return if (fqName != null) {
            val nullableSuffix = if (isNullable()) "?" else ""
            "$fqName$nullableSuffix"
        } else {
            val supertypes = this.supertypes().joinToString()
            println("Unable to get fqName for ${this.javaClass} $this: $supertypes")
            toString().fullType()
        }
    }

    private fun String.fullType(): String {
        if (endsWith('?')) {
            val fullType = imports[substringBeforeLast('?')]
            return if (fullType == null) this else "$fullType?"
        } else {
            return imports[this] ?: this
        }
    }

    // endregion

    companion object {
        private const val JAVA_EXCEPTION_CLASS = "java.lang.Exception"
        private const val JAVA_ERROR_CLASS = "java.lang.Error"
        private const val JAVA_THROWABLE_CLASS = "java.lang.Throwable"
        private const val KOTLIN_EXCEPTION_CLASS = "kotlin.Exception"
        private const val KOTLIN_ERROR_CLASS = "kotlin.Error"
        private const val KOTLIN_THROWABLE_CLASS = "kotlin.Throwable"

        private val topLevelExceptions = arrayOf(
            JAVA_EXCEPTION_CLASS,
            JAVA_THROWABLE_CLASS,
            KOTLIN_EXCEPTION_CLASS,
            KOTLIN_THROWABLE_CLASS
        )

        private val topLevelErrors = arrayOf(
            JAVA_ERROR_CLASS,
            JAVA_THROWABLE_CLASS,
            KOTLIN_ERROR_CLASS,
            KOTLIN_THROWABLE_CLASS
        )

        private val kotlinHelperMethods = arrayOf(
            "let", "run", "with", "apply", "also",
            "print", "println", "toString"
        )
    }
}
