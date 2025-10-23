package de.jplag.php;

import de.jplag.TokenType;

/**
 * Tokens produced by the PHP language module.
 */
public enum PhpTokenType implements TokenType {
    OPEN_TAG("PHP_TAG{"),
    CLOSE_TAG("}PHP_TAG"),
    NAMESPACE("NAMESPACE"),
    USE("USE"),
    CLASS("CLASS"),
    INTERFACE("INTERFACE"),
    TRAIT("TRAIT"),
    ENUM("ENUM"),
    EXTENDS("EXTENDS"),
    IMPLEMENTS("IMPLEMENTS"),
    FUNCTION("FUNCTION"),
    IF("IF"),
    ELSEIF("ELSEIF"),
    ELSE("ELSE"),
    FOR("FOR"),
    FOREACH("FOREACH"),
    WHILE("WHILE"),
    DO("DO"),
    SWITCH("SWITCH"),
    CASE("CASE"),
    DEFAULT("DEFAULT"),
    TRY("TRY"),
    CATCH("CATCH"),
    FINALLY("FINALLY"),
    RETURN("RETURN"),
    BREAK("BREAK"),
    CONTINUE("CONTINUE"),
    THROW("THROW"),
    NEW("NEW"),
    INCLUDE("INCLUDE"),
    REQUIRE("REQUIRE"),
    ECHO("ECHO"),
    PRINT("PRINT"),
    GLOBAL("GLOBAL"),
    STATIC("STATIC"),
    CONST("CONST"),
    YIELD("YIELD"),
    MATCH("MATCH"),
    BLOCK_BEGIN("BLOCK{"),
    BLOCK_END("}BLOCK"),
    ASSIGN("ASSIGN"),
    ATTRIBUTE("ATTRIBUTE");

    private final String description;

    PhpTokenType(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
