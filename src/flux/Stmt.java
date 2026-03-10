package flux;

import java.util.List;

abstract class Stmt {
  interface Visitor<R> {
    R visitRainfallStmt(Rainfall stmt);

    R visitRiverDeclStmt(RiverDecl stmt);

    R visitDamDeclStmt(DamDecl stmt);

    R visitLinkStmt(Link stmt);

    R visitMergeStmt(Merge stmt);

    R visitOutputStmt(Output stmt);
  }

  static class Rainfall extends Stmt {
    Rainfall(List<Double> values) {
      this.values = values;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitRainfallStmt(this);
    }

    final List<Double> values;
  }

  static class RiverDecl extends Stmt {
    RiverDecl(Token name) {
      this.name = name;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitRiverDeclStmt(this);
    }

    final Token name;
  }

  static class DamDecl extends Stmt {
    DamDecl(Token name, double threshold, double minFlowRate, double maxFlowRate) {
      this.name = name;
      this.threshold = threshold;
      this.minFlowRate = minFlowRate;
      this.maxFlowRate = maxFlowRate;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitDamDeclStmt(this);
    }

    final Token name;
    final double threshold;
    final double minFlowRate;
    final double maxFlowRate;
  }

  static class Link extends Stmt {
    Link(Token from, Token to) {
      this.from = from;
      this.to = to;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLinkStmt(this);
    }

    final Token from;
    final Token to;
  }

  static class Merge extends Stmt {
    Merge(Token name, Expr expression) {
      this.name = name;
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitMergeStmt(this);
    }

    final Token name;
    final Expr expression;
  }

  static class Output extends Stmt {
    Output(Token name) {
      this.name = name;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitOutputStmt(this);
    }

    final Token name;
  }

  abstract <R> R accept(Visitor<R> visitor);
}
