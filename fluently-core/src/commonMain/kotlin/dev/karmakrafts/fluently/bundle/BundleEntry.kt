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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Describes a single locale entry within a [LocalizationBundle].
 *
 * Each bundle entry identifies a localized resource file by its [path], provides a human-readable
 * [displayName] for UI, and may declare [defaults] to seed the evaluation context with variables
 * (for example, brand name or feature flags) whenever this locale is loaded.
 *
 * Instances are typically created via JSON deserialization of a localization bundle file.
 *
 * @property displayName A friendly name for the locale suitable for display in UI.
 * @property path Relative or absolute path to the Fluent resource file for this locale.
 * @property defaults A map of variable name to default value injected into the evaluation context when loading.
 */
@Serializable
data class BundleEntry(
    @SerialName("display_name") val displayName: String,
    val path: String,
    val defaults: Map<String, DefaultValue> = emptyMap()
)
