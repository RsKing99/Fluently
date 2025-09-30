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

package dev.karmakrafts.fluently.parser

import dev.karmakrafts.fluently.element.Attribute
import dev.karmakrafts.fluently.element.PatternElement
import dev.karmakrafts.fluently.entry.Term
import dev.karmakrafts.fluently.eval.Evaluable
import dev.karmakrafts.fluently.expr.CompoundExpr
import dev.karmakrafts.fluently.expr.Expr
import dev.karmakrafts.fluently.expr.FunctionReference
import dev.karmakrafts.fluently.expr.NumberLiteral
import dev.karmakrafts.fluently.expr.Reference
import dev.karmakrafts.fluently.expr.SelectExpr
import dev.karmakrafts.fluently.expr.StringLiteral
import dev.karmakrafts.fluently.expr.TermReference
import dev.karmakrafts.fluently.frontend.FluentLexer
import dev.karmakrafts.fluently.frontend.FluentParser
import dev.karmakrafts.fluently.frontend.FluentParserBaseVisitor
import org.antlr.v4.kotlinruntime.tree.TerminalNode

class ExprParser(
    val context: ParserContext
) : FluentParserBaseVisitor<List<Expr>>() {
    override fun defaultResult(): List<Expr> = ArrayList()

    override fun aggregateResult( // @formatter:off
        aggregate: List<Expr>,
        nextResult: List<Expr>
    ): List<Expr> { // @formatter:on
        return aggregate + nextResult
    }

    override fun visitVariableReference(ctx: FluentParser.VariableReferenceContext): List<Expr> {
        return listOf(Reference(Reference.Type.VARIABLE, ctx.IDENT().text, null))
    }

    override fun visitMessageReference(ctx: FluentParser.MessageReferenceContext): List<Expr> {
        val name = ctx.IDENT().text
        val attribute = ctx.attributeAccessor()?.IDENT()?.text
        val type = if (attribute != null) Reference.Type.ATTRIBUTE
        else Reference.Type.MESSAGE
        return listOf(Reference(type, name, attribute))
    }

    private fun expandElementsRecursively( // @formatter:off
        arguments: Map<String, Expr>,
        queue: ArrayDeque<PatternElement>,
        elements: MutableList<PatternElement>
    ) { // @formatter:on
        val visited = ArrayDeque<Evaluable>() // For detecting multi-level cycles

        // Build an info string denoting the detected cycle based on the current visited stack
        fun getCycle(): String = (visited.toList() + visited.first()).joinToString(" -> ") { element ->
            when (element) {
                is Term -> "-${element.name}"
                is Attribute -> ".${element.name}"
                else -> error("Unsupported element type ${element::class.simpleName}")
            }
        }
        // Recursively expand all elements in the queue
        while (queue.isNotEmpty()) {
            when (val element = queue.removeFirst()) {
                // Resolve all term references recursively
                is TermReference -> {
                    val termName = element.entryName
                    val term = context.terms[termName] ?: error("No term named '$termName'")
                    check(term !in visited) { "Term '$termName' cannot reference itself (${getCycle()})" }
                    val attribName = element.attribName
                    val attribute = attribName?.let(term.attributes::get)
                    if (attribute != null) {
                        check(attribute !in visited) { "Attribute '$attribName' cannot reference itself (${getCycle()})" }
                        queue += attribute.elements
                        visited += attribute
                        continue
                    }
                    queue += term.elements
                    visited += term
                }
                // Attempt to resolve variables which match the given arguments of the current parametrized term
                is Reference if element.referenceType == Reference.Type.VARIABLE -> {
                    val varName = element.name
                    val value = if (varName in arguments) arguments[varName]!! else element
                    elements.add(0, value)
                }
                // Any other type of element is directly added to the output
                else -> elements.add(0, element)
            }
        }
    }

    private fun expandElements( // @formatter:off
        elements: List<PatternElement>,
        arguments: Map<String, Expr>
    ): List<PatternElement> { // @formatter:on
        val expandedElements = ArrayList<PatternElement>()
        val queue = ArrayDeque<PatternElement>()
        queue += elements
        expandElementsRecursively(arguments, queue, expandedElements)
        return expandedElements
    }

    override fun visitTermReference(ctx: FluentParser.TermReferenceContext): List<Expr> {
        val name = ctx.IDENT().text
        val attribute = ctx.attributeAccessor()?.IDENT()?.text

        // Parse call arguments if there are any
        val argList = ctx.callArguments()?.argumentList()
        val arguments = HashMap<String, Expr>()
        if (argList != null) for (argument in argList.argument()) {
            val namedArgument = argument.namedArgument()
            if (namedArgument != null) {
                val name = namedArgument.IDENT().text
                // @formatter:off
                val value = namedArgument.stringLiteral()?.accept(context.exprParser)?.first()
                    ?: namedArgument.numberLiteral()!!.accept(context.exprParser).first()
                // @formatter:on
                arguments[name] = value
                continue
            }
            error("Parametrized term may not use positional arguments") // TODO: add support for this?
        }

        // If we are expanding terms, replace term refs with compound expression of actual elements
        if (context.expandTerms) {
            val term = context.terms[name] ?: error("No term named '$name'")
            if (attribute != null) {
                val attrib = term.attributes[attribute] ?: error("No attribute named '$attribute' on term '$name'")
                return listOf(CompoundExpr(expandElements(attrib.elements, arguments)))
            }
            return listOf(CompoundExpr(expandElements(term.elements, arguments)))
        }

        // Otherwise insert term reference which are to be lowered in a secondary parsing step
        if (attribute != null) {
            return listOf(TermReference(name, attribute, arguments))
        }
        return listOf(TermReference(name, null, arguments))
    }

    override fun visitFunctionReference(ctx: FluentParser.FunctionReferenceContext): List<Expr> {
        val name = ctx.IDENT().text
        val arguments = ArrayList<Pair<String?, Expr>>()
        val argList = ctx.callArguments().argumentList()
        if (argList != null) for (argument in argList.argument()) {
            val namedArgument = argument.namedArgument()
            if (namedArgument != null) {
                val name = namedArgument.IDENT().text
                // @formatter:off
                val value = namedArgument.stringLiteral()?.accept(context.exprParser)?.first()
                    ?: namedArgument.numberLiteral()!!.accept(context.exprParser).first()
                // @formatter:on
                arguments += name to value
                continue
            }
            val value = argument.inlineExpression()!!.accept(context.exprParser).first()
            arguments += null to value
        }
        return listOf(FunctionReference(name, arguments))
    }

    override fun visitNumberLiteral(ctx: FluentParser.NumberLiteralContext): List<Expr> {
        val numberText = ctx.NUMBER().text
        val value = if ('.' in numberText) numberText.toDouble()
        else numberText.toLong()
        return listOf(NumberLiteral(value))
    }

    override fun visitStringLiteral(ctx: FluentParser.StringLiteralContext): List<Expr> {
        val builder = StringBuilder()
        for (child in ctx.children!!) {
            when(child) { // @formatter:off
                is TerminalNode -> when(child.symbol.type) {
                    FluentLexer.Tokens.QUOTE -> continue
                    FluentLexer.Tokens.ESCAPED_CHAR -> builder.append(when(val text = child.text) {
                        "\\n" -> "\n"
                        "\\r" -> "\r"
                        "\\t" -> "\t"
                        "\\\"" -> "\""
                        "\\\\" -> "\\"
                        else -> text
                    })
                    FluentLexer.Tokens.UNICODE_CHAR -> builder.append(child.text.substring(2).toInt(16).toChar())
                    FluentLexer.Tokens.STRING_TEXT -> builder.append(child.text)
                }
            } // @formatter:on
        }
        return listOf(StringLiteral(builder.toString()))
    }

    override fun visitSelectExpression(ctx: FluentParser.SelectExpressionContext): List<Expr> {
        fun parseVariantKey(ctx: FluentParser.VariantKeyContext): Expr {
            ctx.IDENT()?.text?.let { key -> return StringLiteral(key) } // Ident keys are interpreted as strings here
            return ctx.numberLiteral()!!.accept(context.exprParser).first()
        }

        val variable = ctx.inlineExpression().accept(context.exprParser).first()

        val variantList = ctx.variantList()
        val variants = LinkedHashMap<Expr, SelectExpr.Variant>()
        for (variant in variantList.variant()) {
            val key = parseVariantKey(variant.variantKey())
            val elements = variant.pattern()
                .patternElement()
                .asSequence()
                .map { element -> element.accept(context.patternElementParser).first() }
                .toList()
            variants[key] = SelectExpr.Variant(key, elements)
        }

        val defaultVariant = variantList.defaultVariant()
        val defaultKey = parseVariantKey(defaultVariant.variantKey())
        val elements = defaultVariant.pattern()
            .patternElement()
            .asSequence()
            .map { element -> element.accept(context.patternElementParser).first() }
            .toList()
        variants[defaultKey] = SelectExpr.Variant(defaultKey, elements, true)

        return listOf(SelectExpr(variable, variants))
    }
}