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

import dev.karmakrafts.fluently.LocalizationFile
import dev.karmakrafts.fluently.bundle.LocalizationBundle
import dev.karmakrafts.fluently.eval.EvaluationContext
import dev.karmakrafts.fluently.eval.EvaluationContextBuilder
import dev.karmakrafts.fluently.eval.EvaluationContextSpec
import dev.karmakrafts.fluently.util.Accessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.io.Source
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
/**
 * Manages reactive localization loading and formatting.
 *
 * This class bridges a dynamic [LocalizationBundle] (which may change over time)
 * with reactive consumers using Kotlin Flows. It:
 * - Tracks available locales and the current locale.
 * - Loads the default and selected locale files on demand using [resourceProvider].
 * - Exposes formatting helpers that emit updated strings whenever inputs change
 *   (bundle, locale, or evaluation context).
 *
 * Typical usage:
 * - Provide a [bundle] as a Flow if your bundle can change at runtime (e.g. hot reload),
 *   or use the secondary constructor with a static bundle.
 * - Provide [resourceProvider] to open localization resource streams by path.
 * - Provide a [coroutineContext] used to back internal StateFlows and coroutines.
 * - Optionally provide [globalContextInit] to pre-populate the evaluation context shared
 *   by all formatting operations (e.g. global functions or variables).
 *
 * Threading/flow semantics:
 * - Internally, a [CoroutineScope] is created from [coroutineContext].
 * - All public StateFlows are hot and start eagerly.
 * - [isLoading] is true while the selected locale has not yet been loaded and
 *   [LocalizationFile.empty] is being used.
 *
 * @property bundle A flow of localization bundles. Use [flowOf] for a static bundle.
 * @property resourceProvider Provides a Source for a given resource path found in the bundle.
 * @property coroutineContext Coroutine context used for internal scope and state management.
 * @property globalContextInit Initializes a global evaluation context shared by all formatting operations.
 *  This is invoked whenever a localization file is loaded.
 */
class LocalizationManager( // @formatter:off
    val bundle: Flow<LocalizationBundle>,
    val resourceProvider: suspend (String) -> Source,
    val coroutineContext: CoroutineContext,
    val globalContextInit: EvaluationContextSpec = {}
) { // @formatter:on
    /** Companion for extension injection. */
    companion object; // For injecting extensions

    /**
     * Creates a manager with a static [bundle].
     * For ABI compatibility with 1.3.X and earlier.
     */
    constructor(
        bundle: LocalizationBundle,
        resourceProvider: suspend (String) -> Source,
        coroutineContext: CoroutineContext,
        globalContextInit: EvaluationContextBuilder.() -> Unit = {}
    ) : this(flowOf(bundle), resourceProvider, coroutineContext, globalContextInit)

    @PublishedApi
    internal val coroutineScope: CoroutineScope = CoroutineScope(coroutineContext)

    /**
     * All locale codes available in the current [bundle].
     * Emits a new set when the bundle changes.
     */
    val locales: StateFlow<Set<String>> = bundle.mapLatest { bundle ->
        bundle.locales
    }.stateIn(coroutineScope, SharingStarted.Eagerly, emptySet())

    /**
     * Localizations loaded for the bundle's default locale.
     * This is used as a fallback when a message is missing in the current locale.
     */
    val defaultLocalizations: StateFlow<LocalizationFile> = bundle.mapLatest { bundle ->
        bundle.loadDefaultLocaleSuspend(resourceProvider, globalContextInit)
    }.stateIn(coroutineScope, SharingStarted.Eagerly, LocalizationFile.empty)

    /**
     * The currently selected locale. If not set explicitly, it will initialize
     * to the bundle's default locale the first time it becomes available.
     */
    val locale: MutableStateFlow<String?> = MutableStateFlow<String?>(null).apply {
        coroutineScope.launch { // @formatter:off
            bundle.mapLatest { bundle -> bundle.defaultLocale }
                .distinctUntilChanged()
                .collect { locale ->
                    if(value != null) return@collect
                    value = locale
                }
        } // @formatter:on
    }

    /**
     * Localizations loaded for the [locale] currently selected.
     * Falls back to [LocalizationFile.empty] until the data is loaded.
     */
    val currentLocalizations: StateFlow<LocalizationFile> =
        combine(bundle, locale.filterNotNull(), defaultLocalizations) { bundle, locale, defaultLocalizations ->
            if (locale == bundle.defaultLocale) return@combine defaultLocalizations
            bundle.loadLocaleSuspend(locale, resourceProvider, globalContextInit)
        }.stateIn(coroutineScope, SharingStarted.Eagerly, LocalizationFile.empty)

    /** True while [currentLocalizations] has not finished loading. */
    val isLoading: StateFlow<Boolean> = currentLocalizations.mapLatest { file ->
        file === LocalizationFile.empty
    }.stateIn(coroutineScope, SharingStarted.Eagerly, true)

    /**
     * Formats a message by name with the supplied reactive [context].
     * Returns null if the message is not found in both current and default localizations.
     */
    fun formatOrNull( // @formatter:off
        name: String,
        context: ReactiveEvaluationContext
    ): Flow<String?> {
        val contextFlow = context.asContextFlow()
        return combine(currentLocalizations, defaultLocalizations, contextFlow) { current, default, context ->
            current.formatOrNull(name, context) ?: default.formatOrNull(name, context)
        }
    }

    /**
     * Formats a message by name with a lazily built reactive context.
     * [contextInit] is invoked each time a localization file is used to build the context.
     */
    inline fun formatOrNull( // @formatter:off
        name: String,
        crossinline contextInit: ReactiveEvaluationContextSpec = {}
    ): Flow<String?> {
        return formatOrNull(name, reactiveEvaluationContext(currentLocalizations, contextInit))
    }

    /**
     * Formats a message attribute by [name] and [attribName] with the supplied reactive [context].
     * Returns null if the attribute is not found in both current and default localizations.
     */
    fun formatOrNull( // @formatter:off
        name: String,
        attribName: String,
        context: ReactiveEvaluationContext
    ): Flow<String?> {
        val contextFlow = context.asContextFlow()
        return combine(currentLocalizations, defaultLocalizations, contextFlow) { current, default, context ->
            current.formatOrNull(name, attribName, context) ?: default.formatOrNull(name, attribName, context)
        }
    }

    /**
     * Formats a message attribute with a lazily built reactive context.
     * [contextInit] is invoked each time a localization file is used to build the context.
     */
    inline fun formatOrNull( // @formatter:off
        name: String,
        attribName: String,
        crossinline contextInit: ReactiveEvaluationContextSpec = {}
    ): Flow<String?> {
        return formatOrNull(name, attribName, reactiveEvaluationContext(currentLocalizations, contextInit))
    }

    /** Like [formatOrNull] but substitutes with "<name>" if missing. */
    fun format(name: String, context: ReactiveEvaluationContext): Flow<String> = formatOrNull(name, context)
        .mapLatest { text -> text ?: "<$name>" }

    /** Like [formatOrNull] but substitutes with "<name>" if missing. */
    fun format(name: String, contextInit: ReactiveEvaluationContextSpec = {}): Flow<String> =
        formatOrNull(name, contextInit).mapLatest { text -> text ?: "<$name>" }

    /** Like [formatOrNull] for attributes but substitutes with "<name.attribName>" if missing. */
    fun format(name: String, attribName: String, context: ReactiveEvaluationContext): Flow<String> = formatOrNull(name, attribName, context)
        .mapLatest { text -> text ?: "<$name.$attribName>" }

    /** Like [formatOrNull] for attributes but substitutes with "<name.attribName>" if missing. */
    fun format(name: String, attribName: String, contextInit: ReactiveEvaluationContextSpec = {}): Flow<String> =
        formatOrNull(name, attribName, contextInit).mapLatest { text -> text ?: "<$name.$attribName>" }

    /**
     * Returns an accessor that formats message values by name.
     *
     * The produced [Accessor] accepts a message identifier and returns the formatted string,
     * applying this file's [globalContextInit].
     */
    fun formatting(context: ReactiveEvaluationContext): Accessor<Flow<String>> = Accessor { format(it, context) }

    /**
     * Returns an accessor that formats message values by name.
     *
     * The produced [Accessor] accepts a message identifier and returns the formatted string,
     * applying this file's [globalContextInit].
     */
    fun formatting(contextInit: ReactiveEvaluationContextSpec = {}): Accessor<Flow<String>> = Accessor { format(it, contextInit) }

    /**
     * Returns an accessor for attributes of the given [entryName].
     *
     * The resulting [Accessor] accepts an attribute name and returns the formatted value for
     * the attribute of the specified message, applying this file's [globalContextInit].
     */
    fun formatting(entryName: String, context: ReactiveEvaluationContext): Accessor<Flow<String>> = Accessor { attribName ->
        format(entryName, attribName, context)
    }

    /**
     * Returns an accessor for attributes of the given [entryName].
     *
     * The resulting [Accessor] accepts an attribute name and returns the formatted value for
     * the attribute of the specified message, applying this file's [globalContextInit].
     */
    fun formatting(entryName: String, contextInit: ReactiveEvaluationContextSpec = {}): Accessor<Flow<String>> =
        Accessor { attribName ->
            format(entryName, attribName, contextInit)
        }

    /** Triggers reload by re-applying the current [locale] value. */
    fun reload() = locale.update { it }
}