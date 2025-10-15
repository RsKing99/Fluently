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

data class SourceRange(
    val start: SourceLocation, val end: SourceLocation
) {
    companion object {
        val undefined: SourceRange = SourceRange(SourceLocation.undefined, SourceLocation.undefined)
        val synthetic: SourceRange = SourceRange(SourceLocation.synthetic, SourceLocation.synthetic)

        fun fromToken(token: Token): SourceRange = SourceRange(
            SourceLocation.fromToken(token), SourceLocation.fromToken(token)
        )

        fun fromTerminalNode(node: TerminalNode): SourceRange = fromToken(node.symbol)

        fun fromContext(context: ParserRuleContext): SourceRange = SourceRange(
            SourceLocation.fromToken(requireNotNull(context.start)),
            SourceLocation.fromToken(requireNotNull(context.stop))
        )

        fun fromLocation(location: SourceLocation): SourceRange = SourceRange(location, location)
    }

    inline val isUndefined: Boolean
        get() = start.isUndefined || end.isUndefined

    inline val isSynthetic: Boolean
        get() = start.isSynthetic || end.isSynthetic
}