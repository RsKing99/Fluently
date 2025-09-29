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

package dev.karmakrafts.fluently.expr

import dev.karmakrafts.fluently.EvaluationContext

data class TermReference(val entryName: String, val attribName: String?, val arguments: Map<String, Expr>) : Expr {
    inline val isParametrized: Boolean
        get() = arguments.isNotEmpty()

    override fun getType(context: EvaluationContext): ExprType {
        error("Term reference hasn't been lowered")
    }

    override fun evaluate(context: EvaluationContext): String {
        error("Term reference hasn't been lowered")
    }
}