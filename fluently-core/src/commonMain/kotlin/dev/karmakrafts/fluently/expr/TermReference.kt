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

package dev.karmakrafts.fluently.expr

import dev.karmakrafts.fluently.eval.EvaluationContext
import dev.karmakrafts.fluently.eval.FluentlyEvaluationException
import dev.karmakrafts.fluently.util.TokenRange

/**
 * Represents a reference to a Fluent term within an expression tree.
 *
 * A term reference points to a term defined in the current localization file and
 * may optionally reference a specific attribute of that term. It can also be
 * parameterized with a map of argument expressions that are evaluated and
 * provided to the referenced term (or attribute) during evaluation.
 */
data class TermReference( // @formatter:off
    override val tokenRange: TokenRange,
    val entryName: String,
    val attribName: String?,
    val arguments: Map<String, Expr>
) : Expr { // @formatter:on
    inline val isParametrized: Boolean
        get() = arguments.isNotEmpty()

    override fun getType(context: EvaluationContext): ExprType {
        return ExprType.STRING
    }

    override fun evaluate(context: EvaluationContext): String {
        val term = context.file.terms().find { term -> term.name == entryName } ?: throw FluentlyEvaluationException( // @formatter:off
            message = "No term named '$entryName'",
            tokenRange = tokenRange
        ) // @formatter:on
        // This is a term reference
        if(attribName == null) { // @formatter:off
            if(context.hasVisitedParent(term)) {
                throw FluentlyEvaluationException( // @formatter:off
                    message = "Term '$term' cannot reference itself (${context.getParentCycle()})",
                    tokenRange = tokenRange
                ) // @formatter:on
            }
            return term.evaluate(context.overlayVariables(arguments))
        } // @formatter:on
        // Otherwise this is a reference to a term attribute
        val attribute = term.attributes[attribName] ?: throw FluentlyEvaluationException( // @formatter:off
            message = "No term attribute named '$attribName' on term '$entryName'",
            tokenRange = tokenRange
        ) // @formatter:on
        if (context.hasVisitedParent(attribute)) {
            throw FluentlyEvaluationException( // @formatter:off
                message = "Attribute '$entryName.$attribName' cannot reference itself (${context.getParentCycle()})",
                tokenRange = tokenRange
            ) // @formatter:on
        }
        return attribute.evaluate(context.overlayVariables(arguments))
    }
}

fun ExprScope.termReference(
    entryName: String, attribName: String? = null, arguments: Map<String, Expr> = emptyMap()
): TermReference = TermReference(
    tokenRange = TokenRange.synthetic, entryName = entryName, attribName = attribName, arguments = arguments
)