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

/**
 * A numeric literal expression.
 *
 * @property value The numeric value as parsed; may be an integer or floating-point number.
 */
data class NumberLiteral(
    override val tokenRange: TokenRange, val value: Number
) : Expr {
    /** True if [value] is a floating-point number. */
    inline val isDouble: Boolean
        get() = value is Double

    override fun getType(context: EvaluationContext): ExprType {
        return ExprType.NUMBER
    }

    override fun evaluate(context: EvaluationContext): String {
        return if (isDouble) value.toDouble().toString()
        else value.toLong().toString()
    }
}

fun ExprScope.number(value: Number): NumberLiteral = NumberLiteral(TokenRange.synthetic, value)