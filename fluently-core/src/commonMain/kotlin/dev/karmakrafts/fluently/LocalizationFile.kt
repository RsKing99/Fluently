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
import dev.karmakrafts.fluently.eval.evaluationContext
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
    internal val messages: MutableMap<String, Message> = HashMap(),
    @PublishedApi internal val globalContextInit: EvaluationContextBuilder.() -> Unit
) { // @formatter:on
    companion object {
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

    private val defaultContext: EvaluationContext = evaluationContext(this, globalContextInit)

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
     * Formats the message with the given [name] using the provided [context].
     *
     * The supplied [context] is merged with this file's preconfigured [globalContextInit]
     * (captured in [defaultContext]) so that variables and functions from both are available
     * during evaluation.
     *
     * @param name The message identifier to format.
     * @param context Additional [EvaluationContext] to merge for this call.
     * @return The formatted string, or null if the message does not exist.
     */
    fun formatOrNull( // @formatter:off
        name: String,
        context: EvaluationContext
    ): String? { // @formatter:on
        return this[name]?.evaluate(defaultContext + context)
    }

    /**
     * Formats the message [name] by building a transient [EvaluationContext] via [contextInit].
     *
     * The created context is combined with this file's [globalContextInit] so that shared
     * variables and functions are always available.
     *
     * @param name The message identifier to format.
     * @param contextInit Lambda to populate a [EvaluationContextBuilder] per-call.
     * @return The formatted string, or null if the message does not exist.
     */
    inline fun formatOrNull( // @formatter:off
        name: String,
        crossinline contextInit: EvaluationContextBuilder.() -> Unit = {}
    ): String? { // @formatter:on
        return this[name]?.evaluate(evaluationContext(this) {
            globalContextInit()
            contextInit()
        })
    }

    /**
     * Formats the attribute [attribName] of message [entryName] using the provided [context].
     *
     * The supplied [context] is merged with this file's [defaultContext] derived from
     * [globalContextInit].
     *
     * @param entryName The message identifier that owns the attribute.
     * @param attribName The attribute name to format.
     * @param context Additional [EvaluationContext] to merge for this call.
     * @return The formatted string, or null if the message or attribute does not exist.
     */
    fun formatOrNull( // @formatter:off
        entryName: String,
        attribName: String,
        context: EvaluationContext
    ): String? { // @formatter:on
        return this[entryName, attribName]?.evaluate(defaultContext + context)
    }

    /**
     * Formats the attribute [attribName] of message [entryName] by constructing a per-call
     * [EvaluationContext] via [contextInit]. The resulting context is combined with the
     * file-level [globalContextInit].
     *
     * @param entryName The message identifier that owns the attribute.
     * @param attribName The attribute name to format.
     * @param contextInit Lambda to populate a [EvaluationContextBuilder] per-call.
     * @return The formatted string, or null if the message or attribute does not exist.
     */
    inline fun formatOrNull( // @formatter:off
        entryName: String,
        attribName: String,
        crossinline contextInit: EvaluationContextBuilder.() -> Unit = {}
    ): String? { // @formatter:on
        return this[entryName, attribName]?.evaluate(evaluationContext(this) {
            globalContextInit()
            contextInit()
        })
    }

    /**
     * Formats the message [name] and returns a non-null string.
     *
     * If the message does not exist, returns a placeholder token in the form "<name>".
     *
     * @param name The message identifier to format.
     * @param context The [EvaluationContext] to merge with the file-level context.
     * @return The formatted message, or a placeholder if missing.
     */
    fun format(name: String, context: EvaluationContext): String = formatOrNull(name, context) ?: "<$name>"

    /**
     * Formats the message [name] using an [EvaluationContext] built by [contextInit].
     *
     * If the message does not exist, returns a placeholder token in the form "<name>".
     *
     * @param name The message identifier to format.
     * @param contextInit Lambda to populate a [EvaluationContextBuilder] per-call.
     * @return The formatted message, or a placeholder if missing.
     */
    fun format(name: String, contextInit: EvaluationContextBuilder.() -> Unit = {}): String =
        formatOrNull(name, contextInit) ?: "<$name>"

    /**
     * Formats the attribute [attribName] of message [name] and returns a non-null string.
     *
     * If the attribute or message does not exist, returns a placeholder token in the form
     * "<name.attribName>".
     *
     * @param name The message identifier that owns the attribute.
     * @param attribName The attribute name to format.
     * @param context The [EvaluationContext] to merge with the file-level context.
     * @return The formatted attribute, or a placeholder if missing.
     */
    fun format(name: String, attribName: String, context: EvaluationContext): String =
        formatOrNull(name, attribName, context) ?: "<$name.$attribName>"

    /**
     * Formats the attribute [attribName] of message [name] using a context built by [contextInit].
     *
     * If the attribute or message does not exist, returns a placeholder token in the form
     * "<name.attribName>".
     *
     * @param name The message identifier that owns the attribute.
     * @param attribName The attribute name to format.
     * @param contextInit Lambda to populate a [EvaluationContextBuilder] per-call.
     * @return The formatted attribute, or a placeholder if missing.
     */
    fun format(name: String, attribName: String, contextInit: EvaluationContextBuilder.() -> Unit = {}): String =
        formatOrNull(name, attribName, contextInit) ?: "<$name.$attribName>"
}