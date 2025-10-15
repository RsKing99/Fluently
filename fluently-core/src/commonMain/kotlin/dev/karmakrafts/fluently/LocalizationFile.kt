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

import dev.karmakrafts.fluently.LocalizationFile.Companion.parse
import dev.karmakrafts.fluently.element.Attribute
import dev.karmakrafts.fluently.element.ElementReducer
import dev.karmakrafts.fluently.entry.LocalizationEntry
import dev.karmakrafts.fluently.entry.Message
import dev.karmakrafts.fluently.entry.Term
import dev.karmakrafts.fluently.eval.EvaluationContext
import dev.karmakrafts.fluently.eval.EvaluationContextBuilder
import dev.karmakrafts.fluently.eval.EvaluationContextSpec
import dev.karmakrafts.fluently.eval.evaluationContext
import dev.karmakrafts.fluently.frontend.FluentLexer
import dev.karmakrafts.fluently.frontend.FluentParser
import dev.karmakrafts.fluently.parser.FluentlyParserException
import dev.karmakrafts.fluently.parser.ParserContext
import dev.karmakrafts.fluently.parser.TermParser
import dev.karmakrafts.fluently.util.Accessor
import kotlinx.io.Source
import kotlinx.io.readString
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.Token
import org.intellij.lang.annotations.Language

@ConsistentCopyVisibility
data class LocalizationFile private constructor( // @formatter:off
    val tokens: List<Token> = emptyList(),
    val entries: MutableList<LocalizationEntry> = ArrayList(),
    val globalContextInit: EvaluationContextSpec = {}
) { // @formatter:on
    companion object {
        /**
         * An empty localization file with no messages.
         *
         * Useful as a neutral default when no resources are available. Formatting calls
         * against this instance will return placeholders as the requested messages/attributes
         * do not exist.
         */
        val empty: LocalizationFile = LocalizationFile()

        /**
         * Parses Fluent [source] text into a [LocalizationFile].
         *
         * This builds an ANTLR lexer and parser, collects terms for attribute resolution, and
         * constructs the final messages map. The optional [globalContextInit] lambda is stored on the
         * resulting [LocalizationFile] and will be composed into later formatting calls.
         *
         * @param source The Fluent source text to parse.
         * @param expandTerms Whether terms are expanded while parsing to save
         *  performance by not evaluating them at runtime.
         * @param globalContextInit A closure to seed every [EvaluationContext] created
         *  by the resulting file instance.
         * @throws IllegalStateException if the Fluent input is syntactically invalid.
         */
        @Throws(FluentlyParserException::class)
        fun parse( // @formatter:off
            @Language("fluent") source: String,
            expandTerms: Boolean = true,
            globalContextInit: EvaluationContextSpec = {}
        ): LocalizationFile { // @formatter:on
            val charStream = CharStreams.fromString(source)
            val lexer = FluentLexer(charStream)
            val tokenStream = CommonTokenStream(lexer)
            tokenStream.fill()
            val tokens = tokenStream.tokens.toList() // Create a copy of the token stream list
            val parser = FluentParser(tokenStream)
            val fileNode = parser.file()
            val file = LocalizationFile( // @formatter:off
                tokens = tokens,
                globalContextInit = globalContextInit
            ) // @formatter:on
            val terms = fileNode.accept(TermParser(file))
            val context = ParserContext(file, terms, expandTerms)
            file.entries += fileNode.accept(context.messageParser)
            if (!expandTerms) file.entries += terms.values // If we don't expand terms at parse time, retain them as entries
            return file
        }

        /**
         * Reads all UTF‑8 text from [source] and delegates to [parse] with a string input.
         */
        @Throws(FluentlyParserException::class)
        fun parse( // @formatter:off
            source: Source,
            globalContextInit: EvaluationContextSpec = {}
        ): LocalizationFile { // @formatter:on
            return parse(source.readString(), globalContextInit = globalContextInit)
        }
    }

    private val defaultContext: EvaluationContext = evaluationContext(this, globalContextInit)

    fun messages(): Sequence<Message> = sequence {
        for (entry in entries) {
            if (entry !is Message) continue
            yield(entry)
        }
    }

    fun messageMap(): Map<String, Message> = messages().associateBy { message -> message.name }

    fun terms(): Sequence<Term> = sequence {
        for (entry in entries) {
            if (entry !is Term) continue
            yield(entry)
        }
    }

    fun termMap(): Map<String, Term> = terms().associateBy { term -> term.name }

    /** Returns true if this file contains the given message */
    fun hasMessage(name: String): Boolean = this[name] != null

    /** Returns true if this file contains the given message attribute */
    fun hasAttribute(entryName: String, attribName: String): Boolean = this[entryName, attribName] != null

    /**
     * Returns the message with the given [name], or null if it does not exist.
     */
    operator fun get(name: String): Message? =
        entries.filterIsInstance<Message>().find { message -> message.name == name }

    /**
     * Returns the attribute [attribName] belonging to the message [entryName], or null if missing.
     */
    operator fun get(entryName: String, attribName: String): Attribute? = this[entryName]?.attributes?.get(attribName)

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
        crossinline contextInit: EvaluationContextSpec = {}
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
        crossinline contextInit: EvaluationContextSpec = {}
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
    fun format(name: String, contextInit: EvaluationContextSpec = {}): String =
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
    fun format(name: String, attribName: String, contextInit: EvaluationContextSpec = {}): String =
        formatOrNull(name, attribName, contextInit) ?: "<$name.$attribName>"

    /**
     * Returns an accessor that formats message values by name.
     *
     * The produced [Accessor] accepts a message identifier and returns the formatted string,
     * applying this file's [globalContextInit].
     */
    fun formatting(context: EvaluationContext): Accessor<String> = Accessor { format(it, context) }

    /**
     * Returns an accessor that formats message values by name.
     *
     * The produced [Accessor] accepts a message identifier and returns the formatted string,
     * applying this file's [globalContextInit].
     */
    fun formatting(contextInit: EvaluationContextSpec = {}): Accessor<String> = Accessor { format(it, contextInit) }

    /**
     * Returns an accessor for attributes of the given [entryName].
     *
     * The resulting [Accessor] accepts an attribute name and returns the formatted value for
     * the attribute of the specified message, applying this file's [globalContextInit].
     */
    fun formatting(entryName: String, context: EvaluationContext): Accessor<String> = Accessor { attribName ->
        format(entryName, attribName, context)
    }

    /**
     * Returns an accessor for attributes of the given [entryName].
     *
     * The resulting [Accessor] accepts an attribute name and returns the formatted value for
     * the attribute of the specified message, applying this file's [globalContextInit].
     */
    fun formatting(entryName: String, contextInit: EvaluationContextSpec = {}): Accessor<String> =
        Accessor { attribName ->
            format(entryName, attribName, contextInit)
        }

    /**
     * Utility function for applying an [ElementReducer] to
     * all messages of this file. Sequentially aggregates all
     * sub-results for the return value.
     *
     * @param R The type of the value produced by reducing this file
     *  using the given [ElementReducer].
     * @param reducer The [ElementReducer] instance which to apply
     *  to all messages of this file.
     * @return A value aggregated by the given [ElementReducer] instance.
     */
    fun <R> accept(reducer: ElementReducer<R>): R {
        return entries.map { entry -> entry.accept(reducer) }.reduce(reducer::aggregate)
    }

    inline fun <reified T : LocalizationEntry, R> acceptOnly(reducer: ElementReducer<R>): R { // @formatter:off
        return entries.filterIsInstance<T>()
            .map { entry -> entry.accept(reducer) }
            .reduce(reducer::aggregate)
    } // @formatter:on
}