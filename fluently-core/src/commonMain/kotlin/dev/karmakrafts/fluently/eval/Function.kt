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

package dev.karmakrafts.fluently.eval

import dev.karmakrafts.fluently.expr.Expr
import dev.karmakrafts.fluently.expr.ExprScope
import dev.karmakrafts.fluently.expr.ExprType

/**
 * Callback invoked to implement a Fluent function.
 *
 * It receives the current [EvaluationContext] and a map of evaluated argument expressions by
 * parameter name, and must return an [Expr] whose value will be evaluated by the caller.
 */
typealias FunctionCallback = ExprScope.(ctx: EvaluationContext, args: Map<String, Expr>) -> Expr

/**
 * A callable function available to expressions during evaluation.
 *
 * Functions may validate argument types (see [parameters]) and declare a [returnType] which
 * is used by type checking. The [callback] implements the function body.
 *
 * @property name The unique identifier of the function.
 * @property returnType The static [ExprType] returned by this function.
 * @property parameters Ordered parameter list as pairs of (name, type).
 * @property callback The implementation invoked when the function is called.
 */
data class Function(
    val name: String,
    val returnType: ExprType,
    val parameters: List<Pair<String, ExprType>>,
    val callback: FunctionCallback
)