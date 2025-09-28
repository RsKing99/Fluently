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

package dev.karmakrafts.fluently.model

import dev.karmakrafts.fluently.model.expr.Expr
import dev.karmakrafts.fluently.model.expr.ExprType

data class Function(
    val name: String,
    val returnType: ExprType,
    val parameters: List<Pair<String, ExprType>>,
    val callback: (Map<String, Expr>) -> String
)