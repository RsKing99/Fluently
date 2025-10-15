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

import dev.karmakrafts.fluently.entry.LocalizationEntry
import dev.karmakrafts.fluently.entry.Message
import dev.karmakrafts.fluently.entry.Term
import dev.karmakrafts.fluently.expr.CompoundExpr
import dev.karmakrafts.fluently.expr.Expr
import dev.karmakrafts.fluently.expr.FunctionReference
import dev.karmakrafts.fluently.expr.NumberLiteral
import dev.karmakrafts.fluently.expr.Reference
import dev.karmakrafts.fluently.expr.SelectExpr
import dev.karmakrafts.fluently.expr.StringLiteral
import dev.karmakrafts.fluently.expr.TermReference

/**
 * A generic visitor-style reducer for the Fluent AST elements.
 *
 * Implementations walk over elements of messages, terms, attributes, and expressions,
 * producing a single accumulated value of type [T]. The reduction is driven by:
 * - [createDefaultValue] to provide a neutral element for leaf nodes.
 * - [aggregate] to combine results from child nodes.
 *
 * Default implementations traverse the tree and delegate to more specific visit methods.
 * Override only the methods you need for your use case.
 *
 * @param T the result type produced by the reduction.
 */
interface ElementReducer<T> {
    /**
     * Provides a neutral/identity value for this reducer.
     *
     * This value is used for leaf nodes or cases where a node contributes nothing
     * to the aggregate. For example, a numeric reducer might return 0, and a list
     * reducer might return an empty list.
     */
    fun createDefaultValue(): T

    /**
     * Combines two partial results into one.
     *
     * @param aggregate the running aggregate up to this point.
     * @param value the next value to fold into the aggregate.
     * @return the combined aggregate.
     */
    fun aggregate(aggregate: T, value: T): T

    /**
     * Dispatches reduction based on the concrete [Element] subtype.
     *
     * @param element the element to reduce.
     * @return the reduced value for the given element.
     */
    fun visitElement(element: Element): T = when (element) {
        is Attribute -> visitAttribute(element)
        is PatternElement -> visitPatternElement(element)
        is LocalizationEntry -> visitLocalizationEntry(element)
        else -> error("Unsupported element type: ${element::class}")
    }

    /**
     * Dispatches reduction based on the concrete [PatternElement] subtype.
     */
    fun visitPatternElement(element: PatternElement): T = when (element) {
        is Block -> visitBlock(element)
        is Text -> visitText(element)
        is Expr -> visitExpr(element)
        else -> error("Unsupported pattern element type: ${element::class}")
    }

    /**
     * Reduces a [Block] by visiting its inner [PatternElement].
     */
    fun visitBlock(block: Block): T = visitPatternElement(block.element)

    /**
     * Reduces a text literal. By default returns [createDefaultValue].
     */
    fun visitText(text: Text): T = createDefaultValue()

    /**
     * Reduces an [Attribute] by reducing and aggregating its pattern elements.
     */
    fun visitAttribute(attribute: Attribute): T {
        return attribute.elements.map(::visitPatternElement).reduce(::aggregate)
    }

    /**
     * Dispatches reduction based on the concrete [LocalizationEntry] subtype.
     */
    fun visitLocalizationEntry(entry: LocalizationEntry): T = when (entry) {
        is Term -> visitTerm(entry)
        is Message -> visitMessage(entry)
    }

    /**
     * Reduces a [Message] by reducing its pattern elements and attributes and aggregating the results.
     */
    fun visitMessage(message: Message): T { // @formatter:off
        return (message.elements.map(::visitPatternElement) + message.attributes.values.map(::visitAttribute))
            .reduce(::aggregate)
    } // @formatter:on

    /**
     * Reduces a [Term] by reducing its pattern elements and attributes and aggregating the results.
     */
    fun visitTerm(term: Term): T { // @formatter:off
        return (term.elements.map(::visitPatternElement) + term.attributes.values.map(::visitAttribute))
            .reduce(::aggregate)
    } // @formatter:on

    /**
     * Dispatches reduction based on the concrete [Expr] subtype.
     */
    fun visitExpr(expr: Expr): T = when (expr) {
        is CompoundExpr -> visitCompoundExpr(expr)
        is NumberLiteral -> visitNumberLiteral(expr)
        is StringLiteral -> visitStringLiteral(expr)
        is SelectExpr -> visitSelectExpr(expr)
        is FunctionReference -> visitFunctionReference(expr)
        is Reference -> visitReference(expr)
        is TermReference -> visitTermReference(expr)
    }

    /**
     * Reduces a [TermReference] by reducing and aggregating its argument expressions.
     */
    fun visitTermReference(reference: TermReference): T {
        return reference.arguments.values.map(::visitExpr).reduce(::aggregate)
    }

    /**
     * Reduces a variable/message reference. By default returns [createDefaultValue].
     */
    fun visitReference(reference: Reference): T = createDefaultValue()

    /**
     * Reduces a [SelectExpr] by reducing its selector, keys, and all variant bodies, aggregating everything.
     */
    fun visitSelectExpr(expr: SelectExpr): T {
        val defaultVariant = expr.defaultVariant
        val variants = expr.variants.values
        // @formatter:off
        return (listOf(visitExpr(expr.variable))
            + visitExpr(defaultVariant.key)
            + defaultVariant.elements.map(::visitPatternElement)
            + variants.flatMap { variant ->
                listOf(visitExpr(variant.key)) + variant.elements.map(::visitPatternElement)
            }).reduce(::aggregate)
        // @formatter:on
    }

    /**
     * Reduces a [CompoundExpr] by reducing its constituent pattern elements and aggregating them.
     */
    fun visitCompoundExpr(expr: CompoundExpr): T {
        return expr.elements.map(::visitPatternElement).reduce(::aggregate)
    }

    /**
     * Reduces a [FunctionReference] by reducing its argument expressions and aggregating them.
     */
    fun visitFunctionReference(reference: FunctionReference): T {
        return reference.arguments.map { (_, expr) -> visitExpr(expr) }.reduce(::aggregate)
    }

    /**
     * Reduces a numeric literal. By default returns [createDefaultValue].
     */
    fun visitNumberLiteral(literal: NumberLiteral): T = createDefaultValue()

    /**
     * Reduces a string literal. By default returns [createDefaultValue].
     */
    fun visitStringLiteral(literal: StringLiteral): T = createDefaultValue()
}