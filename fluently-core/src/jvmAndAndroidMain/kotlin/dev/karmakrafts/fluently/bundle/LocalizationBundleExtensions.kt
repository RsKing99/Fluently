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

fun LocalizationBundle.Companion.fromResource(path: String): LocalizationBundle {
    return this::class.java.getResourceAsStream(path)!!.reader().use { reader ->
        LocalizationBundle.fromJsonString(reader.readText())
    }
}

inline fun LocalizationBundle.loadLocaleFromResource( // @formatter:off
    locale: String,
    crossinline globalContextInit: EvaluationContextBuilder.() -> Unit = {}
): LocalizationFile { // @formatter:on
    return loadLocale(locale, { path ->
        this::class.java.getResourceAsStream(path)!!.asSource().buffered()
    }, globalContextInit)
}