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

import dev.karmakrafts.fluently.expr.Expr
import dev.karmakrafts.fluently.expr.ExprScope
import dev.karmakrafts.fluently.expr.bool
import dev.karmakrafts.fluently.expr.enum
import dev.karmakrafts.fluently.expr.number
import dev.karmakrafts.fluently.expr.string
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

@OptIn(ExperimentalCoroutinesApi::class)
fun ExprScope.string(flow: Flow<String>): Flow<Expr> = flow.mapLatest { value ->
    string(value)
}

@OptIn(ExperimentalCoroutinesApi::class)
fun ExprScope.number(flow: Flow<Number>): Flow<Expr> = flow.mapLatest { value ->
    number(value)
}

@OptIn(ExperimentalCoroutinesApi::class)
fun ExprScope.bool(flow: Flow<Boolean>): Flow<Expr> = flow.mapLatest { value ->
    bool(value)
}

@OptIn(ExperimentalCoroutinesApi::class)
fun ExprScope.enum(flow: Flow<Enum<*>>): Flow<Expr> = flow.mapLatest { value ->
    enum(value)
}