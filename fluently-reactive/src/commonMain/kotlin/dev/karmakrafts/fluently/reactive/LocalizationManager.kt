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
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.io.Source
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class LocalizationManager( // @formatter:off
    val bundle: LocalizationBundle,
    val resourceProvider: (String) -> Source,
    coroutineContext: CoroutineContext,
    @PublishedApi internal val globalContextInit: EvaluationContextBuilder.() -> Unit = {}
) { // @formatter:on
    companion object

    @PublishedApi
    internal val coroutineScope: CoroutineScope = CoroutineScope(coroutineContext)

    inline val locales: Set<String> get() = bundle.locales

    val locale: MutableStateFlow<String> = MutableStateFlow(bundle.defaultLocale)

    @PublishedApi
    internal val _defaultLocalizations: MutableStateFlow<LocalizationFile> = MutableStateFlow(loadDefaultLocale())
    val defaultLocalizations: StateFlow<LocalizationFile> = _defaultLocalizations.asStateFlow()

    val currentLocalizations: SharedFlow<LocalizationFile> =
        locale.combine(defaultLocalizations) { locale, defaultLocalizations ->
            if (locale == bundle.defaultLocale) defaultLocalizations
            else bundle.loadLocale(locale, resourceProvider, globalContextInit)
        }.shareIn( // @formatter:off
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            replay = 1
        ) // @formatter:on

    private fun loadDefaultLocale(): LocalizationFile = bundle.loadDefaultLocale(resourceProvider, globalContextInit)

    fun formatOrNull( // @formatter:off
        name: String,
        context: EvaluationContext
    ): Flow<String?> = currentLocalizations.combine(defaultLocalizations) { current, default ->
        current.formatOrNull(name, context)
            ?: default.formatOrNull(name, context)
    }

    inline fun formatOrNull( // @formatter:off
        name: String,
        crossinline contextInit: EvaluationContextBuilder.() -> Unit = {}
    ): Flow<String?> = currentLocalizations.combine(defaultLocalizations) { current, default ->
        current.formatOrNull(name, contextInit)
            ?: default.formatOrNull(name, contextInit)
    }

    fun formatOrNull( // @formatter:off
        name: String,
        attribName: String,
        context: EvaluationContext
    ): Flow<String?> = currentLocalizations.combine(defaultLocalizations) { current, default ->
        current.formatOrNull(name, attribName, context)
            ?: default.formatOrNull(name, attribName, context)
    }

    inline fun formatOrNull( // @formatter:off
        name: String,
        attribName: String,
        crossinline contextInit: EvaluationContextBuilder.() -> Unit = {}
    ): Flow<String?> = currentLocalizations.combine(defaultLocalizations) { current, default ->
        current.formatOrNull(name, attribName, contextInit)
            ?: default.formatOrNull(name, attribName, contextInit)
    }

    fun format(name: String, context: EvaluationContext): Flow<String> = formatOrNull(name, context)
        .mapLatest { text -> text ?: "<$name>" }

    fun format(name: String, contextInit: EvaluationContextBuilder.() -> Unit = {}): Flow<String> =
        formatOrNull(name, contextInit).mapLatest { text -> text ?: "<$name>" }

    fun format(name: String, attribName: String, context: EvaluationContext): Flow<String> = formatOrNull(name, context)
        .mapLatest { text -> text ?: "<$name.$attribName>" }

    fun format(name: String, attribName: String, contextInit: EvaluationContextBuilder.() -> Unit = {}): Flow<String> =
        formatOrNull(name, attribName, contextInit).mapLatest { text -> text ?: "<$name.$attribName>" }

    fun reload() {
        // Reload the default localization file
        _defaultLocalizations.value = loadDefaultLocale()
        // Re-emit current locale to reload current localization file
        locale.update { it }
    }
}