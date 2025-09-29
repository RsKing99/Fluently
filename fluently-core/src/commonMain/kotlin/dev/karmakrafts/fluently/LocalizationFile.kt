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
import dev.karmakrafts.fluently.frontend.FluentLexer
import dev.karmakrafts.fluently.frontend.FluentParser
import dev.karmakrafts.fluently.parser.ParserContext
import dev.karmakrafts.fluently.parser.TermParser
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.intellij.lang.annotations.Language

data class LocalizationFile( // @formatter:off
    val messages: MutableMap<String, Message> = HashMap(),
    val globalContextInit: EvaluationContextBuilder.() -> Unit
) { // @formatter:on
    companion object {
        fun parse( // @formatter:off
            @Language("fluent") source: String,
            globalContextInit: EvaluationContextBuilder.() -> Unit = {}
        ): LocalizationFile { // @formatter:on
            val charStream = CharStreams.fromString(source)
            val lexer = FluentLexer(charStream)
            val tokenStream = CommonTokenStream(lexer)
            val parser = FluentParser(tokenStream)
            val fileNode = parser.file()
            val file = LocalizationFile(globalContextInit = globalContextInit)
            val terms = fileNode.accept(TermParser(file))
            val context = ParserContext(file, terms, true)
            // @formatter:off
            file.messages += fileNode.accept(context.messageParser)
                .associateBy { message -> message.name }
            // @formatter:on
            return file
        }
    }

    operator fun get(name: String): Message? = messages[name]
    operator fun get(entryName: String, attribName: String): Attribute? =
        messages[entryName]?.attributes?.get(attribName)

    fun format( // @formatter:off
        name: String,
        context: EvaluationContext
    ): String { // @formatter:on
        return this[name]?.evaluate(context) ?: "<missing:$name>"
    }

    inline fun format( // @formatter:off
        name: String,
        crossinline contextInit: EvaluationContextBuilder.() -> Unit = {}
    ): String { // @formatter:on
        return this[name]?.evaluate(this) {
            globalContextInit()
            contextInit()
        } ?: "<missing:$name>"
    }

    fun formatAttribute( // @formatter:off
        entryName: String,
        attribName: String,
        context: EvaluationContext
    ): String { // @formatter:on
        return this[entryName, attribName]?.evaluate(context) ?: "<missing:$entryName.$attribName>"
    }

    inline fun formatAttribute( // @formatter:off
        entryName: String,
        attribName: String,
        crossinline contextInit: EvaluationContextBuilder.() -> Unit = {}
    ): String { // @formatter:on
        return this[entryName, attribName]?.evaluate(this) {
            globalContextInit()
            contextInit()
        } ?: "<missing:$entryName.$attribName>"
    }
}