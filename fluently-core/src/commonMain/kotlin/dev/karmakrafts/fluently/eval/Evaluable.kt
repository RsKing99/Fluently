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

/**
 * Something that can be formatted to a string under a given [EvaluationContext].
 *
 * This is the common contract for messages, terms, attributes, and expression nodes. Implementations
 * should be pure with respect to the provided [EvaluationContext] and not rely on global state.
 */
interface Evaluable {
    /**
     * Formats this element to a string using the provided [context].
     *
     * @param context The evaluation context providing access to the current file, variables and functions.
     * @return The formatted string result.
     */
    fun evaluate(context: EvaluationContext): String
}

/**
 * Convenience overload that evaluates this [Evaluable] by constructing an [EvaluationContext]
 * for the given [file].
 *
 * The optional [contextInit] lambda may customize variables and functions using
 * [EvaluationContextBuilder] prior to building the context.
 *
 * Example:
 * - message.evaluate(file) { variable("name", "Alice") }
 *
 * @param file The localization file (bundle) providing entries available during evaluation.
 * @param contextInit Optional builder to configure variables and functions.
 * @return The formatted string result.
 */
inline fun Evaluable.evaluate( // @formatter:off
    file: LocalizationFile,
    contextInit: EvaluationContextBuilder.() -> Unit = {}
): String { // @formatter:on
    return evaluate(EvaluationContextBuilder().apply(contextInit).build(file))
}