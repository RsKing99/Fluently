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
import dev.karmakrafts.fluently.element.Attribute
import dev.karmakrafts.fluently.expr.Expr
import dev.karmakrafts.fluently.util.Named

/**
 * Immutable context object used during evaluation and type checking.
 *
 * It provides access to the current [file] being formatted, registered [functions], and
 * runtime [variables]. It also carries a [parentStack] used for cycle detection when entries
 * reference each other.
 *
 * Instances are typically created via [EvaluationContextBuilder].
 *
 * @property file The localization file containing messages and terms to resolve references.
 * @property functions The set of available functions indexed by name.
 * @property variables Variables available to expressions (strings or numbers), indexed by name.
 * @property parentStack Internal stack of currently-evaluating parents used for cycle detection.
 */
@ConsistentCopyVisibility
data class EvaluationContext @PublishedApi internal constructor(
    val file: LocalizationFile,
    val functions: Map<String, Function>,
    val variables: Map<String, Expr>,
    val parentStack: ArrayDeque<Named> = ArrayDeque() // Used for multi-level cycle detection
) {
    /**
     * Returns a new context that combines this context with [other].
     *
     * The resulting context:
     * - uses this [file],
     * - merges [functions] with [other]'s functions (values from [other] override on key collision),
     * - merges [variables] with [other]'s variables (values from [other] override on key collision),
     * - reuses the same [parentStack] reference for cycle detection.
     */
    operator fun plus(other: EvaluationContext): EvaluationContext {
        return copy( // @formatter:off
            functions = functions + other.functions,
            variables = variables + other.variables
        ) // @formatter:on
    }

    /**
     * Creates a new context with [functions] overlaid on top of the current set.
     * If a function with the same name exists, the provided one takes precedence.
     */
    fun overlayFunctions(functions: Map<String, Function>): EvaluationContext {
        return copy(functions = this.functions + functions)
    }

    /**
     * Creates a new context with [variables] overlaid on top of the current set.
     * If a variable with the same name exists, the provided one takes precedence.
     */
    fun overlayVariables(variables: Map<String, Expr>): EvaluationContext {
        return copy(variables = this.variables + variables)
    }

    /** Returns a string representing the cycle path when a self-reference is detected. */
    fun getParentCycle(): String = (parentStack.toList() + parentStack.first()) //
        .joinToString(" -> ") { element ->
            when (element) {
                is Attribute -> "${element.entryName}.${element.name}"
                else -> element.name
            }
        }

    /** True if [element] is currently present in [parentStack]. */
    fun hasVisitedParent(element: Named): Boolean = element in parentStack

    /** Pushes a new parent element onto the cycle-detection stack. */
    fun pushParent(element: Named) {
        parentStack.addFirst(element)
    }

    /** Pops the most recent parent element from the cycle-detection stack. */
    fun popParent() {
        parentStack.removeFirst()
    }
}