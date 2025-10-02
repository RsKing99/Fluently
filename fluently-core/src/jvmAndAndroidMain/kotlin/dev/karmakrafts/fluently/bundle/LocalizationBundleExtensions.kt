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

package dev.karmakrafts.fluently.bundle

import dev.karmakrafts.fluently.LocalizationFile
import dev.karmakrafts.fluently.eval.EvaluationContextBuilder
import kotlinx.io.asSource
import kotlinx.io.buffered

/**
 * Creates a [LocalizationBundle] by reading a bundled resource from the classpath and
 * parsing its JSON content.
 *
 * This is a convenience for JVM/Android targets where the bundle JSON is packaged as a resource
 * and accessible via [Class.getResourceAsStream].
 *
 * @param path classpath resource path to the bundle JSON (e.g. "/i18n/bundle.json"). Must start with "/".
 * @return the parsed [LocalizationBundle].
 *
 * @throws NullPointerException if any required resource for the locale cannot be found in the classpath
 *  or if the given [path] doesn't exist within the classpath.
 */
fun LocalizationBundle.Companion.fromResource(path: String): LocalizationBundle {
    return this::class.java.getResourceAsStream(path)!!.reader().use { reader ->
        LocalizationBundle.fromJsonString(reader.readText())
    }
}

/**
 * Loads a locale into this [LocalizationBundle] using resources from the classpath.
 *
 * For each locale file path requested by [LocalizationBundle.loadLocale], this overload opens the
 * corresponding resource stream via [Class.getResourceAsStream] and wires it as a buffered source.
 * This is useful on JVM/Android when locale assets are packaged as resources.
 *
 * @param locale the locale identifier to load (e.g. "en", "en-US").
 * @param globalContextInit optional lambda to populate the global [EvaluationContextBuilder]
 * that will be available during message evaluation.
 * @return the loaded [LocalizationFile] for the given [locale].
 *
 * @throws NullPointerException if any required resource for the locale cannot be found in the classpath.
 */
inline fun LocalizationBundle.loadLocaleFromResource( // @formatter:off
    locale: String,
    crossinline globalContextInit: EvaluationContextBuilder.() -> Unit = {}
): LocalizationFile { // @formatter:on
    return loadLocale(locale, { path ->
        this::class.java.getResourceAsStream(path)!!.asSource().buffered()
    }, globalContextInit)
}