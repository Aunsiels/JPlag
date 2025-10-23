package de.jplag.php;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.jplag.ParsingException;
import de.jplag.Token;
import de.jplag.util.FileUtils;

final class PhpParser {

    List<Token> parse(Set<File> files) throws ParsingException {
        List<Token> tokens = new ArrayList<>();
        for (File file : files) {
            tokens.addAll(parseFile(file));
            tokens.add(Token.fileEnd(file));
        }
        return tokens;
    }

    private List<Token> parseFile(File file) throws ParsingException {
        try {
            String content = FileUtils.readFileContent(file, true);
            PhpTokenizer tokenizer = new PhpTokenizer(file, content);
            return tokenizer.tokenize();
        } catch (IOException e) {
            throw new ParsingException(file, e.getMessage(), e);
        }
    }

    private static final class PhpTokenizer {
        private static final Map<String, PhpTokenType> KEYWORDS = Map.ofEntries(Map.entry("namespace", PhpTokenType.NAMESPACE),
                Map.entry("use", PhpTokenType.USE), Map.entry("class", PhpTokenType.CLASS), Map.entry("interface", PhpTokenType.INTERFACE),
                Map.entry("trait", PhpTokenType.TRAIT), Map.entry("enum", PhpTokenType.ENUM), Map.entry("extends", PhpTokenType.EXTENDS),
                Map.entry("implements", PhpTokenType.IMPLEMENTS), Map.entry("function", PhpTokenType.FUNCTION),
                Map.entry("fn", PhpTokenType.FUNCTION), Map.entry("if", PhpTokenType.IF), Map.entry("elseif", PhpTokenType.ELSEIF),
                Map.entry("else", PhpTokenType.ELSE), Map.entry("for", PhpTokenType.FOR), Map.entry("foreach", PhpTokenType.FOREACH),
                Map.entry("while", PhpTokenType.WHILE), Map.entry("do", PhpTokenType.DO), Map.entry("switch", PhpTokenType.SWITCH),
                Map.entry("case", PhpTokenType.CASE), Map.entry("default", PhpTokenType.DEFAULT), Map.entry("try", PhpTokenType.TRY),
                Map.entry("catch", PhpTokenType.CATCH), Map.entry("finally", PhpTokenType.FINALLY), Map.entry("return", PhpTokenType.RETURN),
                Map.entry("break", PhpTokenType.BREAK), Map.entry("continue", PhpTokenType.CONTINUE), Map.entry("throw", PhpTokenType.THROW),
                Map.entry("new", PhpTokenType.NEW), Map.entry("include", PhpTokenType.INCLUDE), Map.entry("include_once", PhpTokenType.INCLUDE),
                Map.entry("require", PhpTokenType.REQUIRE), Map.entry("require_once", PhpTokenType.REQUIRE), Map.entry("echo", PhpTokenType.ECHO),
                Map.entry("print", PhpTokenType.PRINT), Map.entry("global", PhpTokenType.GLOBAL), Map.entry("static", PhpTokenType.STATIC),
                Map.entry("const", PhpTokenType.CONST), Map.entry("yield", PhpTokenType.YIELD), Map.entry("match", PhpTokenType.MATCH),
                Map.entry("endif", PhpTokenType.BLOCK_END), Map.entry("endforeach", PhpTokenType.BLOCK_END),
                Map.entry("endfor", PhpTokenType.BLOCK_END), Map.entry("endwhile", PhpTokenType.BLOCK_END),
                Map.entry("endswitch", PhpTokenType.BLOCK_END), Map.entry("enddeclare", PhpTokenType.BLOCK_END));

        private final File file;
        private final String input;
        private final int length;
        private final List<Token> tokens = new ArrayList<>();

        private int index;
        private int line;
        private int column;
        private boolean inPhp;
        private char previousChar;

        PhpTokenizer(File file, String input) {
            this.file = file;
            this.input = input;
            this.length = input.length();
            this.index = 0;
            this.line = 1;
            this.column = 1;
            this.inPhp = false;
            this.previousChar = '\0';
        }

        List<Token> tokenize() {
            while (hasNext()) {
                if (!inPhp) {
                    if (!handleOpenTag()) {
                        advance();
                    }
                    continue;
                }

                if (handleCloseTag()) {
                    continue;
                }

                if (skipWhitespaceAndComments()) {
                    continue;
                }

                if (!hasNext()) {
                    break;
                }

                char current = current();
                if (current == '\'' || current == '"' || current == '`') {
                    skipString(current);
                    continue;
                }
                if (current == '<' && peek(1) == '<' && peek(2) == '<') {
                    skipHeredoc();
                    continue;
                }
                if (current == '#' && peek(1) == '[') {
                    emitAttributeToken();
                    continue;
                }
                if (Character.isLetter(current) || current == '_') {
                    emitKeywordIfPresent();
                    continue;
                }
                if (current == '$') {
                    skipVariable();
                    continue;
                }
                if (current == '{') {
                    emitSingleCharToken(PhpTokenType.BLOCK_BEGIN);
                    continue;
                }
                if (current == '}') {
                    emitSingleCharToken(PhpTokenType.BLOCK_END);
                    continue;
                }
                if (current == '=') {
                    handleAssignment();
                    continue;
                }

                advance();
            }

            return Collections.unmodifiableList(tokens);
        }

        private boolean handleOpenTag() {
            if (!hasNext()) {
                return false;
            }
            if (matchAhead("<?php", true)) {
                emitSequenceToken(PhpTokenType.OPEN_TAG, 5);
                inPhp = true;
                return true;
            }
            if (matchAhead("<?=", false)) {
                emitSequenceToken(PhpTokenType.OPEN_TAG, 3);
                inPhp = true;
                return true;
            }
            if (matchAhead("<?", false)) {
                emitSequenceToken(PhpTokenType.OPEN_TAG, 2);
                inPhp = true;
                return true;
            }
            return false;
        }

        private boolean handleCloseTag() {
            if (matchAhead("?>", false)) {
                emitSequenceToken(PhpTokenType.CLOSE_TAG, 2);
                inPhp = false;
                return true;
            }
            return false;
        }

        private void emitKeywordIfPresent() {
            int startLine = line;
            int startColumn = column;
            int lastLine = line;
            int lastColumn = column;
            StringBuilder builder = new StringBuilder();
            while (hasNext()) {
                char c = current();
                if (!isIdentifierPart(c)) {
                    break;
                }
                builder.append(c);
                lastLine = line;
                lastColumn = column;
                advance();
            }
            if (builder.length() == 0) {
                return;
            }
            String word = builder.toString();
            PhpTokenType tokenType = KEYWORDS.get(word.toLowerCase(Locale.ROOT));
            if (tokenType != null) {
                addToken(tokenType, startLine, startColumn, lastLine, lastColumn, word.length());
            }
        }

        private boolean skipWhitespaceAndComments() {
            boolean consumed = false;
            while (hasNext()) {
                char c = current();
                if (Character.isWhitespace(c)) {
                    advance();
                    consumed = true;
                    continue;
                }
                if (c == '/' && peek(1) == '/') {
                    skipLineComment(2);
                    consumed = true;
                    continue;
                }
                if (c == '/' && peek(1) == '*') {
                    skipBlockComment();
                    consumed = true;
                    continue;
                }
                if (c == '#' && peek(1) != '[') {
                    skipLineComment(1);
                    consumed = true;
                    continue;
                }
                break;
            }
            return consumed;
        }

        private void skipString(char delimiter) {
            // consume the opening delimiter
            advance();
            while (hasNext()) {
                char c = current();
                if (c == '\\') {
                    advance();
                    if (hasNext()) {
                        advance();
                    }
                    continue;
                }
                advance();
                if (c == delimiter) {
                    break;
                }
            }
        }

        private void skipHeredoc() {
            // consume <<<
            int consumed = 0;
            while (consumed < 3 && hasNext()) {
                advance();
                consumed++;
            }

            while (hasNext() && Character.isWhitespace(current())) {
                if (current() == '\n' || current() == '\r') {
                    break;
                }
                advance();
            }

            boolean quoted = false;
            char quoteChar = '\0';
            if (current() == '\'' || current() == '"') {
                quoted = true;
                quoteChar = current();
                advance();
            }

            StringBuilder label = new StringBuilder();
            while (hasNext()) {
                char c = current();
                if (quoted) {
                    if (c == quoteChar) {
                        advance();
                        break;
                    }
                    label.append(c);
                    advance();
                } else {
                    if (c == '\n' || c == '\r' || Character.isWhitespace(c)) {
                        break;
                    }
                    label.append(c);
                    advance();
                }
            }

            while (hasNext() && current() != '\n' && current() != '\r') {
                advance();
            }
            consumeLineTerminator();

            if (label.isEmpty()) {
                return;
            }

            String terminator = label.toString();
            while (hasNext()) {
                if (startsWithTerminator(terminator)) {
                    for (int i = 0; i < terminator.length() && hasNext(); i++) {
                        advance();
                    }
                    if (current() == ';') {
                        advance();
                    }
                    while (hasNext() && current() != '\n' && current() != '\r') {
                        advance();
                    }
                    consumeLineTerminator();
                    break;
                }
                skipLine();
            }
        }

        private boolean startsWithTerminator(String terminator) {
            if (index + terminator.length() > length) {
                return false;
            }
            return input.startsWith(terminator, index);
        }

        private void consumeLineTerminator() {
            if (!hasNext()) {
                return;
            }
            char c = current();
            if (c == '\r') {
                advance();
                if (current() == '\n') {
                    advance();
                }
            } else if (c == '\n') {
                advance();
            }
        }

        private void skipLine() {
            while (hasNext()) {
                char c = current();
                advance();
                if (c == '\n') {
                    break;
                }
                if (c == '\r') {
                    if (current() == '\n') {
                        advance();
                    }
                    break;
                }
            }
        }

        private void skipVariable() {
            advance(); // skip $
            while (hasNext()) {
                char c = current();
                if (isIdentifierPart(c) || c == '$') {
                    advance();
                } else if (c == '{') { // ${var}
                    advance();
                    while (hasNext() && current() != '}') {
                        advance();
                    }
                    if (current() == '}') {
                        advance();
                    }
                    break;
                } else {
                    break;
                }
            }
        }

        private void handleAssignment() {
            if (!hasNext()) {
                return;
            }
            int startLine = line;
            int startColumn = column;
            int endLine = line;
            int endColumn = column;
            char next = peek(1);
            if (next == '=' || next == '>') {
                advance();
                return;
            }
            if (previousChar == '!' || previousChar == '=' || previousChar == '<' || previousChar == '>') {
                advance();
                return;
            }
            advance();
            addToken(PhpTokenType.ASSIGN, startLine, startColumn, endLine, endColumn, 1);
        }

        private void emitSingleCharToken(PhpTokenType type) {
            int startLine = line;
            int startColumn = column;
            int endLine = line;
            int endColumn = column;
            advance();
            addToken(type, startLine, startColumn, endLine, endColumn, 1);
        }

        private void emitSequenceToken(PhpTokenType type, int lengthToConsume) {
            int startLine = line;
            int startColumn = column;
            int endLine = line;
            int endColumn = column;
            int consumed = 0;
            while (consumed < lengthToConsume && hasNext()) {
                endLine = line;
                endColumn = column;
                advance();
                consumed++;
            }
            addToken(type, startLine, startColumn, endLine, endColumn, consumed);
        }

        private void emitAttributeToken() {
            int startLine = line;
            int startColumn = column;
            int endLine = line;
            int endColumn = column;
            advance(); // #
            if (current() == '[') {
                endLine = line;
                endColumn = column;
                advance();
            }
            addToken(PhpTokenType.ATTRIBUTE, startLine, startColumn, endLine, endColumn, 2);
        }

        private void skipLineComment(int prefixLength) {
            for (int i = 0; i < prefixLength && hasNext(); i++) {
                advance();
            }
            while (hasNext()) {
                char c = current();
                advance();
                if (c == '\n') {
                    break;
                }
                if (c == '\r') {
                    if (current() == '\n') {
                        advance();
                    }
                    break;
                }
            }
        }

        private void skipBlockComment() {
            advance(); // /
            advance(); // *
            while (hasNext()) {
                char c = current();
                advance();
                if (c == '*' && current() == '/') {
                    advance();
                    break;
                }
            }
        }

        private void addToken(PhpTokenType type, int startLine, int startColumn, int endLine, int endColumn, int length) {
            tokens.add(new Token(type, file, startLine, startColumn, endLine, endColumn, length));
        }

        private boolean matchAhead(String sequence, boolean ignoreCase) {
            if (index + sequence.length() > length) {
                return false;
            }
            if (ignoreCase) {
                return input.regionMatches(true, index, sequence, 0, sequence.length());
            }
            return input.startsWith(sequence, index);
        }

        private char current() {
            if (!hasNext()) {
                return '\0';
            }
            return input.charAt(index);
        }

        private char peek(int offset) {
            int target = index + offset;
            if (target >= length || target < 0) {
                return '\0';
            }
            return input.charAt(target);
        }

        private void advance() {
            if (!hasNext()) {
                return;
            }
            char c = input.charAt(index++);
            previousChar = c;
            if (c == '\r') {
                if (index < length && input.charAt(index) == '\n') {
                    index++;
                    previousChar = '\n';
                }
                line++;
                column = 1;
            } else if (c == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }

        private boolean hasNext() {
            return index < length;
        }

        private boolean isIdentifierPart(char c) {
            return Character.isLetterOrDigit(c) || c == '_';
        }
    }
}
