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

import dev.karmakrafts.fluently.frontend.FluentLexer
import dev.karmakrafts.fluently.frontend.FluentParser
import dev.karmakrafts.fluently.frontend.FluentParserBaseVisitor
import dev.karmakrafts.fluently.model.expr.CallExpr
import dev.karmakrafts.fluently.model.expr.Expr
import dev.karmakrafts.fluently.model.expr.NumberLiteral
import dev.karmakrafts.fluently.model.expr.ReferenceExpr
import dev.karmakrafts.fluently.model.expr.SelectExpr
import dev.karmakrafts.fluently.model.expr.StringLiteral
import org.antlr.v4.kotlinruntime.tree.TerminalNode

object ExprParser : FluentParserBaseVisitor<List<Expr>>() {
    override fun defaultResult(): List<Expr> = ArrayList()

    override fun aggregateResult( // @formatter:off
        aggregate: List<Expr>,
        nextResult: List<Expr>
    ): List<Expr> { // @formatter:on
        return aggregate + nextResult
    }

    override fun visitMessageReference(ctx: FluentParser.MessageReferenceContext): List<Expr> {
        val attribute = ctx.attributeAccessor()?.IDENT()?.text
        val type = if (attribute != null) ReferenceExpr.Type.MESSAGE_ATTRIB
        else ReferenceExpr.Type.MESSAGE
        return listOf(ReferenceExpr(type, ctx.IDENT().text, attribute))
    }

    override fun visitTermReference(ctx: FluentParser.TermReferenceContext): List<Expr> {
        val attribute = ctx.attributeAccessor()?.IDENT()?.text
        val type = if (attribute != null) ReferenceExpr.Type.TERM_ATTRIB
        else ReferenceExpr.Type.TERM
        return listOf(ReferenceExpr(type, ctx.IDENT().text, attribute))
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
                val value = namedArgument.stringLiteral()?.accept(ExprParser)?.first()
                    ?: namedArgument.numberLiteral()!!.accept(ExprParser).first()
                // @formatter:on
                arguments += name to value
                continue
            }
            val value = argument.inlineExpression()!!.accept(ExprParser).first()
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
                is TerminalNode if child.symbol.type == FluentLexer.Tokens.QUOTE -> continue
                is TerminalNode if child.symbol.type == FluentLexer.Tokens.ESCAPED_CHAR -> {
                    builder.append(when(val text = child.text) {
                        "\\n" -> "\n"
                        "\\r" -> "\r"
                        "\\t" -> "\t"
                        "\\\"" -> "\""
                        "\\\\" -> "\\"
                        else -> text
                    })
                }
                is TerminalNode if child.symbol.type == FluentLexer.Tokens.UNICODE_CHAR -> {
                    val value = child.text.substring(1).toInt(16)
                    builder.append(value.toChar())
                }
                is TerminalNode if child.symbol.type == FluentLexer.Tokens.STRING_TEXT -> {
                    builder.append(child.text)
                }
            } // @formatter:on
        }
        return listOf(StringLiteral(builder.toString()))
    }

    override fun visitSelectExpression(ctx: FluentParser.SelectExpressionContext): List<Expr> {
        fun parseVariantKey(ctx: FluentParser.VariantKeyContext): Expr {
            ctx.IDENT()?.text?.let { key -> return StringLiteral(key) } // Ident keys are interpreted as strings here
            return ctx.numberLiteral()!!.accept(ExprParser).first()
        }

        val variable = ctx.inlineExpression().accept(ExprParser).first()

        val variantList = ctx.variantList()
        val variants = LinkedHashMap<Expr, SelectExpr.Variant>()
        for (variant in variantList.variant()) {
            val key = parseVariantKey(variant.variantKey())
            val elements = variant.pattern()
                .patternElement()
                .asSequence()
                .map { element -> element.accept(PatternElementParser).first() }
                .toList()
            variants[key] = SelectExpr.Variant(key, elements)
        }

        val defaultVariant = variantList.defaultVariant()
        val defaultKey = parseVariantKey(defaultVariant.variantKey())
        val elements = defaultVariant.pattern()
            .patternElement()
            .asSequence()
            .map { element -> element.accept(PatternElementParser).first() }
            .toList()
        variants[defaultKey] = SelectExpr.Variant(defaultKey, elements, true)

        return listOf(SelectExpr(variable, variants))
    }
}