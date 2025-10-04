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

@OptIn(ExperimentalCoroutinesApi::class)
data class ReactiveEvaluationContext( // @formatter:off
    val file: Flow<LocalizationFile>,
    val functions: Map<String, Function>,
    val variables: Map<String, Flow<Expr>>
) { // @formatter:on
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
}