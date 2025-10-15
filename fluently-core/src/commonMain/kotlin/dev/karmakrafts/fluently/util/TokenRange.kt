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

import org.antlr.v4.kotlinruntime.ParserRuleContext
import org.antlr.v4.kotlinruntime.Token
import org.antlr.v4.kotlinruntime.tree.TerminalNode

data class TokenRange(
    val start: Int, val end: Int
) {
    companion object {
        const val UNDEFINED: Int = -1
        const val SYNTHETIC: Int = -2

        val undefined: TokenRange = TokenRange(UNDEFINED, UNDEFINED)
        val synthetic: TokenRange = TokenRange(SYNTHETIC, SYNTHETIC)

        fun fromToken(token: Token): TokenRange = TokenRange(token.tokenIndex, token.tokenIndex)

        fun fromTerminalNode(node: TerminalNode): TokenRange = fromToken(node.symbol)

        fun fromContext(context: ParserRuleContext): TokenRange {
            val start = requireNotNull(context.start).tokenIndex
            val end = requireNotNull(context.stop).tokenIndex
            return TokenRange(start, end)
        }
    }

    inline val isUndefined: Boolean
        get() = start == UNDEFINED || end == UNDEFINED

    inline val isSynthetic: Boolean
        get() = start == SYNTHETIC || end == SYNTHETIC

    fun toSourceRange(tokens: List<Token>): SourceRange {
        val startLocation = SourceLocation.fromToken(tokens[start])
        val endLocation = SourceLocation.fromToken(tokens[end])
        return SourceRange(startLocation, endLocation)
    }
}