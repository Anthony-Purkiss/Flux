package flux;

import java.util.ArrayList;
import java.util.List;

import static flux.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    private Expr expression() {
        return addition();
    }

    private Expr addition() {
        Expr expr = identifier(); // start with a single identifier

        while (match(PLUS)) {
            Token operator = previous();
            Expr right = identifier();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr identifier() {
        return new Expr.Identifier(consume(IDENTIFIER, "Expected a river name."));
    }

    private Stmt declaration() {
        try {
            if (match(RAINFALL))
                return rainfallStmt();
            if (match(RIVER))
                return riverDecl();
            if (match(DAM))
                return damDecl();
            return statement(); // if it's not a declaration, it must be a statement
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt statement() {
        if (match(OUTPUT))
            return outputStmt();
        if (check(IDENTIFIER) && checkNext(ARROW))
            return linkStmt();
        if (check(IDENTIFIER) && checkNext(EQUAL))
            return mergeStmt();

        throw error(peek(), "Expected a statement.");
    }

    private Stmt rainfallStmt() {
        consume(LEFT_PAREN, "Expected '(' after 'rainfall'.");
        List<Double> values = new ArrayList<>();
        values.add(parseNumber("Expected number in rainfall statement."));
        while (match(COMMA)) {
            values.add(parseNumber("Expected number after ','."));
        }
        consume(RIGHT_PAREN, "Expected ')' after rainfall values.");
        consume(SEMICOLON, "Expected ';' after rainfall statement.");
        return new Stmt.Rainfall(values);
    }

    private Stmt riverDecl() {
        Token name = consume(IDENTIFIER, "Expected river name after 'River'.");
        consume(SEMICOLON, "Expected ';' after river declaration.");
        return new Stmt.RiverDecl(name);
    }

    private Stmt damDecl() {
        Token name = consume(IDENTIFIER, "Expected dam name after 'Dam'.");
        consume(LEFT_PAREN, "Expected '(' after dam name.");

        double threshold = parseNumber("Expected threshold value.");
        consume(COMMA, "Expected ',' after threshold value.");
        double minFlow = parseNumber("Expected minimum outflow percentage.");
        consume(COMMA, "Expected ',' after minimum outflow percentage.");
        double maxFlow = parseNumber("Expected maximum outflow percentage.");

        consume(RIGHT_PAREN, "Expected ')' after dam parameters.");
        consume(SEMICOLON, "Expected ';' after dam declaration.");
        return new Stmt.DamDecl(name, threshold, minFlow, maxFlow);
    }

    private Stmt linkStmt() {
        Token from = consume(IDENTIFIER, "Expected river or dam name at start of link.");
        consume(ARROW, "Expected '->' in link statement.");
        Token to = consume(IDENTIFIER, "Expected river or dam name at end of link.");
        consume(SEMICOLON, "Expected ';' after link statement.");
        return new Stmt.Link(from, to);
    }

    private Stmt mergeStmt() {
        Token name = consume(IDENTIFIER, "Expected river name at start of merge.");
        consume(EQUAL, "Expected '=' in merge statement.");
        Expr value = expression(); // goes to addition() method.
        consume(SEMICOLON, "Expected ';' after merge statement");
        return new Stmt.Merge(name, value);
    }

    private Stmt outputStmt() {
        Token name = consume(IDENTIFIER, "Expected river name after 'output'.");
        consume(SEMICOLON, "Expected ';' after output statement.");
        return new Stmt.Output(name);
    }

    private double parseNumber(String message) {
        Token number = consume(NUMBER, message);
        return (double) number.literal;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type))
            return advance();

        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd())
            return false;
        return peek().type == type;
    }

    private boolean checkNext(TokenType type) {
        if (current + 1 >= tokens.size())
            return false;
        return tokens.get(current + 1).type == type;
    }

    private Token advance() {
        if (!isAtEnd())
            current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Flux.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON)
                return;

            switch (peek().type) {
                case RAINFALL:
                case RIVER:
                case DAM:
                case OUTPUT:
                    return;
                default:
                    // ignore all other tokens
            }

            advance();
        }
    }
}