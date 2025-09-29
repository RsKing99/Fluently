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

package dev.karmakrafts.fluently

import dev.karmakrafts.fluently.entry.Message
import dev.karmakrafts.fluently.expr.Expr
import dev.karmakrafts.fluently.frontend.FluentLexer
import dev.karmakrafts.fluently.frontend.FluentParser
import dev.karmakrafts.fluently.parser.MessageParser
import dev.karmakrafts.fluently.parser.ParserContext
import dev.karmakrafts.fluently.parser.TermParser
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.intellij.lang.annotations.Language

data class LocalizationFile(
    val messages: Map<String, Message>,
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
            val terms = file.accept(TermParser)
            return LocalizationFile(file.accept(MessageParser(ParserContext(terms))))
        }
    }

    constructor(
        messages: List<Message>,
        globalFunctions: Map<String, Function> = emptyMap(),
        globalVariables: Map<String, Expr> = emptyMap()
    ) : this(messages.associateBy { message -> message.name }, globalFunctions, globalVariables)

    inline fun getMessage( // @formatter:off
        name: String,
        contextInit: EvaluationContext.() -> Unit = {}
    ): String { // @formatter:on
        return messages[name]?.elements?.let { elements ->
            val context = EvaluationContext(this).apply(contextInit)
            elements.joinToString("") { element -> element.evaluate(context) }
        } ?: name
    }

    operator fun get(name: String): String = getMessage(name)

    inline fun getMessageAttribute( // @formatter:off
        entryName: String,
        attribName: String,
        contextInit: EvaluationContext.() -> Unit = {}
    ): String { // @formatter:on
        return messages[entryName]?.attributes[attribName]?.elements?.let { elements ->
            val context = EvaluationContext(this).apply(contextInit)
            elements.joinToString("") { element -> element.evaluate(context) }
        } ?: "$entryName:$attribName"
    }

    operator fun get(entryName: String, attribName: String): String = getMessageAttribute(entryName, attribName)
}