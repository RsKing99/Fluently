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
import dev.karmakrafts.fluently.util.TokenRange

/**
 * A public, addressable Fluent message entry.
 *
 * Messages are the primary entries formatted by consumers. They can contain pattern [elements]
 * and optional [attributes]. During evaluation, the message is pushed onto the parent stack to
 * enable cycle detection; attempting to reference itself directly or indirectly will result in
 * an error raised by the evaluation logic.
 *
 * @property name Unique identifier of this message.
 * @property elements Ordered pattern elements that compose the message value.
 * @property attributes Named attributes available on this message; empty if none.
 */
data class Message(
    override val tokenRange: TokenRange,
    override val name: String,
    override val elements: List<PatternElement>,
    override val attributes: Map<String, Attribute>
) : LocalizationEntry {
    /**
     * Evaluates the message value under [context] with cycle-detection bookkeeping.
     *
     * The message is pushed to [EvaluationContext.parentStack] before delegating to the base
     * implementation and popped afterward.
     */
    override fun evaluate(context: EvaluationContext): String {
        context.pushParent(this)
        val result = super.evaluate(context)
        context.popParent()
        return result
    }
}