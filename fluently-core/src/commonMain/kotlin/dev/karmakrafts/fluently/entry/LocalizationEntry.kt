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

package dev.karmakrafts.fluently.entry

import dev.karmakrafts.fluently.element.Attribute
import dev.karmakrafts.fluently.element.Element
import dev.karmakrafts.fluently.element.PatternElement
import dev.karmakrafts.fluently.eval.Evaluable
import dev.karmakrafts.fluently.eval.EvaluationContext
import dev.karmakrafts.fluently.util.Named

/**
 * A top-level, named entry in a Fluent localization file.
 *
 * Entries correspond to user-defined messages or terms. Both expose a common shape:
 * - a [name] by which they can be referenced,
 * - a list of pattern [elements] that produce the formatted value, and
 * - optional [attributes] addressable by name.
 *
 * The interface extends [Evaluable] and [Named] so entries can be formatted and participate in
 * cycle detection during evaluation.
 */
sealed interface LocalizationEntry : Element, Named {
    /** The identifier of this entry (message or term). */
    override val name: String

    /**
     * The ordered pattern elements that produce the entry's value when evaluated.
     */
    val elements: List<PatternElement>

    /** A map of attribute name to attribute for this entry; empty if none. */
    val attributes: Map<String, Attribute>

    /**
     * Formats the entry by evaluating and concatenating all [elements] under the given [context].
     *
     * Implementations may wrap this call to add cycle detection bookkeeping (see [Message]).
     */
    override fun evaluate(context: EvaluationContext): String {
        return elements.joinToString("") { element -> element.evaluate(context) }
    }
}