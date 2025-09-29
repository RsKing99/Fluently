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

import dev.karmakrafts.fluently.expr.CallExpr
import dev.karmakrafts.fluently.expr.CompoundExpr
import dev.karmakrafts.fluently.expr.Expr
import dev.karmakrafts.fluently.expr.NumberLiteral
import dev.karmakrafts.fluently.expr.ReferenceExpr
import dev.karmakrafts.fluently.expr.SelectExpr
import dev.karmakrafts.fluently.expr.StringLiteral
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
        return listOf(ReferenceExpr(ReferenceExpr.Type.VARIABLE, ctx.IDENT().text, null))
    }

    private fun checkAttributeAccessor(ctx: FluentParser.AttributeAccessorContext) {
        // Check for cyclic reference
        val name = ctx.IDENT().text
        val parent = context.parent
        val lastParent = context.lastParent ?: return
        val type = if (lastParent is ParserContext.ParentMessage) "message"
        else "term"
        // @formatter:off
        check(!(parent is ParserContext.ParentAttribute
            && parent.entry == lastParent
            && parent.name == name)) { "Attribute '$name' on $type '${lastParent.name}' cannot reference itself" }
        // @formatter:on
    }

    override fun visitMessageReference(ctx: FluentParser.MessageReferenceContext): List<Expr> {
        val name = ctx.IDENT().text

        // Check for cyclic reference
        val parent = context.parent
        check(!(parent is ParserContext.ParentMessage && parent.name == name)) { "Message '$name' cannot reference itself" }

        val attributeAccessor = ctx.attributeAccessor()?.apply {
            checkAttributeAccessor(this)
        }
        val attribute = attributeAccessor?.IDENT()?.text
        val type = if (attribute != null) ReferenceExpr.Type.ATTRIBUTE
        else ReferenceExpr.Type.MESSAGE
        return listOf(ReferenceExpr(type, name, attribute))
    }

    override fun visitTermReference(ctx: FluentParser.TermReferenceContext): List<Expr> {
        val name = ctx.IDENT().text

        // Check for cyclic reference
        val parent = context.parent
        check(!(parent is ParserContext.ParentTerm && parent.name == name)) { "Term '$name' cannot reference itself" }

        val term = context.terms[name] ?: error("No term named '$name'")
        val attributeAccessor = ctx.attributeAccessor()?.apply {
            checkAttributeAccessor(this)
        }
        val attribute = attributeAccessor?.IDENT()?.text
        if (attribute != null) {
            val attrib = term.attributes[attribute] ?: error("No attribute named '$attribute' on term '$name'")
            return listOf(CompoundExpr(attrib.elements))
        }
        return listOf(CompoundExpr(term.elements))
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
        return listOf(CallExpr(name, arguments))
    }

    override fun visitNumberLiteral(ctx: FluentParser.NumberLiteralContext): List<Expr> {
        val numberText = ctx.NUMBER().text
        val value = if ('.' in numberText) numberText.toDouble()
        else numberText.toLong()
        return listOf(NumberLiteral(value, value is Double))
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