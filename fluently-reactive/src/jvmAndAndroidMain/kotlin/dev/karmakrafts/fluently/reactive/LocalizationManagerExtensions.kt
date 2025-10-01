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

import dev.karmakrafts.fluently.bundle.LocalizationBundle
import dev.karmakrafts.fluently.bundle.fromResource
import dev.karmakrafts.fluently.eval.EvaluationContextBuilder
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlin.coroutines.CoroutineContext

fun LocalizationManager.Companion.fromResources(
    bundle: LocalizationBundle,
    coroutineContext: CoroutineContext,
    globalContextInit: EvaluationContextBuilder.() -> Unit = {}
): LocalizationManager = LocalizationManager(bundle, { path ->
    this::class.java.getResourceAsStream(path)!!.asSource().buffered()
}, coroutineContext, globalContextInit)

fun LocalizationManager.Companion.fromResources( // @formatter:off
    basePath: String,
    coroutineContext: CoroutineContext,
    globalContextInit: EvaluationContextBuilder.() -> Unit = {}
): LocalizationManager { // @formatter:on
    val bundle = LocalizationBundle.fromResource("$basePath/languages.json")
    return LocalizationManager(bundle, { path ->
        this::class.java.getResourceAsStream("$basePath/$path")!!.asSource().buffered()
    }, coroutineContext, globalContextInit)
}