package crux.ast.traversal;

import crux.ast.*;

public class NullNodeVisitor<T> implements NodeVisitor<T> {
  @Override
  public T visit(ArrayAccess access) {
    return null;
  }

  @Override
  public T visit(ArrayDecl arrayDecl) {
    return null;
  }

  @Override
  public T visit(Assignment assignment) {
    return null;
  }

  @Override
  public T visit(Break brk) {
    return null;
  }

  @Override
  public T visit(FunctionCall functionCall) {
    return null;
  }

  @Override
  public T visit(Continue cont) {
    return null;
  }

  @Override
  public T visit(DeclList declList) {
    return null;
  }

  @Override
  public T visit(WhileLoop loop) {
    return null;
  }

  @Override
  public T visit(FunctionDef functionDef) {
    return null;
  }

  @Override
  public T visit(IfElseBranch ifElseBranch) {
    return null;
  }

  @Override
  public T visit(LiteralBool literalBool) {
    return null;
  }

  @Override
  public T visit(LiteralInt literalInt) {
    return null;
  }

  @Override
  public T visit(OpExpr operation) {
    return null;
  }

  @Override
  public T visit(Return ret) {
    return null;
  }

  @Override
  public T visit(StatementList statementList) {
    return null;
  }

  @Override
  public T visit(VarDecl varDecl) {
    return null;
  }

  @Override
  public T visit(VarAccess vaccess) {
    return null;
  }
}
