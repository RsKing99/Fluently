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
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * A polymorphic default value that can seed an [EvaluationContextBuilder] with a variable.
 *
 * Implementations correspond to primitive types supported by the evaluation context. These values are
 * typically declared in a [BundleEntry] and applied when a [LocalizationBundle] loads a locale, so that
 * messages can rely on predefined variables.
 */
@Polymorphic
@Serializable
sealed interface DefaultValue {
    /** A string default value injected under a given variable name. */
    @SerialName("string")
    @Serializable
    data class StringValue(val value: String) : DefaultValue {
        /** Registers this string [value] as a variable with the provided [name]. */
        context(builder: EvaluationContextBuilder) override fun applyToContext(name: String) {
            builder.variable(name, value)
        }
    }

    /** A long (integral) default value injected under a given variable name. */
    @SerialName("long")
    @Serializable
    data class LongValue(val value: Long) : DefaultValue {
        /** Registers this long [value] as a variable with the provided [name]. */
        context(builder: EvaluationContextBuilder) override fun applyToContext(name: String) {
            builder.variable(name, value)
        }
    }

    /** A double (floating‑point) default value injected under a given variable name. */
    @SerialName("double")
    @Serializable
    data class DoubleValue(val value: Double) : DefaultValue {
        /** Registers this double [value] as a variable with the provided [name]. */
        context(builder: EvaluationContextBuilder) override fun applyToContext(name: String) {
            builder.variable(name, value)
        }
    }

    /** A boolean default value injected under a given variable name. */
    @SerialName("bool")
    @Serializable
    data class BoolValue(val value: Boolean) : DefaultValue {
        /** Registers this boolean [value] as a variable with the provided [name]. */
        context(builder: EvaluationContextBuilder) override fun applyToContext(name: String) {
            builder.variable(name, value)
        }
    }

    /**
     * Applies this default value to the receiver [EvaluationContextBuilder] under the provided [name].
     *
     * Implementations map to the appropriate [EvaluationContextBuilder.variable] overloads.
     */
    context(builder: EvaluationContextBuilder) fun applyToContext(name: String)
}

/** Serializer module registering all [DefaultValue] implementations for polymorphic JSON. */
internal val defaultValueSerializer: SerializersModule = SerializersModule {
    polymorphic(DefaultValue::class) {
        subclass(DefaultValue.StringValue::class)
        subclass(DefaultValue.LongValue::class)
        subclass(DefaultValue.DoubleValue::class)
        subclass(DefaultValue.BoolValue::class)
    }
}