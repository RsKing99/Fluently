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

/**
 * The static type of a Fluent expression.
 *
 * Types are coarse-grained because Fluent formats everything to strings eventually, but knowing
 * whether an expression is numeric can influence function overloading, validation, or selection
 * logic during parsing and evaluation.
 */
enum class ExprType {
    /** A string value resulting from formatting or concatenation. */
    STRING,

    /** A numeric value (integer or floating-point) usable by numeric functions. */
    NUMBER
}