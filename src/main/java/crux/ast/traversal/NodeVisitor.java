package crux.ast.traversal;

import crux.ast.*;

public interface NodeVisitor<T> {
  T visit(ArrayAccess arrayAccess);

  T visit(ArrayDecl arrayDecl);

  T visit(Assignment assignment);

  T visit(Break brk);

  T visit(FunctionCall functionCall);

  T visit(Continue cont);

  T visit(DeclList declList);

  T visit(FunctionDef functionDef);

  T visit(IfElseBranch ifElseBranch);

  T visit(LiteralBool literalBool);

  T visit(LiteralInt literalInt);

  T visit(WhileLoop loop);

  T visit(OpExpr operation);

  T visit(Return ret);

  T visit(StatementList statementList);

  T visit(VarAccess vaccess);

  T visit(VarDecl varDecl);
}
