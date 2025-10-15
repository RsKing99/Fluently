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
import dev.karmakrafts.fluently.entry.Message
import dev.karmakrafts.fluently.frontend.FluentParser
import dev.karmakrafts.fluently.frontend.FluentParserBaseVisitor
import dev.karmakrafts.fluently.util.TokenRange

internal class MessageParser(
    val context: ParserContext
) : FluentParserBaseVisitor<List<Message>>() {
    override fun defaultResult(): List<Message> = ArrayList()

    override fun aggregateResult( // @formatter:off
        aggregate: List<Message>,
        nextResult: List<Message>
    ): List<Message> { // @formatter:on
        return aggregate + nextResult
    }

    private fun parseAttribute(entryName: String, ctx: FluentParser.AttributeContext): Attribute {
        val name = ctx.IDENT().text
        val attribElements = ctx.pattern()
            ?.patternElement()
            ?.asSequence()
            ?.flatMap { element -> element.accept(context.patternElementParser) }
            ?.toList() ?: emptyList()
        return Attribute(TokenRange.fromContext(ctx), entryName, name, attribElements)
    }

    override fun visitMessage(ctx: FluentParser.MessageContext): List<Message> {
        val name = ctx.IDENT().text
        val elements = ctx.pattern()
            ?.patternElement()
            ?.asSequence()
            ?.flatMap { element -> element.accept(context.patternElementParser) }
            ?.toList() ?: emptyList()
        // @formatter:off
        val attributes = ctx.attribute()
            .asSequence()
            .map { attribute -> parseAttribute(name, attribute) }
            .associateBy { attribute -> attribute.name }
        // @formatter:on
        return listOf(Message(TokenRange.fromContext(ctx), name, elements, attributes))
    }
}