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

/**
 * Builder DSL for constructing an [EvaluationContext].
 *
 * Use [variable] helpers to declare runtime variables and [function] to register Fluent functions
 * before calling the internal [build] method via the extension [Evaluable.evaluate].
 */
class EvaluationContextBuilder @PublishedApi internal constructor() {
    @PublishedApi
    internal val functions: HashMap<String, Function> = HashMap()

    @PublishedApi
    internal val variables: HashMap<String, Expr> = HashMap()

    /** Adds all given functions to the function map of this builder instance. */
    fun functions(functions: Map<String, Function>) {
        this.functions += functions
    }

    /** Adds all given variables to the variable map of this builder instance. */
    fun variables(variables: Map<String, Expr>) {
        this.variables += variables
    }

    /** Adds an enum [value] as a lowercase string variable under [name]. */
    fun variable(name: String, value: Enum<*>) {
        variables[name] = StringLiteral(value.name.lowercase())
    }

    /** Adds a string [value] variable under [name]. */
    fun variable(name: String, value: String) {
        variables[name] = StringLiteral(value)
    }

    /** Adds a boolean [value] variable under [name] (stored as a string). */
    fun variable(name: String, value: Boolean) {
        variables[name] = StringLiteral(value.toString())
    }

    /** Adds a numeric [value] variable under [name]. */
    fun variable(name: String, value: Number) {
        variables[name] = NumberLiteral(value)
    }

    /** Registers a function named [name] using a [FunctionBuilder] DSL. */
    inline fun function(name: String, block: FunctionBuilder.() -> Unit) {
        functions[name] = FunctionBuilder(name).apply(block).build()
    }

    /**
     * Builds an [EvaluationContext] for the provided [file].
     *
     * Normally not called directly; prefer [Evaluable.evaluate]. Marked [PublishedApi] to make it
     * accessible to inline call sites while keeping the constructor internal.
     */
    @PublishedApi
    internal fun build(file: LocalizationFile): EvaluationContext = EvaluationContext( // @formatter:off
        file = file,
        functions = functions,
        variables = variables
    ) // @formatter:on
}

/**
 * Specification block (DSL) used by [EvaluationContextBuilder].
 *
 * This typealias represents the builder lambda that configures variables and functions
 * before creating an [EvaluationContext].
 */
typealias EvaluationContextSpec = EvaluationContextBuilder.() -> Unit

/**
 * Creates an [EvaluationContext] for the given [file] using a builder [spec].
 *
 * This is a convenience for `EvaluationContextBuilder().apply(spec).build(file)`.
 * Typical usage is to pass variables and functions needed by message evaluation.
 */
inline fun evaluationContext(file: LocalizationFile, spec: EvaluationContextSpec): EvaluationContext {
    return EvaluationContextBuilder().apply(spec).build(file)
}