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
import dev.karmakrafts.fluently.eval.evaluationContext
import dev.karmakrafts.fluently.expr.Expr
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Reactive wrapper for building EvaluationContext instances as a Flow.
 *
 * This type models a context whose constituents can change over time and thus exposes
 * a Flow<EvaluationContext> via [asContextFlow]. When any of the inputs emit a new value,
 * a new EvaluationContext is produced that reflects the latest state.
 *
 * Typical usage is to bind the [file] and [variables] to reactive data sources (e.g. UI
 * state or preference flows) so that message evaluation automatically reacts to changes.
 *
 * @property file A flow of the current LocalizationFile that supplies messages and terms.
 *                Each emission triggers recomputation of the resulting EvaluationContext.
 * @property functions A static map of available functions by name to be included in each context.
 * @property variables Reactive variables available to expressions, provided as a map from name
 *                     to a Flow of Expr values. All variable flows are combined so that any
 *                     emission from any variable recomputes the context with the latest values.
 */
@OptIn(ExperimentalCoroutinesApi::class)
data class ReactiveEvaluationContext( // @formatter:off
    val file: Flow<LocalizationFile>,
    val functions: Map<String, Function>,
    val variables: Map<String, Flow<Expr>>
) { // @formatter:on
    /**
     * Produces a Flow of immutable EvaluationContext snapshots based on the latest inputs.
     *
     * Behavior:
     * - When there are no [variables], the result simply maps [file] to an EvaluationContext.
     * - Otherwise it combines all variable flows; any variable emission recomputes the context.
     * - The [functions] map is reused for every context emission.
     */
    fun asContextFlow(): Flow<EvaluationContext> {
        return file.flatMapLatest { file ->
            val variableValues = variables.values.toList()
            if (variableValues.isEmpty()) {
                return@flatMapLatest flowOf(evaluationContext(file) {
                    functions(functions)
                })
            }
            val variableNames = variables.keys.toList()
            combine(variableValues) { values ->
                evaluationContext(file) {
                    functions(functions)
                    variables(variableNames.mapIndexed { index, name -> name to values[index] }.toMap())
                }
            }
        }
    }

    /**
     * Returns a new context that combines this context with [other].
     *
     * The resulting context:
     * - uses this [file],
     * - merges [functions] with [other]'s functions (values from [other] override on key collision),
     * - merges [variables] with [other]'s variables (values from [other] override on key collision),
     */
    operator fun plus(other: ReactiveEvaluationContext): ReactiveEvaluationContext {
        return copy( // @formatter:off
            functions = functions + other.functions,
            variables = variables + other.variables
        ) // @formatter:on
    }

    /**
     * Creates a new context with [functions] overlaid on top of the current set.
     * If a function with the same name exists, the provided one takes precedence.
     */
    fun overlayFunctions(functions: Map<String, Function>): ReactiveEvaluationContext {
        return copy(functions = this.functions + functions)
    }

    /**
     * Creates a new context with [variables] overlaid on top of the current set.
     * If a variable with the same name exists, the provided one takes precedence.
     */
    fun overlayVariables(variables: Map<String, Flow<Expr>>): ReactiveEvaluationContext {
        return copy(variables = this.variables + variables)
    }
}