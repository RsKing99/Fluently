package dev.karmakrafts.fluently.frontend

import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import kotlin.test.Test

class FluentParserTest {
    @Test
    fun `Parse example code`() {
        val charStream = CharStreams.fromString(FLUENT_EXAMPLE)
        val lexer = FluentTestLexer(charStream)
        val tokens = CommonTokenStream(lexer)
        val parser = FluentParser(tokens)
        val file = parser.file()
        println(file.toStringTree(parser))
    }
}