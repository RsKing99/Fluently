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
import dev.karmakrafts.fluently.util.TokenRange

/** A string literal expression. */
data class StringLiteral(
    override val tokenRange: TokenRange, val value: String
) : Expr {
    override fun getType(context: EvaluationContext): ExprType = ExprType.STRING
    override fun evaluate(context: EvaluationContext): String = value
}

fun ExprScope.string(value: String): StringLiteral = StringLiteral(
    tokenRange = TokenRange.synthetic, value = value
)