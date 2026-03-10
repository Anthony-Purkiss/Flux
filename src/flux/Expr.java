package flux;

abstract class Expr {
  interface Visitor<R> {
    R visitBinaryExpr(Binary expr);
    R visitIdentifierExpr(Identifier expr);
  }
  
  static class Binary extends Expr {
    Binary(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator; //should always be '+'
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBinaryExpr(this);
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }

  static class Identifier extends Expr {
    Identifier(Token name) {
      this.name = name;
    }
  
    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitIdentifierExpr(this);
    }
  
    final Token name;
  }
  
  abstract <R> R accept(Visitor<R> visitor);
}
