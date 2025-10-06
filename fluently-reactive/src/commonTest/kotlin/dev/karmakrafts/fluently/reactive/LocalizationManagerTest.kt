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

package dev.karmakrafts.fluently.reactive

import dev.karmakrafts.fluently.bundle.LocalizationBundle
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.writeString
import kotlin.test.Test

class LocalizationManagerTest {
    private val bundle = LocalizationBundle.fromJsonString(
        """
            {
              "version": 1,
              "default_locale": "en-US",
              "entries": {
                "en-US": {
                  "display_name": "English (US)",
                  "path": "en_us.ftl"
                },
                "de-DE": {
                  "display_name": "Deutsch (Deutschland)",
                  "path": "de_de.ftl"
                }
              }
            }
        """.trimIndent()
    )

    private fun testSource(source: (String) -> String): suspend (String) -> Source = { locale ->
        Buffer().apply {
            writeString(source(locale))
        }
    }

    @Test
    fun `Format message using default locale`() = runTest {
        val manager = LocalizationManager(flowOf(bundle), testSource {
            """
                my-entry = Hello, World!
            """.trimIndent()
        }, backgroundScope.coroutineContext)

        val entry = manager.format("my-entry").stateIn(backgroundScope)
        entry.first() shouldBe "Hello, World!"
    }

    @Test
    fun `Format message using current locale`() = runTest {
        val manager = LocalizationManager(flowOf(bundle), testSource { locale ->
            if (locale == "en_us.ftl") {
                return@testSource $$"""
                    my-entry = Hello, {$myValue}!
                """.trimIndent()
            }
            $$"""
                my-entry = Hallo, {$myValue}!
            """.trimIndent()
        }, backgroundScope.coroutineContext)

        val myValue = MutableStateFlow("World")
        val entry = manager.format("my-entry") {
            reactiveStringVariable("myValue", myValue)
        }.stateIn(backgroundScope)

        manager.locale.value = "en-US"
        delay(50)
        entry.value shouldBe "Hello, World!"

        manager.locale.value = "de-DE"
        myValue.value = "Welt"
        delay(50)
        entry.value shouldBe "Hallo, Welt!"
    }
}