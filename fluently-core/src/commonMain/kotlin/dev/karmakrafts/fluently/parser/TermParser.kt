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

import dev.karmakrafts.fluently.Attribute
import dev.karmakrafts.fluently.LocalizationFile
import dev.karmakrafts.fluently.entry.Term
import dev.karmakrafts.fluently.frontend.FluentParser
import dev.karmakrafts.fluently.frontend.FluentParserBaseVisitor

class TermParser(
    file: LocalizationFile
) : FluentParserBaseVisitor<Map<String, Term>>() {
    private val parserContext: ParserContext = ParserContext(file, emptyMap(), false)
    private val patternElementParser: PatternElementParser = PatternElementParser(parserContext)

    override fun defaultResult(): Map<String, Term> = HashMap()

    override fun aggregateResult( // @formatter:off
        aggregate: Map<String, Term>,
        nextResult: Map<String, Term>
    ): Map<String, Term> { // @formatter:on
        return aggregate + nextResult
    }

    private fun parseAttribute(entryName: String, ctx: FluentParser.AttributeContext): Attribute {
        val name = ctx.IDENT().text
        val attribElements = ctx.pattern()
            .patternElement()
            .asSequence()
            .map { element -> element.accept(patternElementParser).first() }
            .toList()
        return Attribute(entryName, name, attribElements)
    }

    override fun visitTerm(ctx: FluentParser.TermContext): Map<String, Term> {
        val name = ctx.IDENT().text
        val elements = ctx.pattern()
            .patternElement()
            .asSequence()
            .map { element -> element.accept(patternElementParser).first() }
            .toList()
        // @formatter:off
        val attributes = ctx.attribute()
            .asSequence()
            .map { attribute -> parseAttribute(name, attribute) }
            .associateBy { attribute -> attribute.name }
        // @formatter:on
        return mapOf(name to Term(name, elements, attributes))
    }
}