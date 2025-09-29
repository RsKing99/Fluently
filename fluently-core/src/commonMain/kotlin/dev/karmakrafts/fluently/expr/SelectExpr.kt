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

import dev.karmakrafts.fluently.EvaluationContext
import dev.karmakrafts.fluently.element.PatternElement

data class SelectExpr(val variable: Expr, val variants: Map<Expr, Variant>) : Expr {
    data class Variant(val key: Expr, val elements: List<PatternElement>, val isDefault: Boolean = false)

    inline val defaultVariant: Variant
        get() = variants.values.first { variant -> variant.isDefault }

    override fun getType(context: EvaluationContext): ExprType {
        return ExprType.STRING
    }

    override fun evaluate(context: EvaluationContext): String {
        val value = variable.evaluate(context)
        for ((key, variant) in variants) {
            val keyValue = key.evaluate(context)
            if (keyValue != value) continue
            return variant.elements.joinToString("") { element -> element.evaluate(context) }
        }
        return defaultVariant.elements.joinToString("") { element -> element.evaluate(context) }
    }
}