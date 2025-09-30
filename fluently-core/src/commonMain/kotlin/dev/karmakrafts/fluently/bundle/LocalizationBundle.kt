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
import dev.karmakrafts.fluently.bundle.LocalizationBundle.Companion.VERSION
import dev.karmakrafts.fluently.bundle.LocalizationBundle.Companion.fromJsonString
import dev.karmakrafts.fluently.bundle.LocalizationBundle.Companion.readJsonFrom
import dev.karmakrafts.fluently.eval.EvaluationContextBuilder
import dev.karmakrafts.fluently.util.json
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.Serializable
import org.intellij.lang.annotations.Language

/**
 * A serializable container describing available locales and resources for an application.
 *
 * The bundle format maps locale codes to [BundleEntry] metadata and provides helpers to load the
 * underlying Fluent files with appropriate default variables. Use [fromJsonString] or [readJsonFrom]
 * to deserialize a bundle definition, and [loadLocale] to obtain a parsed [LocalizationFile].
 *
 * @property version Schema version of the bundle; used to validate compatibility.
 * @property entries Mapping from locale code to the corresponding [BundleEntry].
 */
@ConsistentCopyVisibility
@Serializable
data class LocalizationBundle private constructor( // @formatter:off
    val version: Int = VERSION,
    val entries: Map<String, BundleEntry> = emptyMap()
) { // @formatter:on
    companion object {
        /** Current supported [LocalizationBundle] schema version. */
        const val VERSION: Int = 1

        /**
         * Parses a JSON [source] string into a [LocalizationBundle] and validates [version].
         * @throws IllegalStateException if the bundle version does not match [VERSION].
         */
        fun fromJsonString(@Language("json") source: String): LocalizationBundle =
            json.decodeFromString<LocalizationBundle>(source).apply {
                check(version == VERSION) { "Mismatched localization bundle version" }
            }

        /** Reads UTF‑8 JSON from [source] and delegates to [fromJsonString]. */
        fun readJsonFrom(source: Source): LocalizationBundle = fromJsonString(source.readString())
    }

    /** The set of locale codes available in this bundle. */
    inline val locales: Set<String> get() = entries.keys

    /**
     * Loads and parses the Fluent resource for [locale] into a [LocalizationFile].
     *
     * The [resourceProvider] is responsible for returning a [Source] for the file path declared by the
     * matching [BundleEntry]. The optional [globalContextInit] can seed the evaluation context with variables
     * and functions; defaults from the bundle entry are applied afterward via [BundleEntry.applyDefaults].
     *
     * @param locale The locale code to load.
     * @param globalContextInit Optional initializer for the evaluation context builder.
     * @param resourceProvider Function that returns a [Source] for a given file path.
     * @return The parsed localization file ready for message formatting.
     * @throws IllegalStateException If [locale] is not present in [entries].
     */
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

    /** Serializes this bundle to a compact JSON string. */
    fun toJsonString(): String = json.encodeToString(this)

    /** Writes the JSON representation of this bundle to the given [sink]. */
    fun writeJsonTo(sink: Sink) = sink.writeString(toJsonString())
}