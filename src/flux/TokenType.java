package flux;

enum TokenType {
  // Single-character tokens.
  EQUAL, PLUS, SEMICOLON,
  LEFT_PAREN, RIGHT_PAREN, COMMA,

  // Two character tokens.
  ARROW, // '->'

  // Literals.
  IDENTIFIER, NUMBER,

  // Keywords.
  DAM, OUTPUT, RIVER, RAINFALL,

  EOF
}