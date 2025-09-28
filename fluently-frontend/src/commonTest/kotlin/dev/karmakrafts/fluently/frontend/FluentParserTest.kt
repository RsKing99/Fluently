package dev.karmakrafts.fluently.frontend

import com.strumenta.antlrkotlin.runtime.BitSet
import org.antlr.v4.kotlinruntime.ANTLRErrorListener
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.Parser
import org.antlr.v4.kotlinruntime.RecognitionException
import org.antlr.v4.kotlinruntime.Recognizer
import org.antlr.v4.kotlinruntime.atn.ATNConfigSet
import org.antlr.v4.kotlinruntime.dfa.DFA
import kotlin.test.Test

class FluentParserTest {
    private object ErrorListener : ANTLRErrorListener {
        override fun reportAmbiguity(
            recognizer: Parser,
            dfa: DFA,
            startIndex: Int,
            stopIndex: Int,
            exact: Boolean,
            ambigAlts: BitSet,
            configs: ATNConfigSet
        ) = Unit

        override fun reportAttemptingFullContext(
            recognizer: Parser,
            dfa: DFA,
            startIndex: Int,
            stopIndex: Int,
            conflictingAlts: BitSet,
            configs: ATNConfigSet
        ) = Unit

        override fun reportContextSensitivity(
            recognizer: Parser, dfa: DFA, startIndex: Int, stopIndex: Int, prediction: Int, configs: ATNConfigSet
        ) = Unit

        override fun syntaxError(
            recognizer: Recognizer<*, *>,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            msg: String,
            e: RecognitionException?
        ) {
            throw RuntimeException(e)
        }
    }

    @Test
    fun `Parse example code`() {
        val charStream = CharStreams.fromString(FLUENT_EXAMPLE)
        val lexer = FluentTestLexer(charStream)
        lexer.removeErrorListeners()
        lexer.addErrorListener(ErrorListener)
        val tokens = CommonTokenStream(lexer)
        val parser = FluentParser(tokens)
        parser.removeErrorListeners()
        parser.addErrorListener(ErrorListener)
        val file = parser.file()
        println(file.toStringTree(parser))
    }
}