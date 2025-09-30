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

import dev.karmakrafts.fluently.eval.EvaluationContextBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BundleEntry(
    val locale: String,
    @SerialName("display_name") val displayName: String,
    val path: String,
    val defaults: Map<String, DefaultValue> = emptyMap()
) {
    context(builder: EvaluationContextBuilder) fun applyDefaults() {
        builder.apply {
            for ((name, value) in defaults) when (value) {
                is DefaultValue.StringValue -> variable(name, value.value)
                is DefaultValue.LongValue -> variable(name, value.value)
                is DefaultValue.DoubleValue -> variable(name, value.value)
                is DefaultValue.BoolValue -> variable(name, value.value)
            }
        }
    }
}
