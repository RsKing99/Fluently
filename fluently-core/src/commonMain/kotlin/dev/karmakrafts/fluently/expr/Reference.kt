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

data class Reference(val referenceType: Type, val name: String, val attributeName: String?) : Expr {
    enum class Type { MESSAGE, ATTRIBUTE, VARIABLE }

    override fun getType(context: EvaluationContext): ExprType {
        return ExprType.STRING
    }

    override fun evaluate(context: EvaluationContext): String {
        return when (referenceType) {
            Type.MESSAGE -> {
                val message = context.file[name] ?: error("No message named '$name'")
                check(!context.hasVisitedParent(message)) {
                    "Message '$name' cannot reference itself (${context.getParentCycle()})"
                }
                message.evaluate(context)
            }

            Type.ATTRIBUTE -> {
                val attribute =
                    context.file[name, attributeName!!] ?: error("No attribute named '$name.$attributeName'")
                check(!context.hasVisitedParent(attribute)) {
                    "Attribute '$name.$attributeName' cannot reference itself (${context.getParentCycle()})"
                }
                attribute.evaluate(context)
            }

            Type.VARIABLE -> context.variables[name]?.evaluate(context) ?: "<missing:$name>"
        }
    }
}