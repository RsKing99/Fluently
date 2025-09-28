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

package dev.karmakrafts.fluently.model

import dev.karmakrafts.fluently.frontend.FluentLexer
import dev.karmakrafts.fluently.frontend.FluentParser
import dev.karmakrafts.fluently.model.entry.LocalizationEntry
import dev.karmakrafts.fluently.model.entry.Message
import dev.karmakrafts.fluently.model.entry.Term
import dev.karmakrafts.fluently.model.expr.Expr
import dev.karmakrafts.fluently.parser.LocalizationEntryParser
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.intellij.lang.annotations.Language

data class LocalizationFile(
    val entries: List<LocalizationEntry> = emptyList(),
    val globalFunctions: Map<String, Function> = emptyMap(),
    val globalVariables: Map<String, Expr> = emptyMap()
) {
    companion object {
        fun parse(@Language("fluent") source: String): LocalizationFile {
            val charStream = CharStreams.fromString(source)
            val lexer = FluentLexer(charStream)
            val tokenStream = CommonTokenStream(lexer)
            val parser = FluentParser(tokenStream)
            val file = parser.file()
            return LocalizationFile(file.accept(LocalizationEntryParser))
        }
    }

    private inline fun <reified E : LocalizationEntry> elementsByType(): Lazy<Map<String, E>> = lazy { // @formatter:off
        entries.asSequence()
            .filterIsInstance<E>()
            .associateBy { entry -> entry.name }
    } // @formatter:on

    val messages: Map<String, Message> by elementsByType<Message>()
    val terms: Map<String, Term> by elementsByType<Term>()

    @PublishedApi
    internal inline fun <E : LocalizationEntry> evaluate( // @formatter:off
        entries: Map<String, E>,
        name: String,
        contextInit: EvaluationContext.() -> Unit
    ): String { // @formatter:on
        return entries[name]?.elements?.let { elements ->
            val context = EvaluationContext(this).apply(contextInit)
            elements.joinToString("") { element -> element.evaluate(context) }
        } ?: name
    }

    @PublishedApi
    internal inline fun <E : LocalizationEntry> evaluateAttribute( // @formatter:off
        entries: Map<String, E>,
        entryName: String,
        attribName: String,
        contextInit: EvaluationContext.() -> Unit
    ): String { // @formatter:on
        return entries[entryName]?.attributes[attribName]?.elements?.let { elements ->
            val context = EvaluationContext(this).apply(contextInit)
            elements.joinToString("") { element -> element.evaluate(context) }
        } ?: "$entryName:$attribName"
    }

    inline fun getMessage(// @formatter:off
        name: String,
        contextInit: EvaluationContext.() -> Unit = {}
    ): String = evaluate(messages, name, contextInit) // @formatter:on

    inline fun getMessageAttribute(// @formatter:off
        entryName: String,
        attribName: String,
        contextInit: EvaluationContext.() -> Unit = {}
    ): String = evaluateAttribute(messages, entryName, attribName, contextInit) // @formatter:on

    inline fun getTerm(// @formatter:off
        name: String,
        contextInit: EvaluationContext.() -> Unit = {}
    ): String = evaluate(terms, name, contextInit) // @formatter:on

    inline fun getTermAttribute(// @formatter:off
        entryName: String,
        attribName: String,
        contextInit: EvaluationContext.() -> Unit = {}
    ): String = evaluateAttribute(terms, entryName, attribName, contextInit) // @formatter:on
}