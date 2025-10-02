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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.io.Source
import kotlin.coroutines.CoroutineContext

/**
 * Central reactive entry point for formatting localized messages.
 *
 * This manager loads Fluent localization files for the current [locale] from the provided [bundle]
 * and exposes them as cold and hot reactive streams. It also provides helpers to format messages
 * by name (and optional attribute) either returning nullable results or falling back to a readable
 * placeholder like "<key>" when not found.
 *
 * The manager keeps the default bundle locale loaded and switches current localizations whenever
 * [locale] changes. All formatting functions combine the current locale with the default locale so
 * missing messages are transparently resolved from the default.
 *
 * @param bundle A [LocalizationBundle] that knows how to list locales and load per-locale files.
 * @param resourceProvider A function that opens a resource by path for reading (used by the bundle).
 * @param coroutineContext Coroutine context used for internal reactive pipelines and scoping.
 * @param globalContextInit Builder applied to each formatting [EvaluationContext] to provide shared
 *   variables, functions, or terms common to the whole application.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocalizationManager( // @formatter:off
    val bundle: LocalizationBundle,
    val resourceProvider: (String) -> Source,
    val coroutineContext: CoroutineContext,
    val globalContextInit: EvaluationContextBuilder.() -> Unit = {}
) { // @formatter:on
    companion object // For injecting extensions

    @PublishedApi
    internal val coroutineScope: CoroutineScope = CoroutineScope(coroutineContext)

    /**
     * All locales available in the [bundle]. The default locale is accessible via
     * [LocalizationBundle.defaultLocale].
     */
    inline val locales: Set<String> get() = bundle.locales

    /**
     * Currently selected UI locale as a [MutableStateFlow]. Updating the value triggers reloading
     * of [currentLocalizations]. Initialized with [LocalizationBundle.defaultLocale].
     */
    val locale: MutableStateFlow<String> = MutableStateFlow(bundle.defaultLocale)

    @PublishedApi
    internal val _defaultLocalizations: MutableStateFlow<LocalizationFile> = MutableStateFlow(loadDefaultLocale())

    /**
     * Hot state flow with the default-locale [LocalizationFile]. This is always kept loaded and is
     * used as a fallback for missing messages in the current locale.
     */
    inline val defaultLocalizations: StateFlow<LocalizationFile> get() = _defaultLocalizations

    /**
     * Hot state flow with the [LocalizationFile] for the current [locale]. When the locale equals
     * the default, this simply points to [defaultLocalizations].
     */
    val currentLocalizations: StateFlow<LocalizationFile> =
        locale.combine(defaultLocalizations) { locale, defaultLocalizations ->
            if (locale == bundle.defaultLocale) defaultLocalizations
            else bundle.loadLocale(locale, resourceProvider, globalContextInit)
        }.stateIn(coroutineScope, SharingStarted.Eagerly, _defaultLocalizations.value)

    private fun loadDefaultLocale(): LocalizationFile = bundle.loadDefaultLocale(resourceProvider, globalContextInit)

    /**
     * Formats a message by [name] using the given evaluation [context].
     *
     * Returns a [Flow] that emits the formatted string whenever underlying localizations change,
     * or null if the message is missing both in the current and the default locale.
     */
    fun formatOrNull( // @formatter:off
        name: String,
        context: EvaluationContext
    ): Flow<String?> = currentLocalizations.combine(defaultLocalizations) { current, default ->
        current.formatOrNull(name, context) ?: default.formatOrNull(name, context)
    }

    /**
     * Formats a message by [name] using a lazily built context via [contextInit].
     *
     * The builder is applied on top of a base context created for the current localization file and
     * extended with [globalContextInit]. Returns null if the message cannot be resolved neither in
     * the current nor the default locale.
     */
    inline fun formatOrNull( // @formatter:off
        name: String,
        crossinline contextInit: EvaluationContextBuilder.() -> Unit = {}
    ): Flow<String?> = currentLocalizations.combine(defaultLocalizations) { current, default ->
        current.formatOrNull(name, contextInit) ?: default.formatOrNull(name, contextInit)
    }

    /**
     * Formats an attribute [attribName] of a message [name] using the given [context].
     *
     * Returns null if the attribute cannot be resolved in both current and default locales.
     */
    fun formatOrNull( // @formatter:off
        name: String,
        attribName: String,
        context: EvaluationContext
    ): Flow<String?> = currentLocalizations.combine(defaultLocalizations) { current, default ->
        current.formatOrNull(name, attribName, context) ?: default.formatOrNull(name, attribName, context)
    }

    /**
     * Formats an attribute [attribName] of a message [name] using a lazily built context via
     * [contextInit]. Returns null if the attribute cannot be resolved in both locales.
     */
    inline fun formatOrNull( // @formatter:off
        name: String,
        attribName: String,
        crossinline contextInit: EvaluationContextBuilder.() -> Unit = {}
    ): Flow<String?> = currentLocalizations.combine(defaultLocalizations) { current, default ->
        current.formatOrNull(name, attribName, contextInit) ?: default.formatOrNull(name, attribName, contextInit)
    }

    /**
     * Formats a message by [name] using the given [context].
     *
     * Unlike [formatOrNull], this never emits null. If the message is missing it emits a readable
     * placeholder in the form of "<name>".
     */
    fun format(name: String, context: EvaluationContext): Flow<String> = formatOrNull(name, context)
        .mapLatest { text -> text ?: "<$name>" }

    /**
     * Formats a message by [name] using a lazily built context via [contextInit].
     *
     * Emits a placeholder "<name>" if the message cannot be found.
     */
    fun format(name: String, contextInit: EvaluationContextBuilder.() -> Unit = {}): Flow<String> =
        formatOrNull(name, contextInit).mapLatest { text -> text ?: "<$name>" }

    /**
     * Formats an attribute [attribName] of a message [name] using the given [context].
     *
     * Emits a placeholder "<name.attrib>" if the attribute cannot be found.
     */
    fun format(name: String, attribName: String, context: EvaluationContext): Flow<String> = formatOrNull(name, attribName, context)
        .mapLatest { text -> text ?: "<$name.$attribName>" }

    /**
     * Formats an attribute [attribName] of a message [name] using a lazily built context via
     * [contextInit]. Emits a placeholder "<name.attrib>" if the attribute cannot be found.
     */
    fun format(name: String, attribName: String, contextInit: EvaluationContextBuilder.() -> Unit = {}): Flow<String> =
        formatOrNull(name, attribName, contextInit).mapLatest { text -> text ?: "<$name.$attribName>" }

    /**
     * Forces reloading of localization files.
     *
     * Reloads the default localization file and then re-emits the current [locale] to refresh the
     * current locale file if needed.
     */
    fun reload() {
        // Reload the default localization file
        _defaultLocalizations.value = loadDefaultLocale()
        // Re-emit current locale to reload current localization file
        locale.update { it }
    }
}