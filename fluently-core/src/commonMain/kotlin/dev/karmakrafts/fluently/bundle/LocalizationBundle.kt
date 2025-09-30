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
import dev.karmakrafts.fluently.util.json
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.Serializable
import org.intellij.lang.annotations.Language

@ConsistentCopyVisibility
@Serializable
data class LocalizationBundle private constructor( // @formatter:off
    val version: Int = VERSION,
    val entries: Map<String, BundleEntry> = emptyMap()
) { // @formatter:on
    companion object {
        const val VERSION: Int = 1

        fun fromJsonString(@Language("json") source: String): LocalizationBundle =
            json.decodeFromString<LocalizationBundle>(source).apply {
                check(version == VERSION) { "Mismatched localization bundle version" }
            }

        fun readJsonFrom(source: Source): LocalizationBundle = fromJsonString(source.readString())
    }

    inline val locales: Set<String> get() = entries.keys

    inline fun loadLocale(
        locale: String,
        crossinline globalContextInit: EvaluationContextBuilder.() -> Unit = {},
        resourceProvider: (String) -> Source
    ): LocalizationFile {
        val entry = entries[locale] ?: error("Could not load language $locale")
        return resourceProvider(entry.path).use { source ->
            LocalizationFile.parse(source) {
                globalContextInit()
                entry.applyDefaults()
            }
        }
    }

    fun toJsonString(): String = json.encodeToString(this)
    fun writeJsonTo(sink: Sink) = sink.writeString(toJsonString())
}