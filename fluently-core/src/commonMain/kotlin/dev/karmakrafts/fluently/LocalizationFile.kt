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

import dev.karmakrafts.fluently.LocalizationFile.Companion.fromMessages
import dev.karmakrafts.fluently.LocalizationFile.Companion.parse
import dev.karmakrafts.fluently.element.Attribute
import dev.karmakrafts.fluently.entry.Message
import dev.karmakrafts.fluently.eval.EvaluationContext
import dev.karmakrafts.fluently.eval.EvaluationContextBuilder
import dev.karmakrafts.fluently.eval.evaluate
import dev.karmakrafts.fluently.frontend.FluentLexer
import dev.karmakrafts.fluently.frontend.FluentParser
import dev.karmakrafts.fluently.parser.ParserContext
import dev.karmakrafts.fluently.parser.TermParser
import kotlinx.io.Source
import kotlinx.io.readString
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.intellij.lang.annotations.Language

/**
 * In-memory representation of a parsed Fluent localization resource.
 *
 * A localization file holds all public message entries parsed from a Fluent source and provides
 * helpers to format messages and their attributes. Instances are typically created via one of the
 * [parse] overloads or [fromMessages] for programmatic construction. A shared
 * [globalContextInit] lambda can be supplied to seed the evaluation context with variables and
 * functions for every formatting call made through this file.
 *
 * This type is immutable from the API perspective; while [messages] is a mutable map internally,
 * it is only mutated during parsing/initialization and then accessed read-only through the API.
 *
 * @property messages Parsed messages indexed by name.
 * @property globalContextInit A lambda that initializes an [EvaluationContextBuilder] with common
 * variables and functions applied to every formatting request.
 */
@ConsistentCopyVisibility
data class LocalizationFile private constructor( // @formatter:off
    val messages: MutableMap<String, Message> = HashMap(),
    val globalContextInit: EvaluationContextBuilder.() -> Unit
) { // @formatter:on
    companion object {
        /**
         * Creates an empty localization file with no messages and no global context initialization.
         */
        fun empty(): LocalizationFile = LocalizationFile(globalContextInit = {})

        /**
         * Constructs a localization file from an existing [messages] map.
         *
         * Use this when messages are produced programmatically (e.g., in tests). The optional
         * [globalContextInit] seeds the evaluation context for all subsequent formatting calls.
         */
        fun fromMessages( // @formatter:off
            messages: Map<String, Message>,
            globalContextInit: EvaluationContextBuilder.() -> Unit = {}
        ): LocalizationFile { // @formatter:on
            val file = LocalizationFile(globalContextInit = globalContextInit)
            file.messages += messages
            return file
        }

        /**
         * Parses Fluent [source] text into a [LocalizationFile].
         *
         * This builds an ANTLR lexer and parser, collects terms for attribute resolution, and
         * constructs the final messages map. The optional [globalContextInit] lambda is stored on the
         * resulting [LocalizationFile] and will be composed into later formatting calls.
         *
         * @throws IllegalStateException if the Fluent input is syntactically invalid.
         */
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
            file.messages += fileNode.accept(context.messageParser).associateBy { message -> message.name }
            return file
        }

        /**
         * Reads all UTF‑8 text from [source] and delegates to [parse] with a string input.
         */
        fun parse( // @formatter:off
            source: Source,
            globalContextInit: EvaluationContextBuilder.() -> Unit = {}
        ): LocalizationFile { // @formatter:on
            return parse(source.readString(), globalContextInit)
        }
    }

    /**
     * Returns the message with the given [name], or null if it does not exist.
     */
    operator fun get(name: String): Message? = messages[name]

    /**
     * Returns the attribute [attribName] belonging to the message [entryName], or null if missing.
     */
    operator fun get(entryName: String, attribName: String): Attribute? =
        messages[entryName]?.attributes?.get(attribName)

    /**
     * Formats the message identified by [name] using a pre-built [context].
     *
     * Returns a placeholder string of the form `<missing:name>` if the message is not present.
     */
    fun format( // @formatter:off
        name: String,
        context: EvaluationContext
    ): String { // @formatter:on
        return this[name]?.evaluate(context) ?: "<missing:$name>"
    }

    /**
     * Formats the message [name] by building an [EvaluationContext] for this file.
     *
     * The [contextInit] lambda can customize variables or functions for this specific call.
     * Values provided by [contextInit] are composed after [globalContextInit], so they can override
     * defaults.
     *
     * Returns `<missing:name>` if the message is not defined.
     */
    inline fun format( // @formatter:off
        name: String,
        crossinline contextInit: EvaluationContextBuilder.() -> Unit = {}
    ): String { // @formatter:on
        return this[name]?.evaluate(this) {
            globalContextInit()
            contextInit()
        } ?: "<missing:$name>"
    }

    /**
     * Formats the attribute [attribName] of message [entryName] using a pre-built [context].
     *
     * Returns `<missing:entry.attribute>` if the attribute or message is not present.
     */
    fun formatAttribute( // @formatter:off
        entryName: String,
        attribName: String,
        context: EvaluationContext
    ): String { // @formatter:on
        return this[entryName, attribName]?.evaluate(context) ?: "<missing:$entryName.$attribName>"
    }

    /**
     * Formats the attribute [attribName] of message [entryName] by building an [EvaluationContext]
     * for this file. The [contextInit] lambda can supply per-call variables and functions which are
     * applied after [globalContextInit].
     *
     * Returns `<missing:entry.attribute>` if the attribute or message is not defined.
     */
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