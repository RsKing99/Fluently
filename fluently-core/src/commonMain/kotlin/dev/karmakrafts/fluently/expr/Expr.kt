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

import dev.karmakrafts.fluently.element.PatternElement
import dev.karmakrafts.fluently.eval.EvaluationContext

/**
 * Base contract for all expression nodes in the Fluent AST.
 *
 * Implementations must be able to report their static [ExprType] within a given [EvaluationContext]
 * and can be evaluated through [PatternElement.evaluate]. Some higher-level nodes produced by parsing
 * may not be directly evaluable or typable until they are lowered (see [TermReference] for example).
 */
sealed interface Expr : PatternElement {
    /**
     * Returns the static type of this expression under the provided [context].
     *
     * Implementations may consult functions, variables, or bundle metadata available in the context
     * to determine their type. Some nodes that require lowering may throw if asked for a type too early.
     *
     * @param context The evaluation context providing access to functions, variables, and entries.
     * @return The inferred or declared [ExprType] of this expression.
     * @throws IllegalStateException If the expression cannot be typed prior to lowering.
     */
    fun getType(context: EvaluationContext): ExprType
}