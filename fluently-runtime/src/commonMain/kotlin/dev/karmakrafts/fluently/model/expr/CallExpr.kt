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

package dev.karmakrafts.fluently.model.expr

import dev.karmakrafts.fluently.model.EvaluationContext

data class CallExpr( // @formatter:off
    val name: String,
    val arguments: List<Pair<String?, Expr>>
) : Expr { // @formatter:on
    override fun getType(context: EvaluationContext): ExprType {
        val function = context.functions[name] ?: error("No function named '$name'")
        return function.returnType
    }

    override fun evaluate(context: EvaluationContext): String {
        val function = context.functions[name] ?: error("No function named '$name'")
        val parameters = function.parameters
        val arguments = HashMap<String, Expr>()
        var currentArgIndex = 0
        for (argumentIndex in this.arguments.indices) {
            val (name, value) = this.arguments[argumentIndex]
            val valueType = value.getType(context)
            if (name != null) {
                val parameter = parameters.first { (paramName, _) -> paramName == name }
                val (_, paramType) = parameter
                check(valueType == paramType) { "Expected argument of type $paramType for '$name' but got $valueType" }
                arguments[name] = value
                currentArgIndex = parameters.indexOf(parameter)
                continue
            }
            val (paramName, paramType) = parameters.getOrNull(currentArgIndex)
                ?: error("Could not match parameter $currentArgIndex for function ${this.name}")
            check(valueType == paramType) { "Expected argument of type $paramType for '$name' but got $valueType" }
            arguments[paramName] = value
            currentArgIndex++
        }
        return context.functions[name]?.callback?.invoke(context, arguments)?.evaluate(context) ?: "<missing:${name}()>"
    }
}