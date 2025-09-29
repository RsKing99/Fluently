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

import dev.karmakrafts.fluently.LocalizationFile
import dev.karmakrafts.fluently.entry.Term

data class ParserContext( // @formatter:off
    val file: LocalizationFile,
    val terms: Map<String, Term>,
    val expandTerms: Boolean = false
) { // @formatter:on
    val messageParser: MessageParser = MessageParser(this)
    val patternElementParser: PatternElementParser = PatternElementParser(this)
    val exprParser: ExprParser = ExprParser(this)
}