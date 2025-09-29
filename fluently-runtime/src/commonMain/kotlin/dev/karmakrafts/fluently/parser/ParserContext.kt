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

package dev.karmakrafts.fluently.parser

import dev.karmakrafts.fluently.entry.Term

data class ParserContext(
    val terms: Map<String, Term>
) {
    sealed interface Parent {
        val name: String
    }

    data class ParentTerm(override val name: String) : Parent
    data class ParentMessage(override val name: String) : Parent
    data class ParentAttribute(val entry: Parent, override val name: String) : Parent

    val messageParser: MessageParser = MessageParser(this)
    val patternElementParser: PatternElementParser = PatternElementParser(this)
    val exprParser: ExprParser = ExprParser(this)
    val parentStack: ArrayDeque<Parent> = ArrayDeque()

    inline val parent: Parent get() = parentStack.last()
    inline val lastParent: Parent? get() = parentStack.getOrNull(parentStack.size - 2)

    fun pushParent(parent: Parent) {
        parentStack.addLast(parent)
    }

    fun popParent() {
        parentStack.removeLast()
    }

    fun pushMessageParent(name: String) = pushParent(ParentMessage(name))
    fun pushTermParent(name: String) = pushParent(ParentTerm(name))
    fun pushAttributeParent(name: String) = pushParent(ParentAttribute(parent, name))
}