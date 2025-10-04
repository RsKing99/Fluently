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

package dev.karmakrafts.fluently.reactive

import dev.karmakrafts.fluently.LocalizationFile
import dev.karmakrafts.fluently.eval.EvaluationContext
import dev.karmakrafts.fluently.eval.Function
import dev.karmakrafts.fluently.eval.FunctionBuilder
import dev.karmakrafts.fluently.expr.Expr
import dev.karmakrafts.fluently.expr.NumberLiteral
import dev.karmakrafts.fluently.expr.StringLiteral
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class ReactiveEvaluationContextBuilder {
    @PublishedApi
    internal val functions: HashMap<String, Function> = HashMap()
    private val variables: HashMap<String, Flow<Expr>> = HashMap()

    /** Adds all given functions to the function map of this builder instance. */
    fun functions(functions: Map<String, Function>) {
        this.functions += functions
    }

    /** Adds all given variables to the variable map of this builder instance. */
    fun variables(variables: Map<String, Expr>) {
        this.variables += variables.mapValues { (_, value) -> flowOf(value) }
    }

    /** Adds all given variables to the variable map of this builder instance. */
    fun reactiveVariables(variables: Map<String, Flow<Expr>>) {
        this.variables += variables
    }

    /** Adds the given expression (literal) as a value under [name]. */
    fun variable(name: String, value: Expr) {
        variables[name] = flowOf(value)
    }

    /** Adds the given expression (literal) as a value under [name]. */
    fun reactiveVariable(name: String, value: Flow<Expr>) {
        variables[name] = value
    }

    /** Adds an enum [value] as a lowercase string variable under [name]. */
    fun enumVariable(name: String, value: Enum<*>) {
        variables[name] = flowOf(StringLiteral(value.name.lowercase()))
    }

    /** Adds an enum [value] as a lowercase string variable under [name]. */
    fun reactiveEnumVariable(name: String, value: Flow<Enum<*>>) {
        variables[name] = value.mapLatest { enum -> StringLiteral(enum.name.lowercase()) }
    }

    /** Adds a string [value] variable under [name]. */
    fun stringVariable(name: String, value: String) {
        variables[name] = flowOf(StringLiteral(value))
    }

    /** Adds a string [value] variable under [name]. */
    fun reactiveStringVariable(name: String, value: Flow<String>) {
        variables[name] = value.mapLatest { string -> StringLiteral(string) }
    }

    /** Adds a boolean [value] variable under [name] (stored as a string). */
    fun boolVariable(name: String, value: Boolean) {
        variables[name] = flowOf(StringLiteral(value.toString()))
    }

    /** Adds a boolean [value] variable under [name] (stored as a string). */
    fun reactiveBoolVariable(name: String, value: Flow<Boolean>) {
        variables[name] = value.mapLatest { bool -> StringLiteral(bool.toString()) }
    }

    /** Adds a numeric [value] variable under [name]. */
    fun numberVariable(name: String, value: Number) {
        variables[name] = flowOf(NumberLiteral(value))
    }

    /** Adds a numeric [value] variable under [name]. */
    fun reactiveNumberVariable(name: String, value: Flow<Number>) {
        variables[name] = value.mapLatest { number -> NumberLiteral(number) }
    }

    /** Registers a function named [name] using a [FunctionBuilder] DSL. */
    inline fun function(name: String, block: FunctionBuilder.() -> Unit) {
        functions[name] = FunctionBuilder(name).apply(block).build()
    }

    fun build(file: Flow<LocalizationFile>): ReactiveEvaluationContext = ReactiveEvaluationContext( // @formatter:off
        file = file,
        functions = functions,
        variables = variables
    ) // @formatter:on
}

typealias ReactiveEvaluationContextSpec = ReactiveEvaluationContextBuilder.() -> Unit

inline fun reactiveEvaluationContext( // @formatter:off
    file: Flow<LocalizationFile>,
    spec: ReactiveEvaluationContextSpec
): ReactiveEvaluationContext { // @formatter:on
    return ReactiveEvaluationContextBuilder().apply(spec).build(file)
}

fun EvaluationContext.asReactive(): ReactiveEvaluationContext {
    return ReactiveEvaluationContext(flowOf(file), functions, variables.mapValues { (_, value) -> flowOf(value) })
}