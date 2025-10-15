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
import dev.karmakrafts.fluently.eval.EvaluationContextSpec
import dev.karmakrafts.fluently.expr.DefaultExprScope
import dev.karmakrafts.fluently.util.json
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.SerialName
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
 * @property defaultLocale The BCP-47 language code of the fallback locale used by this localization bundle.
 * @property entries Mapping from locale code to the corresponding [BundleEntry].
 */
@ConsistentCopyVisibility
@Serializable
data class LocalizationBundle private constructor( // @formatter:off
    val version: Int = VERSION,
    @SerialName("default_locale") val defaultLocale: String,
    val entries: Map<String, BundleEntry> = emptyMap(),
    val defaults: Map<String, DefaultValue> = emptyMap()
) { // @formatter:on
    companion object {
        /** Current supported [LocalizationBundle] schema version. */
        const val VERSION: Int = 1

        /**
         * Parses a JSON [source] string into a [LocalizationBundle] and validates [version].
         * @throws IllegalStateException if the bundle version does not match [VERSION].
         */
        @Throws(FluentlyBundleException::class)
        fun fromJsonString(@Language("json") source: String): LocalizationBundle =
            json.decodeFromString<LocalizationBundle>(source).apply {
                if (version != VERSION) {
                    throw FluentlyBundleException("Mismatched localization bundle version")
                }
            }

        /** Reads UTF‑8 JSON from [source] and delegates to [fromJsonString]. */
        fun readJsonFrom(source: Source): LocalizationBundle = fromJsonString(source.readString())
    }

    /** The set of locale codes available in this bundle. */
    inline val locales: Set<String> get() = entries.keys

    /**
     * Returns a human‑readable display name for the given [locale] code.
     *
     * If the bundle contains an entry for [locale] with a custom [BundleEntry.displayName],
     * that value is returned; otherwise the original [locale] code is returned as a
     * sensible fallback. This is useful when presenting a list of languages in UI where
     * some locales may not provide a localized name.
     *
     * @param locale BCP‑47 locale code to look up.
     * @return The display name from the bundle entry, or [locale] if not defined.
     */
    fun getLocaleName(locale: String): String = entries[locale]?.displayName ?: locale

    /**
     * Attempts to find the closest matching locale defined by this bundle
     * based on the given locale.
     * This will perform a lookup over all entries, and if no match is found,
     * the aliases of each entry will be sequentially checked to find a possible
     * match.
     *
     * @param locale The requested locale.
     * @return The locale closest to the requested one based on this
     *  bundle's entries or null if no matching locale could be found.
     */
    fun findClosestLocale(locale: String): String? {
        if (entries[locale] != null) return locale // Fast path, we don't need to look at aliases
        for ((closestLocale, entry) in entries) {
            if (locale !in entry.aliases) continue
            return closestLocale
        }
        return null
    }

    /**
     * Attempt to find the closes matching bundle entry for the given locale.
     * See [findClosestLocale] for details on the behaviour.
     *
     * @param locale The locale for which to retrieve the closest bundle entry.
     * @return The closest matching bundle entry for the given locale or null
     *  if no matching entry could be found.
     */
    fun findClosestEntry(locale: String): BundleEntry? {
        val closestLocale = findClosestLocale(locale) ?: return null
        return entries[closestLocale]
    }

    @Throws(FluentlyBundleException::class)
    fun getClosestEntryOrDefault(locale: String): BundleEntry =
        findClosestEntry(locale) ?: findClosestEntry(defaultLocale)
        ?: throw FluentlyBundleException("Could not load language $locale")

    /**
     * Loads and parses the Fluent resource for [locale] into a [LocalizationFile].
     *
     * The [resourceProvider] is responsible for returning a [Source] for the file path declared by the
     * matching [BundleEntry]. The optional [globalContextInit] can seed the evaluation context with variables
     * and functions.
     *
     * @param locale The locale code to load.
     * @param globalContextInit Optional initializer for the evaluation context builder.
     * @param resourceProvider Function that returns a [Source] for a given file path.
     * @return The parsed localization file ready for message formatting.
     * @throws IllegalStateException If [locale] is not present in [entries].
     */
    inline fun loadLocale( // @formatter:off
        locale: String,
        resourceProvider: (String) -> Source,
        crossinline globalContextInit: EvaluationContextSpec = {}
    ): LocalizationFile { // @formatter:on
        val entry = getClosestEntryOrDefault(locale)
        return resourceProvider(entry.path).use { source ->
            LocalizationFile.parse(source) {
                globalContextInit()
                variables(defaults.mapValues { (_, default) -> with(DefaultExprScope) { default.asExpr() } })
                variables(entry.defaults.mapValues { (_, default) -> with(DefaultExprScope) { default.asExpr() } })
            }
        }
    }

    /** Same as [loadLocale] but wires through resource provider suspension */
    suspend inline fun loadLocaleSuspend(
        locale: String,
        resourceProvider: suspend (String) -> Source,
        crossinline globalContextInit: EvaluationContextSpec = {}
    ): LocalizationFile {
        val entry = getClosestEntryOrDefault(locale)
        return resourceProvider(entry.path).use { source ->
            LocalizationFile.parse(source) {
                globalContextInit()
                variables(defaults.mapValues { (_, default) -> default.asExpr() })
                variables(entry.defaults.mapValues { (_, default) -> default.asExpr() })
            }
        }
    }

    /**
     * Loads and parses the Fluent resource for the bundle's [defaultLocale].
     *
     * This is a convenience wrapper around [loadLocale] that delegates to it with
     * [defaultLocale]. Any variables or functions provided by [globalContextInit]
     * are added to the evaluation context before the entry's default values are applied.
     *
     * @param globalContextInit Optional initializer for the evaluation context builder.
     * It can be used to register global variables and functions available to all messages.
     * @param resourceProvider Function that returns a [Source] for a given file path.
     * Typically, this opens the .ftl file declared by the matching [BundleEntry].
     * @return The parsed localization file for [defaultLocale].
     * @throws IllegalStateException If the [defaultLocale] is not present in [entries].
     */
    inline fun loadDefaultLocale( // @formatter:off
        resourceProvider: (String) -> Source,
        crossinline globalContextInit: EvaluationContextSpec = {}
    ): LocalizationFile { // @formatter:on
        return loadLocale(defaultLocale, resourceProvider, globalContextInit)
    }

    /** Same as [loadDefaultLocale] but wires through resource provider suspension */
    suspend inline fun loadDefaultLocaleSuspend( // @formatter:off
        resourceProvider: suspend (String) -> Source,
        crossinline globalContextInit: EvaluationContextSpec = {}
    ): LocalizationFile { // @formatter:on
        return loadLocaleSuspend(defaultLocale, resourceProvider, globalContextInit)
    }

    /** Serializes this bundle to a compact JSON string. */
    fun toJsonString(): String = json.encodeToString(this)

    /** Writes the JSON representation of this bundle to the given [sink]. */
    fun writeJsonTo(sink: Sink) = sink.writeString(toJsonString())
}