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

/**
 * Creates a JVM/Android-backed LocalizationManager that loads Fluent resources from the classpath.
 *
 * This convenience factory wires LocalizationManager with a resourceProvider that uses
 * Class.getResourceAsStream to open files bundled into your application JAR or Android APK.
 * Use this overload when you already have an initialized LocalizationBundle instance
 * (e.g., created via LocalizationBundle.fromResource).
 *
 * Typical usage:
 * ```
 * val manager = LocalizationManager.fromResources(bundle, Dispatchers.Main)
 * ```
 *
 * @param bundle The bundle describing available locales and how to locate per-locale resources.
 * @param coroutineContext Coroutine context used by the manager's reactive pipelines.
 * @param globalContextInit Optional builder applied to each EvaluationContext used for formatting
 *   to inject shared variables/functions/terms.
 *
 * @throws [NullPointerException] when the one of the bundle's Fluent files does not
 *  exist within the current classpath.
 */
fun LocalizationManager.Companion.fromResources(
    bundle: LocalizationBundle,
    coroutineContext: CoroutineContext,
    globalContextInit: EvaluationContextBuilder.() -> Unit = {}
): LocalizationManager = LocalizationManager(bundle, { path ->
    this::class.java.getResourceAsStream(path)!!.asSource().buffered()
}, coroutineContext, globalContextInit)

/**
 * Creates a LocalizationManager loading resources from a base classpath directory.
 *
 * The function expects a directory structure packaged into the classpath as follows:
 * - "$basePath/languages.json" — bundle manifest with locales and default locale
 * - "$basePath/<file>.ftl" — per-locale Fluent files referenced by the bundle
 *
 * Internally it constructs the LocalizationBundle from languages.json and uses
 * Class.getResourceAsStream to read all subsequent files.
 *
 * Example:
 * ```
 * val manager = LocalizationManager.fromResources("/i18n", Dispatchers.Main)
 * ```
 *
 * @param basePath Classpath base directory where languages.json and localization files live
 *             (for example: "/i18n" or "/localization").
 * @param coroutineContext Coroutine context used by the manager's reactive pipelines.
 * @param globalContextInit Optional builder applied to each EvaluationContext used for formatting
 *   to inject shared variables/functions/terms.
 *
 * @throws [NullPointerException] when the `languages.json` or one of the bundle's
 *  Fluent files does not exist witih the current classpath.
 */
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