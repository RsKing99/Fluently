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

package dev.karmakrafts.fluently.util

import com.strumenta.antlrkotlin.runtime.BitSet
import dev.karmakrafts.fluently.parser.FluentlyParserException
import org.antlr.v4.kotlinruntime.ANTLRErrorListener
import org.antlr.v4.kotlinruntime.Parser
import org.antlr.v4.kotlinruntime.RecognitionException
import org.antlr.v4.kotlinruntime.Recognizer
import org.antlr.v4.kotlinruntime.Token
import org.antlr.v4.kotlinruntime.atn.ATNConfigSet
import org.antlr.v4.kotlinruntime.dfa.DFA

internal class FluentlyErrorListener(
    private val file: String
) : ANTLRErrorListener {
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
        recognizer: Parser, dfa: DFA, startIndex: Int, stopIndex: Int, conflictingAlts: BitSet, configs: ATNConfigSet
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
        throw FluentlyParserException(
            message = "Syntax error in line $line:$charPositionInLine: $msg",
            cause = e,
            tokenRange = when (offendingSymbol) {
                is Token -> TokenRange.fromToken(offendingSymbol)
                else -> if (e != null) TokenRange.fromToken(requireNotNull(e.offendingToken)) else TokenRange.undefined
            }
        )
    }
}