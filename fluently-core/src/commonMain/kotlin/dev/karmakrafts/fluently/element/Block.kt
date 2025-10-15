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
import dev.karmakrafts.fluently.util.TokenRange

/**
 * A block element that inserts a newline before the contained [element].
 *
 * This represents a continuation line in Fluent patterns. During evaluation the inner element is
 * evaluated and its value is prefixed with a single newline character.
 *
 * @property element The nested pattern element to render after the newline.
 */
data class Block(
    override val tokenRange: TokenRange, val element: PatternElement
) : PatternElement {
    override fun evaluate(context: EvaluationContext): String {
        return "\n${element.evaluate(context)}"
    }
}