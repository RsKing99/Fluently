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

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Polymorphic
@Serializable
sealed interface DefaultValue {
    @SerialName("string")
    @Serializable
    data class StringValue(val value: String) : DefaultValue

    @SerialName("long")
    @Serializable
    data class LongValue(val value: Long) : DefaultValue

    @SerialName("double")
    @Serializable
    data class DoubleValue(val value: Double) : DefaultValue

    @SerialName("bool")
    @Serializable
    data class BoolValue(val value: Boolean) : DefaultValue
}

internal val defaultValueSerializer: SerializersModule = SerializersModule {
    polymorphic(DefaultValue::class) {
        subclass(DefaultValue.StringValue::class)
        subclass(DefaultValue.LongValue::class)
        subclass(DefaultValue.DoubleValue::class)
        subclass(DefaultValue.BoolValue::class)
    }
}