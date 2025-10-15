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
 * Reference to a built-in or user-defined function call in an expression.
 *
 * Functions are resolved from [EvaluationContext.functions] by [name]. Positional and named arguments
 * are supported. Each argument is represented as a pair where the first component is an optional
 * parameter name and the second is the value expression.
 *
 * During evaluation, arguments are matched to the function's declared parameters in order, with
 * named arguments taking precedence and setting the current positional index accordingly. Argument
 * types are validated against the function signature using [Expr.getType].
 *
 * @property name The function identifier to invoke.
 * @property arguments The ordered list of call arguments; each pair is (parameterName?, valueExpr).
 */
data class FunctionReference( // @formatter:off
    override val tokenRange: TokenRange,
    val name: String,
    val arguments: List<Pair<String?, Expr>>
) : Expr { // @formatter:on
    override fun getType(context: EvaluationContext): ExprType {
        val function = context.functions[name] ?: throw FluentlyEvaluationException( // @formatter:off
            message = "No function named '$name'",
            tokenRange = tokenRange
        ) // @formatter:on
        return function.returnType
    }

    override fun evaluate(context: EvaluationContext): String {
        val function = context.functions[name] ?: throw FluentlyEvaluationException( // @formatter:off
            message = "No function named '$name'",
            tokenRange = tokenRange
        ) // @formatter:on
        val parameters = function.parameters
        val arguments = HashMap<String, Expr>()
        var currentArgIndex = 0
        for (argumentIndex in this.arguments.indices) {
            val (name, value) = this.arguments[argumentIndex]
            val valueType = value.getType(context)
            if (name != null) {
                val parameter =
                    parameters.find { (paramName, _) -> paramName == name } ?: throw FluentlyEvaluationException(
                        message = "No parameter named '$name' in call to function ${this.name}", tokenRange = tokenRange
                    )
                val (_, paramType) = parameter
                if (valueType != paramType) {
                    throw FluentlyEvaluationException(
                        message = "Expected argument of type $paramType for '$name' but got $valueType",
                        tokenRange = tokenRange
                    )
                }
                arguments[name] = value
                currentArgIndex = parameters.indexOf(parameter) + 1
                continue
            }
            val (paramName, paramType) = parameters.getOrNull(currentArgIndex) ?: throw FluentlyEvaluationException(
                message = "Could not match parameter $currentArgIndex for function ${this.name}",
                tokenRange = tokenRange
            )
            if (valueType != paramType) {
                throw FluentlyEvaluationException(
                    message = "Expected argument of type $paramType for '$name' but got $valueType",
                    tokenRange = tokenRange
                )
            }
            arguments[paramName] = value
            currentArgIndex++
        }
        return context.functions[name]?.callback?.invoke(DefaultExprScope, context, arguments)?.evaluate(context)
            ?: "<missing:${name}()>"
    }
}

fun ExprScope.functionReference(name: String, arguments: List<Pair<String?, Expr>>): FunctionReference =
    FunctionReference( // @formatter:off
        tokenRange = TokenRange.synthetic,
        name = name,
        arguments = arguments
    ) // @formatter:on