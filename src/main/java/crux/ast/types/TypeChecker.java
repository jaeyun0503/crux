package crux.ast.types;

import crux.ast.SymbolTable.Symbol;
import crux.ast.*;
import crux.ast.traversal.NullNodeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    public Void visit(VarAccess vaccess) {
      Type temp = vaccess.getSymbol().getType();
      setNodeType(vaccess, temp);
      return null;
    }

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
      assignment.getLocation().accept(this);
      assignment.getValue().accept(this);

      Type locationType = getType(assignment.getLocation());
      Type valueType = getType(assignment.getValue());
      Type result = locationType.assign(valueType);

      setNodeType(assignment, result);
      lastStatementReturns = false;
      return null;
    }

    @Override
    public Void visit(Break brk) {
      hasBreak = true;
      lastStatementReturns = false;
      return null;
    }

    @Override
    public Void visit(FunctionCall functionCall) {
      List<Type> argumentTypes = new ArrayList<>();
      for (Expression arg : functionCall.getArguments()) {
        arg.accept(this);
        argumentTypes.add(getType(arg));
      }

      TypeList argumentListType = new TypeList(argumentTypes);
      Type funcType = functionCall.getCallee().getType();
      Type resultType = funcType.call(argumentListType);

      setNodeType(functionCall, resultType);
      lastStatementReturns = false;
      return null;
    }

    @Override
    public Void visit(Continue cont) {
      lastStatementReturns = false;
      return null;         // TODO: Need fix
    }

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
    public Void visit(IfElseBranch ifElseBranch) {
      Expression condition = ifElseBranch.getCondition();
      condition.accept(this);

      Type conditionType = getType(condition);

      if (!(conditionType instanceof BoolType)) {
        setNodeType(ifElseBranch, new ErrorType(""));
      }

      boolean thenReturns, elseReturns;

      visit(ifElseBranch.getThenBlock());
      thenReturns = lastStatementReturns;

      visit(ifElseBranch.getElseBlock());
      elseReturns = lastStatementReturns;

      lastStatementReturns = thenReturns && elseReturns;
      return null;
    }

    @Override
    public Void visit(ArrayAccess access) {
      access.getIndex().accept(this);

      Type arrayType = access.getBase().getType();
      Type indexType = getType(access.getIndex());

      Type temp = arrayType.index(indexType);
      setNodeType(access, temp);
      return null;
    }

    @Override
    public Void visit(LiteralBool literalBool) {
      setNodeType(literalBool, new BoolType());
      return null;
    }

    @Override
    public Void visit(LiteralInt literalInt) {
      setNodeType(literalInt, new IntType());
      return null;
    }

    @Override
    public Void visit(WhileLoop loop) {
      boolean outerHasBreak = hasBreak;
      hasBreak = false;

      Expression condition = loop.getCondition();
      condition.accept(this);

      Type conditionType = getType(condition);
      if (!(conditionType instanceof BoolType)) {
        setNodeType(loop, new ErrorType(""));
      }

      visit(loop.getBody());
      hasBreak = outerHasBreak;

      return null;
    }

    @Override
    public Void visit(OpExpr op) {
      op.getLeft().accept(this);
      if (op.getRight() != null) {
        op.getRight().accept(this);
      }

      Type leftType = getType(op.getLeft());
      Type rightType = (op.getRight() == null) ? new VoidType() : getType(op.getRight());

      Type result;
      switch (op.getOp()) {
        case ADD:
          result = leftType.add(rightType);
          break;
        case SUB:
          result = leftType.sub(rightType);
          break;
        case MULT:
          result = leftType.mul(rightType);
          break;
        case DIV:
          result = leftType.div(rightType);
          break;
        case LOGIC_AND:
          result = leftType.and(rightType);
          break;
        case LOGIC_OR:
          result = leftType.or(rightType);
          break;
        case LOGIC_NOT:
          result = leftType.not();
          break;
        case GT:
        case GE:
        case LT:
        case LE:
        case EQ:
        case NE:
          result = leftType.compare(rightType);
          break;
        default:
          result = new ErrorType("Wrong operation: " + op.getOp());
      }

      setNodeType(op, result);
      return null;
    }

    @Override
    public Void visit(Return ret) {
      ret.getValue().accept(this);
      Type valueType = getType(ret.getValue());
      if (currentFunctionReturnType == null) {
        setNodeType(ret, new ErrorType("null return"));
      } else {
        Type temp = currentFunctionReturnType.assign(valueType);
        if (temp instanceof ErrorType)
          setNodeType(ret, temp);
        else
          setNodeType(ret, new VoidType());
      }
      lastStatementReturns = true;
      return null;
    }

    @Override
    public Void visit(StatementList statementList) {
      lastStatementReturns = false;
      List<Node> statements = statementList.getChildren();
      for (int i = 0; i < statements.size(); i++) {
        Statement statement = (Statement) statements.get(i);
        statement.accept(this);

        if (lastStatementReturns) {
//          setNodeType(statement, new ErrorType(""));
          break;
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
