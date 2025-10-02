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

import dev.karmakrafts.fluently.eval.EvaluationContextBuilder
import kotlinx.io.asSource
import kotlinx.io.buffered

/**
 * Parses a [LocalizationFile] from a classpath resource on JVM/Android.
 *
 * This convenience function opens the resource via [Class.getResourceAsStream],
 * wraps it as a buffered source and delegates to [LocalizationFile.parse].
 * It is intended for cases where your .ftl or localization files are packaged
 * as resources within your application or library JAR.
 *
 * @param path classpath resource path to the localization file (e.g. "/i18n/en.ftl"). Must start with "/".
 * @param globalContextInit optional initializer for the global [EvaluationContextBuilder]
 * that will be available during message evaluation.
 * @return the parsed [LocalizationFile] instance.
 *
 * @throws NullPointerException if [path] doesn't exist within the classpath.
 */
fun LocalizationFile.Companion.parseResource( // @formatter:off
    path: String,
    globalContextInit: EvaluationContextBuilder.() -> Unit = {}
): LocalizationFile { // @formatter:on
    return this::class.java.getResourceAsStream(path)!!.use { stream ->
        LocalizationFile.parse(stream.asSource().buffered(), globalContextInit)
    }
}