/**
 * An implementation of the formal Fluent EBNF grammar in ANTLR
 * as a separate lexer and parser using inline Kotlin code for
 * contextful tokenization.
 * Original can be found here: https://github.com/projectfluent/fluent/blob/master/spec/fluent.ebnf
 *
 * @author Alexander Hinze
 * @since 27/09/2025
 */

lexer grammar FluentLexer;

// ---------- Custom functions for contextful tokenization
@members {
    fun isTabOrSpace(cp: Int): Boolean = cp == ' '.code || cp == '\t'.code
    fun isContinuation(): Boolean = !isTabOrSpace(_input.LA(2))

    fun nextAfterWhitespace(): Int {
        var laIndex = 2 // Start at char that follows after current match target
        var currentChar = _input.LA(laIndex)
        while(isTabOrSpace(currentChar)) {
            currentChar = _input.LA(++laIndex)
        }
        return currentChar
    }

    fun selectorFollows(): Boolean {
        val code = nextAfterWhitespace()
        return code == '*'.code || code == '['.code // Match *[ and [
    }

    fun placeableEndFollows(): Boolean = nextAfterWhitespace() == '}'.code
}

// ---------- Default mode for handling root entries in the file

COMMENT: ('# ' | '## ' | '### ') ~[\r\n]*;
WS: [\u0020\u0009] -> channel(HIDDEN); // Default mode doesn't care about whitespace
NL: ('\n' | ('\r' '\n'?));
ARROW: '->';
R_BRACE: '}' -> popMode;
L_BRACK: '[';
R_BRACK: ']' -> pushMode(M_VALUE);
L_PAREN: '(';
R_PAREN: ')';
MINUS: '-';
DOT: '.';
COMMA: ',';
COLON: ':';
ASTERISK: '*';
DOLLAR: '$';
QUOTE: '"' -> pushMode(M_STRING);
EQ: '=' -> pushMode(M_VALUE); // After any equals, we enter value mode
NUMBER: [0-9]+ (DOT [0-9]+);
IDENT: [a-zA-Z] [a-zA-Z0-9_-]*;

ERROR: .; // Everything else in default mode is considered an error

// ---------- Value mode interprets verbatim text and switches to different modes when needed
mode M_VALUE;

M_VALUE_END: // When the next char is neither ' ', '\t' or '.', we match NL to exit value mode
    { isContinuation() || selectorFollows() || placeableEndFollows() }?
    NL -> popMode, type(NL)
    ;

L_BRACE: '{' -> pushMode(DEFAULT_MODE);
BLANK_INLINE: WS+;
M_VALUE_NL: NL -> type(NL);
TEXT_CHAR: [\u0000-\uFFFF];

// ---------- String mode interprects escape codes and characters that usually have a special meaning
mode M_STRING;

fragment HEX_DIGIT: [a-fA-F0-9];

M_STRING_END: QUOTE -> popMode, type(QUOTE);
UNICODE_CHAR: '\\' [uU] HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT (HEX_DIGIT HEX_DIGIT)?;
ESCAPED_CHAR: '\\' [nrt0];
STRING_TEXT: ~('"' | '\\')+;