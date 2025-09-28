/**
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
    (message NL)
    | (term NL)
    | COMMENT
    ;

term:
    MINUS
    IDENT
    EQ // After this whitespace counts
    BLANK_INLINE?
    pattern
    attribute*
    ;

message:
    IDENT
    EQ // After this whitespace counts
    BLANK_INLINE?
    ((pattern
    attribute*)
    | (attribute+))
    ;

attribute:
    NL
    DOT
    IDENT
    EQ // After this whitespace counts
    BLANK_INLINE?
    pattern
    ;

pattern:
    patternElement+
    ;

patternElement:
    inlineText
    | blockText
    | inlinePlaceable
    | blockPlaceable
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

namedArgument:
    IDENT
    COLON
    (stringLiteral
    | numberLiteral)
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

inlineText:
    (TEXT_CHAR
    | BLANK_INLINE)+
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