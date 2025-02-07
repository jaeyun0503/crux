package crux.ast.types;

import crux.ast.SymbolTable.Symbol;
import crux.ast.*;
import crux.ast.traversal.NullNodeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class will associate types with the AST nodes from Stage 2
 */
public final class TypeChecker {
  private final ArrayList<String> errors = new ArrayList<>();

  public ArrayList<String> getErrors() {
    return errors;
  }

  public void check(DeclList ast) {
    var inferenceVisitor = new TypeInferenceVisitor();
    inferenceVisitor.visit(ast);
  }

  /**
   * Helper function, should be used to add error into the errors array
   */
  private void addTypeError(Node n, String message) {
    errors.add(String.format("TypeError%s[%s]", n.getPosition(), message));
  }

  /**
   * Helper function, should be used to record Types if the Type is an ErrorType then it will call
   * addTypeError
   */
  private void setNodeType(Node n, Type ty) {
    ((BaseNode) n).setType(ty);
    if (ty.getClass() == ErrorType.class) {
      var error = (ErrorType) ty;
      addTypeError(n, error.getMessage());
    }
  }

  /**
   * Helper to retrieve Type from the map
   */
  public Type getType(Node n) {
    return ((BaseNode) n).getType();
  }


  /**
   * This calls will visit each AST node and try to resolve it's type with the help of the
   * symbolTable.
   */
  private final class TypeInferenceVisitor extends NullNodeVisitor<Void> {
    private Symbol currentFunctionSymbol;
    private Type currentFunctionReturnType;

    private boolean lastStatementReturns;
    private boolean hasBreak;

    @Override
    public Void visit(VarAccess vaccess) { return null; }

    @Override
    public Void visit(ArrayDecl arrayDecl) {
      ArrayType arrayType = (ArrayType) arrayDecl.getSymbol().getType();
      Type baseType = arrayType.getBase();
      if (!(baseType instanceof BoolType || baseType instanceof IntType)) {
        setNodeType(arrayDecl, new ErrorType(""));
      } else {
        setNodeType(arrayDecl, arrayType);
      }
      lastStatementReturns = false;
      return null;
    }

    @Override
    public Void visit(Assignment assignment) {
      return null;
    }

    @Override
    public Void visit(Break brk) { hasBreak = true; lastStatementReturns = false; }

    @Override
    public Void visit(FunctionCall functionCall) { return null; }

    @Override
    public Void visit(Continue cont) { return null; }

    @Override
    public Void visit(DeclList declList) {
      for (Node decl : declList.getChildren()) {
        decl.accept(this);
      }
      return null;
    }

    @Override
    public Void visit(FunctionDef functionDef) {
      currentFunctionSymbol = functionDef.getSymbol();
      FuncType functionType = (FuncType) currentFunctionSymbol.getType();
      currentFunctionReturnType = functionType.getRet();

      if (currentFunctionSymbol.getName().equals("main")) {
        if (!(currentFunctionReturnType instanceof VoidType)) {
          setNodeType(functionDef, new ErrorType(""));
        }
        if (!functionType.getArgs().isEmpty()) {
          setNodeType(functionDef, new ErrorType(""));
        }
      }

      for (Type arg : functionType.getArgs()) {
        if (!(arg instanceof BoolType || arg instanceof IntType)) {
          setNodeType(functionDef, new ErrorType(""));
        }
      }

      visit(functionDef.getStatements());

      if (!(currentFunctionReturnType instanceof VoidType) && !lastStatementReturns) {
        setNodeType(functionDef, new ErrorType(""));
      }

      currentFunctionSymbol = null;
      currentFunctionReturnType = null;
      return null;
    }

    @Override
    public Void visit(IfElseBranch ifElseBranch) { return null; }

    @Override
    public Void visit(ArrayAccess access) { return null; }

    @Override
    public Void visit(LiteralBool literalBool) { return null; }

    @Override
    public Void visit(LiteralInt literalInt) { return null; }

    @Override
    public Void visit(WhileLoop loop) { return null; }

    @Override
    public Void visit(OpExpr op) { return null; }

    @Override
    public Void visit(Return ret) { return null; }

    @Override
    public Void visit(StatementList statementList) {
      lastStatementReturns = false;
      List<Node> statements = statementList.getChildren();
      for (int i = 0; i < statements.size(); i++) {
        Statement statement = (Statement) statements.get(i);
        statement.accept(this);

        if (lastStatementReturns) {
          setNodeType(statement, new ErrorType(""));
        }
      }

      return null;
    }

    @Override
    public Void visit(VarDecl varDecl) {
      Type varType = varDecl.getSymbol().getType();
      if (!(varType instanceof BoolType || varType instanceof IntType)) {
        setNodeType(varDecl, new ErrorType(""));
      } else {
        setNodeType(varDecl, varType);
      }
      lastStatementReturns = false;
      return null;
    }
  }
}
