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

package dev.karmakrafts.fluently.element

import dev.karmakrafts.fluently.eval.Evaluable
import dev.karmakrafts.fluently.util.TokenRangeProvider

/**
 * Base type for all traversable parts of a Fluent element tree.
 */
interface Element : Evaluable, TokenRangeProvider {
    /**
     * Reduces this element by traversing the underlying element tree
     * and aggregating the result using the given [ElementReducer] instance.
     *
     * @param R The type of the value produced by reducing this element
     *  using the given [ElementReducer].
     * @param reducer The [ElementReducer] instance which to apply to this element.
     * @return A value aggregated by the given [ElementReducer] instance.
     */
    fun <R> accept(reducer: ElementReducer<R>): R = reducer.visitElement(this)
}