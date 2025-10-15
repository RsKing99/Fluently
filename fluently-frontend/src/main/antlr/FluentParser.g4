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

/*
 * An implementation of the formal Fluent EBNF grammar in ANTLR
 * as a separate lexer and parser using inline Kotlin code for
 * contextful tokenization.
 * Original can be found here: https://github.com/projectfluent/fluent/blob/master/spec/fluent.ebnf
 *
 * @author Alexander Hinze
 * @since 27/09/2025
 */

parser grammar FluentParser;

options {
    tokenVocab = FluentLexer;
}

file:
    (entry
    | blankBlock)*
    EOF
    ;

entry:
    message
    | term
    | COMMENT
    ;

/*
 * Modification 09/10/2025: Remove the requirement for superfluous equal sign after an identifier
 *  Fix for https://github.com/projectfluent/fluent/issues/190
 */
term:
    MINUS
    IDENT
    (EQ // After this whitespace counts
    BLANK_INLINE?
    pattern
    attribute*)?
    ;

/*
 * Modification 09/10/2025: Remove the requirement for superfluous equal sign after an identifier
 *  Fix for https://github.com/projectfluent/fluent/issues/190
 */
message:
    IDENT
    (EQ // After this whitespace counts
    BLANK_INLINE?
    ((pattern
    attribute*)
    | (attribute+)))?
    ;

/*
 * Modification 09/10/2025: Remove the requirement for superfluous equal sign after an identifier
 *  Fix for https://github.com/projectfluent/fluent/issues/190
 */
attribute:
    NL
    DOT
    IDENT
    (EQ // After this whitespace counts
    BLANK_INLINE?
    pattern)?
    ;

pattern:
    patternElement+
    ;

patternElement:
    blockText
    | inlineText
    | blockPlaceable
    | inlinePlaceable
    ;

selectExpression:
    inlineExpression
    ARROW
    variantList
    ;

variantList:
    variant*
    defaultVariant
    variant*
    NL
    ;

variant:
    NL
    variantKey
    BLANK_INLINE?
    pattern
    ;

defaultVariant:
    NL
    ASTERISK
    variantKey
    BLANK_INLINE?
    pattern
    ;

// Whitespace counts after this rule
variantKey:
    L_BRACK
    (numberLiteral | IDENT)
    R_BRACK
    ;

inlineExpression:
    inlinePlaceable
    | stringLiteral
    | numberLiteral
    | functionReference
    | variableReference
    | termReference
    | messageReference
    ;

variableReference:
    DOLLAR
    IDENT
    ;

termReference:
    MINUS
    IDENT
    attributeAccessor?
    callArguments?
    ;

messageReference:
    IDENT
    attributeAccessor?
    ;

attributeAccessor:
    DOT
    IDENT
    ;

functionReference:
    IDENT
    callArguments
    ;

callArguments:
    L_PAREN
    argumentList?
    R_PAREN
    ;

argumentList:
    argument
    (COMMA
    argument)*
    ;

argument:
    namedArgument
    | inlineExpression
    ;

/*
 * Modification 08/10/2025: Make named arguments accept inlineExpression instead of only literals
 *  Fix for https://github.com/projectfluent/fluent/issues/230
 */
namedArgument:
    IDENT
    COLON
    inlineExpression
    ;

numberLiteral:
    MINUS?
    NUMBER
    ;

stringLiteral:
    QUOTE
    (STRING_TEXT
    | UNICODE_CHAR
    | ESCAPED_CHAR)*
    QUOTE
    ;

blockPlaceable:
    blankBlock
    BLANK_INLINE?
    inlinePlaceable
    ;

inlinePlaceable:
    L_BRACE // Whitespace doesn't count until R_BRACE
    (selectExpression
    | inlineExpression)
    R_BRACE
    ;

/*
 * Modification 27/09/2025: Make inlineText match BLANK_INLINEs
 *  Fix a grammar discrepancy due to the different lexing approach
 */
inlineText:
    (BLANK_INLINE
    | TEXT_CHAR)+
    ;

blockText:
    blankBlock
    BLANK_INLINE
    inlineText
    ;

blankBlock:
    (BLANK_INLINE?
    NL)+
    ;