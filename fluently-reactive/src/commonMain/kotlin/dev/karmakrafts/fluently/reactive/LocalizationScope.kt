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

import dev.karmakrafts.fluently.util.Accessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1

interface LocalizationScope {
    val localizationManager: LocalizationManager
    val coroutineScope: CoroutineScope

    fun format(name: String, context: ReactiveEvaluationContext): Flow<String>
    fun format(name: String, attribName: String, context: ReactiveEvaluationContext): Flow<String>
}

fun LocalizationScope.format( // @formatter:off
    name: String,
    context: ReactiveEvaluationContext,
    callback: (String) -> Unit
) = coroutineScope.launch { // @formatter:on
    format(name, context).collect(callback)
}

fun LocalizationScope.format( // @formatter:off
    name: String,
    context: ReactiveEvaluationContext,
    property: KMutableProperty0<String>
) = coroutineScope.launch { // @formatter:on
    format(name, context).collect(property::set)
}

context(instance: T) fun <T> LocalizationScope.format( // @formatter:off
    name: String,
    context: ReactiveEvaluationContext,
    property: KMutableProperty1<T, String>
) = coroutineScope.launch { // @formatter:on
    format(name, context).collect { text ->
        property.set(instance, text)
    }
}

inline fun LocalizationScope.format( // @formatter:off
    name: String,
    crossinline contextInit: ReactiveEvaluationContextSpec = {}
): Flow<String> { // @formatter:on
    return format(name, reactiveEvaluationContext(localizationManager.currentLocalizations, contextInit))
}

inline fun LocalizationScope.format( // @formatter:off
    name: String,
    crossinline contextInit: ReactiveEvaluationContextSpec = {},
    noinline callback: (String) -> Unit
) = coroutineScope.launch { // @formatter:on
    format(name, contextInit).collect(callback)
}

inline fun LocalizationScope.format( // @formatter:off
    name: String,
    crossinline contextInit: ReactiveEvaluationContextSpec = {},
    property: KMutableProperty0<String>
) = coroutineScope.launch { // @formatter:on
    format(name, contextInit).collect(property::set)
}

inline context(instance: T) fun <T> LocalizationScope.format( // @formatter:off
    name: String,
    crossinline contextInit: ReactiveEvaluationContextSpec = {},
    property: KMutableProperty1<T, String>
) = coroutineScope.launch { // @formatter:on
    format(name, contextInit).collect { text ->
        property.set(instance, text)
    }
}

fun LocalizationScope.format( // @formatter:off
    name: String,
    attribName: String,
    context: ReactiveEvaluationContext,
    callback: (String) -> Unit
) = coroutineScope.launch { // @formatter:on
    format(name, attribName, context).collect(callback)
}

fun LocalizationScope.format( // @formatter:off
    name: String,
    attribName: String,
    context: ReactiveEvaluationContext,
    property: KMutableProperty0<String>
) = coroutineScope.launch { // @formatter:on
    format(name, attribName, context).collect(property::set)
}

context(instance: T) fun <T> LocalizationScope.format( // @formatter:off
    name: String,
    attribName: String,
    context: ReactiveEvaluationContext,
    property: KMutableProperty1<T, String>
) = coroutineScope.launch { // @formatter:on
    format(name, attribName, context).collect { text ->
        property.set(instance, text)
    }
}

inline fun LocalizationScope.format( // @formatter:off
    name: String,
    attribName: String,
    crossinline contextInit: ReactiveEvaluationContextSpec = {}
): Flow<String> { // @formatter:on
    return format(
        name, attribName, reactiveEvaluationContext(localizationManager.currentLocalizations, contextInit)
    )
}

inline fun LocalizationScope.format( // @formatter:off
    name: String,
    attribName: String,
    crossinline contextInit: ReactiveEvaluationContextSpec = {},
    noinline callback: (String) -> Unit
) = coroutineScope.launch { // @formatter:on
    format(name, attribName, contextInit).collect(callback)
}

inline fun LocalizationScope.format( // @formatter:off
    name: String,
    attribName: String,
    crossinline contextInit: ReactiveEvaluationContextSpec,
    property: KMutableProperty0<String>
) = coroutineScope.launch { // @formatter:on
    format(name, attribName, contextInit).collect(property::set)
}

inline context(instance: T) fun <T> LocalizationScope.format( // @formatter:off
    name: String,
    attribName: String,
    crossinline contextInit: ReactiveEvaluationContextSpec = {},
    property: KMutableProperty1<T, String>
) = coroutineScope.launch { // @formatter:on
    format(name, attribName, contextInit).collect { text ->
        property.set(instance, text)
    }
}

fun LocalizationScope.formatting(context: ReactiveEvaluationContext): Accessor<Flow<String>> =
    Accessor { format(it, context) }

inline fun LocalizationScope.formatting(crossinline contextInit: ReactiveEvaluationContextSpec = {}): Accessor<Flow<String>> =
    Accessor { format(it, contextInit) }

fun LocalizationScope.formatting(entryName: String, context: ReactiveEvaluationContext): Accessor<Flow<String>> =
    Accessor { attribName ->
        format(entryName, attribName, context)
    }

inline fun LocalizationScope.formatting(
    entryName: String, crossinline contextInit: ReactiveEvaluationContextSpec = {}
): Accessor<Flow<String>> = Accessor { attribName ->
    format(entryName, attribName, contextInit)
}