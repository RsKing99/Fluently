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