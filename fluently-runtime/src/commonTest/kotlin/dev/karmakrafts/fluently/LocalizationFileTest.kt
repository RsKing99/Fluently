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

import dev.karmakrafts.fluently.expr.ExprType
import dev.karmakrafts.fluently.expr.StringLiteral
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalizationFileTest {
    @Test
    fun `Parse empty file`() {
        val file = LocalizationFile.parse("")
        assertTrue(file.messages.isEmpty())
    }

    @Test
    fun `Parse simple file`() {
        val file = LocalizationFile.parse(
            """
            -my-term = TESTING
            message-number-one = HELLO
            message-number-two = HELLOU
        """.trimIndent()
        )
        assertEquals(2, file.messages.size)
    }

    @Test
    fun `Parse complex file`() {
        val file = LocalizationFile.parse(
            $$"""
            # Some line comments
            -my-term-1 = TESTING
            -my-term-2 = {-my-term-1}::
            ## With different significance
            message-number-one = {-my-term-2} Karma Krafts
            message-number-two = {-my-term-2} TESTING
            message-number-three = It's a { $test ->
                [fox] 🦊
                {"\n\u0020"}fops
                [wolf] 🐺
                {"\n\u0020"}wolp
                *[turtle] 🐢
                {"\n\u0020"}turt
            }! {DEXCL(name: "Pure Kotlin Fluent implementation", 42)}
             .foo = Testing
        """.trimIndent()
        )

        assertEquals(3, file.messages.size)
        assertEquals("""TESTING:: Karma Krafts""", file.format("message-number-one"))
        assertEquals("""TESTING:: TESTING""", file.format("message-number-two"))
        assertEquals(
            """It's a 🐺${"\n\n"} wolp! Pure Kotlin Fluent implementation (42)!!""",
            file.format("message-number-three") {
                variable("test", "wolf")
                function("DEXCL") {
                    parameter("name", ExprType.STRING)
                    parameter("index", ExprType.NUMBER)
                    action { ctx, args ->
                        val name = args["name"]!!.evaluate(ctx)
                        val index = args["index"]!!.evaluate(ctx)
                        StringLiteral("$name ($index)!!")
                    }
                }
            })
    }
}