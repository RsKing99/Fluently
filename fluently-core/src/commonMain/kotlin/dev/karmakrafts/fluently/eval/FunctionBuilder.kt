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

import dev.karmakrafts.fluently.expr.ExprType
import dev.karmakrafts.fluently.expr.StringLiteral

/**
 * DSL builder for declaring a [Function].
 *
 * Instances are created by [EvaluationContextBuilder.function] and configured by setting [returnType],
 * adding [parameter]s, and providing an [action] (the callback body).
 */
class FunctionBuilder @PublishedApi internal constructor(private val name: String) {
    /** The function's static return type. Defaults to [ExprType.STRING]. */
    var returnType: ExprType = ExprType.STRING
    private val parameters: ArrayList<Pair<String, ExprType>> = ArrayList()
    internal var callback: FunctionCallback = { _, _ -> StringLiteral("") }

    /** Adds a parameter with [name] and expected [type]. */
    fun parameter(name: String, type: ExprType) {
        parameters += name to type
    }

    /** Sets the function implementation [callback]. */
    fun action(callback: FunctionCallback) {
        this.callback = callback
    }

    /** Builds the immutable [Function] instance. */
    @PublishedApi
    internal fun build(): Function = Function( // @formatter:off
        name = name,
        returnType = returnType,
        parameters = parameters,
        callback = callback
    ) // @formatter:on
}