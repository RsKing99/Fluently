/*
 * Copyright 2025 Karma Krafts & associates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.karmakrafts.fluently.reactive

import dev.karmakrafts.fluently.eval.EvaluationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1

/**
 * A lightweight, coroutine-aware formatting facade that provides scoped, memoized access
 * to localized strings managed by [LocalizationManager].
 *
 * Key characteristics:
 * - Scope-cached flows: Each unique combination of message name, optional attribute name, and
 *   [EvaluationContext] is cached inside this scope and exposed as a hot [SharedFlow] with a
 *   replay of 1 value. Identical requests within the same [LocalizationScope] share the same
 *   upstream flow and emissions.
 * - Lifecycle-bound sharing: Internally uses [shareIn] with [SharingStarted.WhileSubscribed],
 *   meaning upstream work is active only while there are subscribers, and the last value is
 *   replayed to late subscribers.
 * - Thread-safety: Access to the internal cache is guarded by a [Mutex].
 * - Reactivity: Emissions automatically update when the current localization file or inputs used
 *   in the evaluation context change upstream.
 *
 * Usage notes:
 * - Use the EvaluationContext builder overloads when you want to build the context from the
 *   current localization file. The builder automatically applies the manager's global context init.
 * - Use the callback overloads for convenient collection within this scope's [coroutineScope].
 *
 * @param localizationManager The underlying source of formatted localization flows.
 * @param coroutineScope The [CoroutineScope] used for sharing and callback collection. By default,
 * it is created from the manager's coroutine context.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocalizationScope( // @formatter:off
    @PublishedApi internal val localizationManager: LocalizationManager,
    @PublishedApi internal val coroutineScope: CoroutineScope = CoroutineScope(localizationManager.coroutineContext)
) { // @formatter:on

    private val cache: HashMap<ReactiveLocalizationKey, SharedFlow<String>> = HashMap()
    private val cacheMutex: Mutex = Mutex()

    /**
     * Formats a localization message as a hot, scope-shared flow using the provided reactive context.
     *
     * The resulting flow is memoized inside this LocalizationScope per unique combination of name and
     * context (including resolved file and variables). Internally it is shared with replay(1), so the
     * last value is delivered immediately to late subscribers and the upstream stays active only while
     * there are subscribers.
     *
     * @param name The message identifier in the localization bundle.
     * @param context A reactive evaluation context that drives formatting and updates.
     * @return A Flow that emits the formatted string and updates when inputs change.
     */
    suspend fun format(name: String, context: ReactiveEvaluationContext): Flow<String> {
        return ReactiveLocalizationKey.fromContext(name, context).flatMapLatest { key ->
            cacheMutex.withLock {
                cache.getOrPut(key) {
                    localizationManager.format(name, context)
                        .shareIn(coroutineScope, SharingStarted.WhileSubscribed(), 1)
                }
            }
        }
    }

    /**
     * Starts collecting the formatted message and forwards every update to the given callback.
     *
     * Collection is launched in this scope's coroutineScope and will be cancelled when the scope is
     * cancelled. The underlying formatting flow is shared and memoized within this LocalizationScope.
     *
     * @param name The message identifier.
     * @param context Reactive evaluation context to use for formatting.
     * @param callback Consumer that receives every formatted value.
     */
    fun format(name: String, context: ReactiveEvaluationContext, callback: (String) -> Unit) {
        coroutineScope.launch {
            format(name, context).collect(callback)
        }
    }

    /**
     * Binds the formatted message to a writable property reference. The property is updated on every
     * emission from the underlying shared flow.
     *
     * @param name The message identifier.
     * @param context Reactive evaluation context to use for formatting.
     * @param property A mutable property (e.g., a var) to receive formatted values.
     */
    fun format(name: String, context: ReactiveEvaluationContext, property: KMutableProperty0<String>) {
        coroutineScope.launch {
            format(name, context).collect(property::set)
        }
    }

    /**
     * Binds the formatted message to a mutable property on a receiver instance using a context receiver.
     *
     * This is useful for updating fields on an object without capturing it in a lambda.
     *
     * @param name The message identifier.
     * @param context Reactive evaluation context to use for formatting.
     * @param property A mutable property on the receiver instance to update with formatted values.
     */
    context(instance: T) fun <T> format( // @formatter:off
        name: String,
        context: ReactiveEvaluationContext,
        property: KMutableProperty1<T, String>
    ) { // @formatter:on
        coroutineScope.launch {
            format(name, context).collect { text ->
                property.set(instance, text)
            }
        }
    }

    /**
     * Formats a message by constructing a ReactiveEvaluationContext from the current localization file
     * and the provided builder.
     *
     * The manager's global context initializer is applied automatically before your builder.
     *
     * @param name The message identifier.
     * @param contextInit Builder used to extend or override functions and variables in the context.
     * @return A Flow that emits the formatted string and updates when inputs change.
     */
    suspend inline fun format( // @formatter:off
        name: String,
        crossinline contextInit: ReactiveEvaluationContextBuilder.() -> Unit = {}
    ): Flow<String> { // @formatter:on
        return format(name, reactiveEvaluationContext(localizationManager.currentLocalizations, contextInit))
    }

    /**
     * Convenience overload that builds the reactive context and collects the formatted value into a callback.
     *
     * @param name The message identifier.
     * @param contextInit Builder to initialize the evaluation context from the current localization file.
     * @param callback Consumer that receives every formatted value.
     */
    inline fun format(
        name: String,
        crossinline contextInit: ReactiveEvaluationContextBuilder.() -> Unit = {},
        noinline callback: (String) -> Unit
    ) {
        coroutineScope.launch {
            format(name, contextInit).collect(callback)
        }
    }

    /**
     * Convenience overload that builds the reactive context and binds results to a writable property.
     *
     * @param name The message identifier.
     * @param contextInit Builder to initialize the evaluation context from the current localization file.
     * @param property A mutable property (e.g., a var) to receive formatted values.
     */
    inline fun format(
        name: String,
        crossinline contextInit: ReactiveEvaluationContextBuilder.() -> Unit = {},
        property: KMutableProperty0<String>
    ) {
        coroutineScope.launch {
            format(name, contextInit).collect(property::set)
        }
    }

    /**
     * Convenience overload that builds the reactive context and writes results to a mutable property on
     * a receiver instance using a context receiver.
     *
     * @param name The message identifier.
     * @param contextInit Builder to initialize the evaluation context from the current localization file.
     * @param property A mutable property on the receiver instance to update with formatted values.
     */
    inline context(instance: T) fun <T> format(
        name: String,
        crossinline contextInit: ReactiveEvaluationContextBuilder.() -> Unit = {},
        property: KMutableProperty1<T, String>
    ) {
        coroutineScope.launch {
            format(name, contextInit).collect { text ->
                property.set(instance, text)
            }
        }
    }

    /**
     * Formats a specific attribute of a localization message as a hot, scope-shared flow.
     *
     * @param name The message identifier.
     * @param attribName The attribute name within the message to format.
     * @param context Reactive evaluation context that drives formatting and updates.
     * @return A Flow that emits the formatted attribute and updates when inputs change.
     */
    suspend fun format(name: String, attribName: String, context: ReactiveEvaluationContext): Flow<String> {
        return ReactiveLocalizationKey.fromContext(name, attribName, context).flatMapLatest { key ->
            cacheMutex.withLock {
                cache.getOrPut(key) {
                    localizationManager.format(name, attribName, context)
                        .shareIn(coroutineScope, SharingStarted.WhileSubscribed(), 1)
                }
            }
        }
    }

    /**
     * Collects a formatted attribute and forwards updates to the given callback.
     *
     * @param name The message identifier.
     * @param attribName The attribute name within the message.
     * @param context Reactive evaluation context to use for formatting.
     * @param callback Consumer that receives every formatted value.
     */
    fun format(name: String, attribName: String, context: ReactiveEvaluationContext, callback: (String) -> Unit) {
        coroutineScope.launch {
            format(name, attribName, context).collect(callback)
        }
    }

    /**
     * Binds a formatted attribute to a writable property reference.
     *
     * @param name The message identifier.
     * @param attribName The attribute name within the message.
     * @param context Reactive evaluation context to use for formatting.
     * @param property A mutable property (e.g., a var) to receive formatted values.
     */
    fun format(
        name: String, attribName: String, context: ReactiveEvaluationContext, property: KMutableProperty0<String>
    ) {
        coroutineScope.launch {
            format(name, attribName, context).collect(property::set)
        }
    }

    /**
     * Binds a formatted attribute to a mutable property on a receiver instance using a context receiver.
     *
     * @param name The message identifier.
     * @param attribName The attribute name within the message.
     * @param context Reactive evaluation context to use for formatting.
     * @param property A mutable property on the receiver instance to update with formatted values.
     */
    context(instance: T) fun <T> format(
        name: String, attribName: String, context: ReactiveEvaluationContext, property: KMutableProperty1<T, String>
    ) {
        coroutineScope.launch {
            format(name, attribName, context).collect { text ->
                property.set(instance, text)
            }
        }
    }

    /**
     * Formats a specific attribute by constructing a ReactiveEvaluationContext from the current
     * localization file and the provided builder.
     *
     * @param name The message identifier.
     * @param attribName The attribute name within the message to format.
     * @param contextInit Builder used to extend or override functions and variables in the context.
     * @return A Flow that emits the formatted attribute and updates when inputs change.
     */
    suspend inline fun format( // @formatter:off
        name: String,
        attribName: String,
        crossinline contextInit: ReactiveEvaluationContextBuilder.() -> Unit = {}
    ): Flow<String> { // @formatter:on
        return format(
            name, attribName, reactiveEvaluationContext(localizationManager.currentLocalizations, contextInit)
        )
    }

    /**
     * Convenience overload that builds the reactive context and collects a formatted attribute into a callback.
     *
     * @param name The message identifier.
     * @param attribName The attribute name within the message.
     * @param contextInit Builder to initialize the evaluation context from the current localization file.
     * @param callback Consumer that receives every formatted value.
     */
    inline fun format(
        name: String,
        attribName: String,
        crossinline contextInit: ReactiveEvaluationContextBuilder.() -> Unit = {},
        noinline callback: (String) -> Unit
    ) {
        coroutineScope.launch {
            format(name, attribName, contextInit).collect(callback)
        }
    }

    /**
     * Convenience overload that builds the reactive context and binds a formatted attribute to a property.
     *
     * @param name The message identifier.
     * @param attribName The attribute name within the message.
     * @param contextInit Builder to initialize the evaluation context from the current localization file.
     * @param property A mutable property (e.g., a var) to receive formatted values.
     */
    inline fun format(
        name: String,
        attribName: String,
        crossinline contextInit: ReactiveEvaluationContextBuilder.() -> Unit = {},
        property: KMutableProperty0<String>
    ) {
        coroutineScope.launch {
            format(name, attribName, contextInit).collect(property::set)
        }
    }

    /**
     * Convenience overload that builds the reactive context and writes a formatted attribute to a mutable
     * property on a receiver instance using a context receiver.
     *
     * @param name The message identifier.
     * @param attribName The attribute name within the message.
     * @param contextInit Builder to initialize the evaluation context from the current localization file.
     * @param property A mutable property on the receiver instance to update with formatted values.
     */
    inline context(instance: T) fun <T> format(
        name: String,
        attribName: String,
        crossinline contextInit: ReactiveEvaluationContextBuilder.() -> Unit = {},
        property: KMutableProperty1<T, String>
    ) {
        coroutineScope.launch {
            format(name, attribName, contextInit).collect { text ->
                property.set(instance, text)
            }
        }
    }

    /**
     * Derive a new localization sub-scope from this scope instance,
     * inheriting this scope's [LocalizationManager] instance.
     *
     * @param coroutineContext The coroutine context of the derived scope.
     */
    fun derive(coroutineContext: CoroutineContext): LocalizationScope = LocalizationScope(
        localizationManager = localizationManager, coroutineScope = CoroutineScope(coroutineContext)
    )
}