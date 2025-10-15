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

data class SourceLocation(
    val line: Int, val column: Int
) {
    companion object {
        const val UNDEFINED: Int = -1
        const val SYNTHETIC: Int = -2

        val undefined: SourceLocation = SourceLocation(UNDEFINED, UNDEFINED)
        val synthetic: SourceLocation = SourceLocation(SYNTHETIC, SYNTHETIC)

        fun fromToken(token: Token): SourceLocation = SourceLocation(
            token.line, token.charPositionInLine
        )

        fun fromContext(
            context: ParserRuleContext
        ): SourceLocation = fromToken(requireNotNull(context.start ?: context.stop))

        fun fromTerminalNode(
            node: TerminalNode
        ): SourceLocation = fromToken(node.symbol)
    }

    inline val isUndefined: Boolean
        get() = line == UNDEFINED || column == UNDEFINED

    inline val isSynthetic: Boolean
        get() = line == SYNTHETIC || column == SYNTHETIC
}