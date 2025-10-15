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
import dev.karmakrafts.fluently.expr.DefaultExprScope
import dev.karmakrafts.fluently.expr.Expr
import dev.karmakrafts.fluently.expr.ExprScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.js.JsName
import kotlin.jvm.JvmName

/**
 * Builder DSL for constructing a ReactiveEvaluationContext.
 *
 * This builder mirrors EvaluationContextBuilder but accepts reactive inputs. Use the
 * various variable helpers to supply static or Flow-based values and [function] to
 * register Fluent functions. Finally, call [build] (usually via [reactiveEvaluationContext])
 * to obtain a ReactiveEvaluationContext that can be turned into a Flow of
 * EvaluationContext snapshots through [ReactiveEvaluationContext.asContextFlow].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReactiveEvaluationContextBuilder @PublishedApi internal constructor() : ExprScope by DefaultExprScope {
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
    @JsName("variablesReactive")
    @JvmName("variablesReactive")
    fun variables(variables: Map<String, Flow<Expr>>) {
        this.variables += variables
    }

    /** Adds the given expression (literal) as a value under [name]. */
    fun variable(name: String, value: Expr) {
        variables[name] = flowOf(value)
    }

    /** Adds the given expression (literal) as a value under [name]. */
    fun variable(name: String, value: Flow<Expr>) {
        variables[name] = value
    }

    /** Registers a function named [name] using a [FunctionBuilder] DSL. */
    inline fun function(name: String, block: FunctionBuilder.() -> Unit) {
        functions[name] = FunctionBuilder(name).apply(block).build()
    }

    /**
     * Builds a ReactiveEvaluationContext for the provided [file].
     *
     * The resulting context can be transformed into a Flow of immutable EvaluationContext
     * snapshots via [ReactiveEvaluationContext.asContextFlow]. Each emission from [file]
     * or any declared reactive variable will trigger a new snapshot.
     */
    fun build(file: Flow<LocalizationFile>): ReactiveEvaluationContext = ReactiveEvaluationContext( // @formatter:off
        file = file,
        functions = functions,
        variables = variables
    ) // @formatter:on
}

/**
 * Specification block (DSL) used by [ReactiveEvaluationContextBuilder].
 *
 * This typealias represents the builder lambda that configures reactive variables and
 * functions before creating a [ReactiveEvaluationContext].
 */
typealias ReactiveEvaluationContextSpec = ReactiveEvaluationContextBuilder.() -> Unit

/**
 * Creates a ReactiveEvaluationContext for the given [file] using a builder [spec].
 *
 * This is a convenience for `ReactiveEvaluationContextBuilder().apply(spec).build(file)`.
 * Typical usage is to bind [file] and variables to reactive sources so evaluations update
 * automatically when inputs change.
 */
inline fun reactiveEvaluationContext( // @formatter:off
    file: Flow<LocalizationFile>,
    spec: ReactiveEvaluationContextSpec
): ReactiveEvaluationContext { // @formatter:on
    return ReactiveEvaluationContextBuilder().apply(spec).build(file)
}

/**
 * Converts a non-reactive [EvaluationContext] into a [ReactiveEvaluationContext].
 *
 * The returned context wraps the static [EvaluationContext.file] and [EvaluationContext.variables]
 * into single-emission Flows. This is useful when integrating existing code with reactive APIs.
 * Note that the resulting flows will not update unless you rebuild the underlying
 * EvaluationContext and call this function again.
 */
fun EvaluationContext.asReactive(): ReactiveEvaluationContext {
    return ReactiveEvaluationContext(flowOf(file), functions, variables.mapValues { (_, value) -> flowOf(value) })
}