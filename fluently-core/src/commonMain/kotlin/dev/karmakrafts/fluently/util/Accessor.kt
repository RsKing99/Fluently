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

package dev.karmakrafts.fluently.util

import kotlin.jvm.JvmInline
import kotlin.reflect.KProperty

/**
 * A lightweight delegated-property accessor.
 *
 * @param T The type of value provided by this accessor.
 * @property getter Function that receives the delegated property's name and
 * returns the corresponding value.
 */
@JvmInline
value class Accessor<T>(
    private val getter: (String) -> T
) {
    /**
     * Provides the value for a delegated property by invoking the [getter]
     * with the property's [KProperty.name].
     *
     * @param thisRef The object containing the delegated property; may be null
     * for top-level or local properties. Not used by this implementation.
     * @param property Reflection information about the delegated property.
     * @return The value obtained from [getter] for the given property name.
     */
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return getter(property.name)
    }
}