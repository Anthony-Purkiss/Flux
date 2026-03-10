package flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static flux.TokenType.*;

class Scanner {
  private final String source;
  private final List<Token> tokens = new ArrayList<>();
  private int start = 0;
  private int current = 0;
  private int line = 1;
  private static final Map<String, TokenType> keywords;

  static {
    keywords = new HashMap<>();
    keywords.put("Dam", DAM);
    keywords.put("output", OUTPUT);
    keywords.put("River", RIVER);
    keywords.put("rainfall", RAINFALL);
  }

  Scanner(String source) {
    this.source = source;
  }

  List<Token> scanTokens() {
    while (!isAtEnd()) {
      // We are at the beginning of the next lexeme.
      start = current;
      scanToken();
    }

    tokens.add(new Token(EOF, "", null, line));
    return tokens;
  }

  private void scanToken() {
    char c = advance();
    switch (c) {
      case '(':
        addToken(LEFT_PAREN);
        break;
      case ')':
        addToken(RIGHT_PAREN);
        break;
      case ',':
        addToken(COMMA);
        break;
      case '=':
        addToken(EQUAL);
        break;
      case '+':
        addToken(PLUS);
        break;
      case ';':
        addToken(SEMICOLON);
        break;
      case '-':
        if (match('>')) {
          addToken(ARROW);
        } else {
          Flux.error(line, "Unexpected Character: '-'. Did you mean '->' for a river link?");
        }
        break;
      case '/':
        if (match('/')) {
          // A comment goes until the end of the line.
          while (peek() != '\n' && !isAtEnd())
            advance();
        } else {
          Flux.error(line, "Unexpected Character: '/'. Did you mean '//' for a comment?");
        }
        break;
      case ' ':
      case '\r':
      case '\t':
        // Ignore whitespace.
        break;

      case '\n':
        line++;
        break;

      default:
        if (isDigit(c)) {
          number();
        } else if (isAlpha(c)) {
          identifier();
        } else {
          Flux.error(line, "Unexpected character: " + c);
        }
        break;
    }
  }

  private void identifier() {
    while (isAlphaNumeric(peek()))
      advance();

    String text = source.substring(start, current);
    TokenType type = keywords.get(text);
    if (type == null)
      type = IDENTIFIER;
    addToken(type);
  }

  private void number() {
    while (isDigit(peek()))
      advance();

    String text = source.substring(start, current);
    double value = Double.parseDouble(text);
    addToken(NUMBER, value);
  }

  private boolean match(char expected) {
    if (isAtEnd())
      return false;
    if (source.charAt(current) != expected)
      return false;

    current++;
    return true;
  }

  private char peek() {
    if (isAtEnd())
      return '\0';
    return source.charAt(current);
  }

  private boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') ||
        (c >= 'A' && c <= 'Z') ||
        c == '_';
  }

  private boolean isAlphaNumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }

  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private boolean isAtEnd() {
    return current >= source.length();
  }

  private char advance() {
    return source.charAt(current++);
  }

  private void addToken(TokenType type) {
    addToken(type, null);
  }

  private void addToken(TokenType type, Object literal) {
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }
}