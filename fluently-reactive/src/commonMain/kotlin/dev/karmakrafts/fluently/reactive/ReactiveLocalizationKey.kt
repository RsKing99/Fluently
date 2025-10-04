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

import dev.karmakrafts.fluently.eval.Function
import dev.karmakrafts.fluently.expr.Expr
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

@OptIn(ExperimentalCoroutinesApi::class)
internal data class ReactiveLocalizationKey( // @formatter:off
    val name: String,
    val attribName: String?,
    val functions: Map<String, Function>,
    val variables: Map<String, Expr>
) { // @formatter:on
    companion object {
        fun fromContext(name: String, context: ReactiveEvaluationContext): Flow<ReactiveLocalizationKey> {
            return context.asContextFlow().mapLatest { context ->
                ReactiveLocalizationKey(name, null, context.functions, context.variables)
            }
        }

        fun fromContext(
            name: String, attribName: String, context: ReactiveEvaluationContext
        ): Flow<ReactiveLocalizationKey> {
            return context.asContextFlow().mapLatest { context ->
                ReactiveLocalizationKey(name, attribName, context.functions, context.variables)
            }
        }
    }
}