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

import dev.karmakrafts.fluently.element.Block
import dev.karmakrafts.fluently.element.PatternElement
import dev.karmakrafts.fluently.element.Text
import dev.karmakrafts.fluently.frontend.FluentParser
import dev.karmakrafts.fluently.frontend.FluentParserBaseVisitor

class PatternElementParser(
    val context: ParserContext
) : FluentParserBaseVisitor<List<PatternElement>>() {
    override fun defaultResult(): List<PatternElement> = ArrayList()

    override fun aggregateResult( // @formatter:off
        aggregate: List<PatternElement>,
        nextResult: List<PatternElement>
    ): List<PatternElement> { // @formatter:on
        return aggregate + nextResult
    }

    override fun visitInlineText(ctx: FluentParser.InlineTextContext): List<PatternElement> {
        return listOf(Text(ctx.text.trimStart(' ', '\t')))
    }

    override fun visitBlockText(ctx: FluentParser.BlockTextContext): List<PatternElement> {
        return listOf(Block(ctx.inlineText().accept(this).first()))
    }

    override fun visitInlinePlaceable(ctx: FluentParser.InlinePlaceableContext): List<PatternElement> {
        val selectExpression = ctx.selectExpression()
        if (selectExpression != null) {
            return listOf(selectExpression.accept(context.exprParser).first())
        }
        return listOf(ctx.inlineExpression()!!.accept(context.exprParser).first())
    }

    override fun visitBlockPlaceable(ctx: FluentParser.BlockPlaceableContext): List<PatternElement> {
        return listOf(Block(ctx.inlinePlaceable().accept(context.exprParser).first()))
    }
}