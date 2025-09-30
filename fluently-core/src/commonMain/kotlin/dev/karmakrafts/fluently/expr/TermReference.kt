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

package dev.karmakrafts.fluently.expr

import dev.karmakrafts.fluently.eval.EvaluationContext

/**
 * Reference to a Fluent term in an expression tree.
 *
 * A term is a private reusable entry whose identifier starts with a dash in Fluent source (for example, `-brand-name`).
 * This node may optionally address a specific attribute of the term and may carry named arguments.
 *
 * This is a high-level AST node produced by parsing; it must be lowered to a concrete, resolved expression before
 * type inference or evaluation. Until then, [getType] and [evaluate] will throw an error.
 *
 * @property entryName The name of the target term (without the leading dash).
 * @property attribName The name of the attribute addressed on the term, or `null` to reference the term's value.
 * @property arguments Named arguments to pass to the term when it is formatted; empty if none.
 */
data class TermReference(
    val entryName: String, val attribName: String?, val arguments: Map<String, Expr>
) : Expr {
    /**
     * Indicates whether this reference supplies any arguments to the term.
     */
    inline val isParametrized: Boolean
        get() = arguments.isNotEmpty()

    override fun getType(context: EvaluationContext): ExprType {
        error("Term reference hasn't been lowered")
    }

    override fun evaluate(context: EvaluationContext): String {
        error("Term reference hasn't been lowered")
    }
}