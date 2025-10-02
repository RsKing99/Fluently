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
import dev.karmakrafts.fluently.eval.EvaluationContextBuilder
import dev.karmakrafts.fluently.eval.evaluationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
class LocalizationScope( // @formatter:off
    @PublishedApi internal val localizationManager: LocalizationManager,
    @PublishedApi internal val coroutineScope: CoroutineScope = CoroutineScope(localizationManager.coroutineContext)
) { // @formatter:on

    private val cache: HashMap<LocalizationKey, SharedFlow<String>> = HashMap()
    private val cacheMutex: Mutex = Mutex()

    /**
     * Formats a localized message identified by [name] using the provided [context].
     *
     * The result is a hot [SharedFlow] that emits and re-emits when the underlying
     * localization data or inputs referenced by [context] change. Within the same scope,
     * identical calls are memoized and share the same upstream flow.
     *
     * @param name The message identifier.
     * @param context An explicit [EvaluationContext] to use for formatting.
     * @return A [SharedFlow] with a replay of the latest formatted string.
     */
    suspend fun format(name: String, context: EvaluationContext): SharedFlow<String> {
        return cacheMutex.withLock {
            cache.getOrPut(LocalizationKey.fromContext(name, context)) {
                localizationManager.format(name, context).shareIn(coroutineScope, SharingStarted.WhileSubscribed(), 1)
            }
        }
    }

    /**
     * Convenience overload that collects the formatted [name] using an explicit [context]
     * and delivers updates to [callback] within this scope's [coroutineScope].
     *
     * @param name The message identifier.
     * @param context The explicit [EvaluationContext] to use for formatting.
     * @param callback Invoked with every formatted value and subsequent updates.
     */
    fun format(name: String, context: EvaluationContext, callback: (String) -> Unit) {
        coroutineScope.launch {
            format(name, context).collect(callback)
        }
    }

    /**
     * Formats a localized message identified by [name] using a lazily constructed [EvaluationContext].
     *
     * The context builder receives the current localization file and is pre-populated by the
     * manager's [LocalizationManager.globalContextInit]. You can further customize it via [contextInit].
     *
     * @param name The message identifier.
     * @param contextInit Builder to configure the [EvaluationContext]. Optional.
     * @return A memoized, hot [SharedFlow] that emits the latest formatted value.
     */
    suspend inline fun format(
        name: String, crossinline contextInit: EvaluationContextBuilder.() -> Unit = {}
    ): SharedFlow<String> {
        val file = localizationManager.currentLocalizations.value
        val context = evaluationContext(file) {
            localizationManager.globalContextInit(this)
            contextInit()
        }
        return format(name, context)
    }

    /**
     * Convenience overload that builds an [EvaluationContext] using [contextInit],
     * then collects the formatted [name] and passes emissions to [callback].
     * Collection runs in this scope's [coroutineScope].
     *
     * @param name The message identifier.
     * @param contextInit Optional builder to customize the [EvaluationContext].
     * @param callback Invoked with every formatted value and subsequent updates.
     */
    inline fun format(
        name: String,
        crossinline contextInit: EvaluationContextBuilder.() -> Unit = {},
        noinline callback: (String) -> Unit
    ) {
        coroutineScope.launch {
            format(name, contextInit).collect(callback)
        }
    }

    /**
     * Formats a specific attribute [attribName] of a localized message [name] using the given [context].
     *
     * Behaves like the non-attribute overload but targets a message attribute instead of the
     * message value. Results are memoized within this scope and exposed as a hot [SharedFlow].
     *
     * @param name The message identifier.
     * @param attribName The attribute name to format (e.g., "title", "label").
     * @param context An explicit [EvaluationContext] to use for formatting.
     * @return A [SharedFlow] with a replay of the latest formatted attribute value.
     */
    suspend fun format(name: String, attribName: String, context: EvaluationContext): SharedFlow<String> {
        return cacheMutex.withLock {
            cache.getOrPut(LocalizationKey.fromContext(name, attribName, context)) {
                localizationManager.format(name, attribName, context)
                    .shareIn(coroutineScope, SharingStarted.WhileSubscribed(), 1)
            }
        }
    }

    /**
     * Convenience overload that collects a formatted attribute [attribName] of message [name]
     * using an explicit [context], delivering updates to [callback] within this scope's [coroutineScope].
     *
     * @param name The message identifier.
     * @param attribName The attribute name to format.
     * @param context The explicit [EvaluationContext] to use for formatting.
     * @param callback Invoked with every formatted value and subsequent updates.
     */
    fun format(name: String, attribName: String, context: EvaluationContext, callback: (String) -> Unit) {
        coroutineScope.launch {
            format(name, attribName, context).collect(callback)
        }
    }

    /**
     * Formats an attribute [attribName] of message [name] using a lazily constructed [EvaluationContext].
     *
     * The context builder is pre-initialized by the manager's global context setup and can be customized
     * via [contextInit]. The resulting flow is memoized within this scope and shared among identical calls.
     *
     * @param name The message identifier.
     * @param attribName The attribute name to format.
     * @param contextInit Optional builder to customize the [EvaluationContext].
     * @return A hot [SharedFlow] emitting the latest formatted attribute value.
     */
    suspend inline fun format(
        name: String, attribName: String, crossinline contextInit: EvaluationContextBuilder.() -> Unit = {}
    ): SharedFlow<String> {
        val file = localizationManager.currentLocalizations.value
        val context = evaluationContext(file) {
            localizationManager.globalContextInit(this)
            contextInit()
        }
        return format(name, attribName, context)
    }

    /**
     * Convenience overload that builds an [EvaluationContext] via [contextInit] and collects a
     * formatted attribute [attribName] of message [name], delivering updates to [callback] within
     * this scope's [coroutineScope].
     *
     * @param name The message identifier.
     * @param attribName The attribute name to format.
     * @param contextInit Optional builder to customize the [EvaluationContext].
     * @param callback Invoked with every formatted value and subsequent updates.
     */
    inline fun format(
        name: String,
        attribName: String,
        crossinline contextInit: EvaluationContextBuilder.() -> Unit = {},
        noinline callback: (String) -> Unit
    ) {
        coroutineScope.launch {
            format(name, attribName, contextInit).collect(callback)
        }
    }
}