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
import dev.karmakrafts.fluently.element.PatternElement
import dev.karmakrafts.fluently.eval.EvaluationContext

/**
 * A private reusable Fluent term entry (identifier starts with a dash in source).
 *
 * Terms are intended to be referenced from within messages (optionally with parameters) but are not
 * formatted directly. They may define [attributes] and contain pattern [elements], similar to messages.
 *
 * Attempting to evaluate a term directly will fail; terms should be resolved and formatted through the
 * referencing message or a lowered expression.
 *
 * @property name Unique identifier of this term (without the leading dash).
 * @property elements Ordered pattern elements that compose the term's value.
 * @property attributes Named attributes available on this term; empty if none.
 */
data class Term(
    override val name: String,
    override val elements: List<PatternElement>,
    override val attributes: Map<String, Attribute>
) : LocalizationEntry {
    override fun evaluate(context: EvaluationContext): String {
        error("Terms cannot be evaluated at runtime")
    }
}