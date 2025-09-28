package dev.karmakrafts.fluently.frontend

import org.antlr.v4.kotlinruntime.CharStream
import org.antlr.v4.kotlinruntime.Token

class FluentTestLexer(input: CharStream) : FluentLexer(input) {
    override fun mode(m: Int) {
        super.mode(m)
        println("---------- Changed mode to ${modeNames[m]}")
    }

    override fun emit(token: Token) {
        super.emit(token)
        println("Emitted token ${vocabulary.getSymbolicName(token.type)}: $token")
    }
}