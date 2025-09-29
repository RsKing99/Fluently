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

import dev.karmakrafts.fluently.model.Function
import dev.karmakrafts.fluently.model.LocalizationFile
import dev.karmakrafts.fluently.model.expr.ExprType
import dev.karmakrafts.fluently.model.expr.StringLiteral
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalizationFileTest {
    @Test
    fun `Parse empty file`() {
        val file = LocalizationFile.parse("")
        assertTrue(file.entries.isEmpty())
    }

    @Test
    fun `Parse simple file`() {
        val file = LocalizationFile.parse("""
            -my-term = TESTING
            message-number-one = HELLO
            message-number-two = HELLOU
        """.trimIndent())
        assertEquals(3, file.entries.size)
    }

    @Test
    fun `Parse complex file`() {
        val file = LocalizationFile.parse($$"""
            -my-term = TESTING
            message-number-one = {-my-term} Karma Krafts
            message-number-two = {-my-term} TESTING
            message-number-three = It's a { $test ->
                [fox] 🦊
                {" "}fops
                [wolp] 🐺
                {" "}wolp
                *[turtle] 🐢
                {" "}turt
            }! {DEXCL(name: "Pure Kotlin Fluent implementation", 1)}
        """.trimIndent())
        assertEquals(4, file.entries.size)
        assertEquals("""It's a 🐺${'\n'} wolp! Pure Kotlin Fluent implementation!!""", file.getMessage("message-number-three") {
            variables["test"] = StringLiteral("wolp")
            functions["DEXCL"] = Function("DEXCL", ExprType.STRING,
                listOf("name" to ExprType.STRING, "index" to ExprType.NUMBER)) { ctx, args ->
                val name = args.values.first().evaluate(ctx)
                StringLiteral("$name!!")
            }
        })
    }
}