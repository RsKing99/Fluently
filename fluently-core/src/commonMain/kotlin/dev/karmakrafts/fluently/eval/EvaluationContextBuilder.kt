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

import dev.karmakrafts.fluently.LocalizationFile
import dev.karmakrafts.fluently.expr.Expr
import dev.karmakrafts.fluently.expr.NumberLiteral
import dev.karmakrafts.fluently.expr.StringLiteral

class EvaluationContextBuilder @PublishedApi internal constructor() {
    @PublishedApi
    internal val functions: HashMap<String, Function> = HashMap()

    @PublishedApi
    internal val variables: HashMap<String, Expr> = HashMap()

    fun variable(name: String, value: Enum<*>) {
        variables[name] = StringLiteral(value.name.lowercase())
    }

    fun variable(name: String, value: String) {
        variables[name] = StringLiteral(value)
    }

    fun variable(name: String, value: Boolean) {
        variables[name] = StringLiteral(value.toString())
    }

    fun variable(name: String, value: Number) {
        variables[name] = NumberLiteral(value)
    }

    inline fun function(name: String, block: FunctionBuilder.() -> Unit) {
        functions[name] = FunctionBuilder(name).apply(block).build()
    }

    @PublishedApi
    internal fun build(file: LocalizationFile): EvaluationContext = EvaluationContext( // @formatter:off
        file = file,
        functions = functions,
        variables = variables
    ) // @formatter:on
}