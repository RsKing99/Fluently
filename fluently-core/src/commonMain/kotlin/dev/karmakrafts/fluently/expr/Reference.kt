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
import dev.karmakrafts.fluently.eval.FluentlyEvaluationException
import dev.karmakrafts.fluently.util.TokenRange

/**
 * Reference to a message, attribute, or variable within the current bundle or context.
 *
 * The [referenceType] determines how [name] (and optionally [attributeName]) are interpreted.
 * Message and attribute references are resolved from the [EvaluationContext.file], while variable
 * references are taken from [EvaluationContext.variables].
 *
 * @property referenceType The kind of target being referenced.
 * @property name The identifier of the message/term or variable being referenced.
 * @property attributeName The attribute name when [referenceType] is [Type.ATTRIBUTE]; otherwise `null`.
 */
data class Reference(
    override val tokenRange: TokenRange, val referenceType: Type, val name: String, val attributeName: String?
) : Expr {
    /** The supported kinds of references. */
    enum class Type { MESSAGE, ATTRIBUTE, VARIABLE }

    override fun getType(context: EvaluationContext): ExprType {
        return ExprType.STRING
    }

    override fun evaluate(context: EvaluationContext): String {
        return when (referenceType) {
            Type.MESSAGE -> {
                val message = context.file[name] ?: throw FluentlyEvaluationException( // @formatter:off
                    message = "No message named '$name'",
                    tokenRange = tokenRange
                ) // @formatter:on
                if (context.hasVisitedParent(message)) {
                    throw FluentlyEvaluationException(
                        message = "Message '$name' cannot reference itself (${context.getParentCycle()})",
                        tokenRange = tokenRange
                    )
                }
                message.evaluate(context)
            }

            Type.ATTRIBUTE -> {
                val attribute = context.file[name, attributeName!!] ?: throw FluentlyEvaluationException( // @formatter:off
                    message = "No attribute named '$name.$attributeName'",
                    tokenRange = tokenRange
                ) // @formatter:on
                if (context.hasVisitedParent(attribute)) {
                    throw FluentlyEvaluationException(
                        message = "Attribute '$name.$attributeName' cannot reference itself (${context.getParentCycle()})",
                        tokenRange = tokenRange
                    )
                }
                attribute.evaluate(context)
            }

            Type.VARIABLE -> context.variables[name]?.evaluate(context) ?: "<missing:$name>"
        }
    }
}

fun ExprScope.reference(type: Reference.Type, name: String, attributeName: String? = null): Reference = Reference( // @formatter:off
    tokenRange = TokenRange.synthetic,
    referenceType = type,
    name = name,
    attributeName = attributeName
) // @formatter:on