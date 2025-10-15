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

package dev.karmakrafts.fluently.element

import dev.karmakrafts.fluently.eval.EvaluationContext
import dev.karmakrafts.fluently.util.Named
import dev.karmakrafts.fluently.util.TokenRange

/**
 * A named attribute belonging to a message or term.
 *
 * Attributes behave like mini-patterns attached to an entry and can be referenced separately
 * (for example, brand-name.title). They evaluate by concatenating their [elements]. During
 * evaluation, the attribute participates in cycle detection through the evaluation context's
 * parent stack.
 *
 * @property entryName The name of the owning entry (message or term).
 * @property name The attribute identifier as referenced in source and APIs.
 * @property elements Ordered pattern elements that compose the attribute's value.
 */
data class Attribute( // @formatter:off
    override val tokenRange: TokenRange,
    val entryName: String,
    override val name: String,
    val elements: List<PatternElement>
) : Element, Named { // @formatter:on
    /**
     * Evaluates this attribute under [context] with cycle-detection bookkeeping.
     */
    override fun evaluate(context: EvaluationContext): String {
        context.pushParent(this)
        val result = elements.joinToString("") { element -> element.evaluate(context) }
        context.popParent()
        return result
    }
}