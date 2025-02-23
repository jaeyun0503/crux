package crux.ir;

import crux.ast.SymbolTable.Symbol;
import crux.ast.*;
import crux.ast.traversal.NodeVisitor;
import crux.ast.types.*;
import crux.ir.insts.*;

import java.util.*;

class InstPair {
  public InstPair(Instruction start, Instruction end, Value value) {
    this.start = start;
    this.end = end;
    this.value = value;
  }

  public InstPair(Instruction start, Instruction end) {
    this(start, end, null);
  }

  private Instruction start;
  private Instruction end;
  private Value value;

  public Instruction getStart() {
    return start;
  }

  public Instruction getEnd() {
    return end;
  }

  public Value getValue() {
    return value;
  }
}


/**
 * Convert AST to IR and build the CFG
 */
public final class ASTLower implements NodeVisitor<InstPair> {
  private Program mCurrentProgram = null;
  private Function mCurrentFunction = null;

  private Map<Symbol, LocalVar> mCurrentLocalVarMap = null;

  /**
   * A constructor to initialize member variables
   */
  public ASTLower() {}

  public Program lower(DeclList ast) {
    visit(ast);
    return mCurrentProgram;
  }

  @Override
  public InstPair visit(DeclList declList) {
    mCurrentProgram = new Program();
    for (var decl : declList.getChildren()) {
      decl.accept(this);
    }

    return null;
  }


  /**
   * This visitor should create a Function instance for the functionDefinition node, add parameters
   * to the localVarMap, add the function to the program, and init the function start Instruction.
   */
  @Override
  public InstPair visit(FunctionDef functionDef) {
     // prevents infinite recursion issues?
//    if (mCurrentProgram.getFunctions().hasNext()) {
//        for (Iterator<Function> it = mCurrentProgram.getFunctions(); it.hasNext(); ) {
//            Function f = it.next();
//            if (f.getName().equals(functionDef.getSymbol().getName())) {
//              return null;
//            }
//        }
//    }

    // initialize mCurrentFunction and mCurrentLocalVarMap
    mCurrentFunction = new Function(functionDef.getSymbol().getName(), (FuncType) functionDef.getType());
    mCurrentLocalVarMap = new HashMap<>();

    // for each argument, create a temp var and map its symbol to it
    List<LocalVar> paramList = new ArrayList<>();
    for (Symbol param : functionDef.getParameters()) {
      LocalVar localVar = mCurrentFunction.getTempVar(param.getType());
      paramList.add(localVar);
      mCurrentLocalVarMap.put(param, localVar);
    }

    // set the arguments for mCurrentFunction
    mCurrentFunction.setArguments(new ArrayList<>(paramList));
    // add mCurrentFunction to mCurrentProgram
    mCurrentProgram.addFunction(mCurrentFunction);

    // visit the function body
    InstPair bodyPair = functionDef.accept(this);
    // set the starting instruction of mCurrentFunction
    if (bodyPair != null) {
      mCurrentFunction.setStart(bodyPair.getStart());
    }

    // set mCurrentFunction and mCurrentLocalVarMap to null after visiting
    mCurrentFunction = null;
    mCurrentLocalVarMap = null;

    return null;
  }


  @Override
  public InstPair visit(StatementList statementList) {
    NopInst startInst = new NopInst();
    Instruction lastInst = startInst;

    for (Node stmt : statementList.getChildren()) {
      InstPair stmtPair = stmt.accept(this);

      if (stmtPair != null) {
        lastInst.setNext(0, stmtPair.getStart());
        lastInst = stmtPair.getEnd();
      }
    }

    return new InstPair(startInst, lastInst);
  }


  /**
   * Declarations, could be either local or Global
   */
  @Override
  public InstPair visit(VarDecl varDecl) {
    if (mCurrentFunction == null) {
      IntegerConstant size = IntegerConstant.get(mCurrentProgram, 1);
      GlobalDecl globalDecl = new GlobalDecl(varDecl.getSymbol(), size);
      mCurrentProgram.addGlobalVar(globalDecl);
    } else {
      LocalVar localVar = mCurrentFunction.getTempVar(varDecl.getType());
      mCurrentLocalVarMap.put(varDecl.getSymbol(), localVar);
    }

    NopInst nop = new NopInst();
    return new InstPair(nop, nop);
  }


  /**
   * Create a declaration for array and connected it to the CFG
   */
  @Override
  public InstPair visit(ArrayDecl arrayDecl) {
    ArrayType array = (ArrayType) arrayDecl.getSymbol().getType();
    IntegerConstant size = IntegerConstant.get(mCurrentProgram, array.getExtent());
    GlobalDecl globalDecl = new GlobalDecl(arrayDecl.getSymbol(), size);
    mCurrentProgram.addGlobalVar(globalDecl);

    NopInst nop = new NopInst();
    return new InstPair(nop, nop);
  }


  /**
   * LookUp the name in the map(s). For globals, we should do a load to get the value to load into a
   * LocalVar.
   */
  @Override
  public InstPair visit(VarAccess name) { return null; }


  /**
   * If the location is a VarAccess to a LocalVar, copy the value to it. If the location is a
   * VarAccess to a global, store the value. If the location is ArrayAccess, store the value.
   */
  @Override
  public InstPair visit(Assignment assignment) { return null; }

  /**
   * Lower a FunctionCall.
   */
  @Override
  public InstPair visit(FunctionCall functionCall) { return null; }


  /**
   * to Handle Operations like Arithmetics and Comparisons Also to handle logical operations (and,
   * or, not)
   */
  @Override
  public InstPair visit(OpExpr operation) { return null; }


  /**
   * It should compute the address into the array, do the load, and return the value in a LocalVar.
   */
  @Override
  public InstPair visit(ArrayAccess access) { return null; }


  /**
   * Copy the literal into a tempVar
   */
  @Override
  public InstPair visit(LiteralBool literalBool) { return null; }


  /**
   * Copy the literal into a tempVar
   */
  @Override
  public InstPair visit(LiteralInt literalInt) { return null; }


  /**
   * Lower a Return.
   */
  @Override
  public InstPair visit(Return ret) { return null; }


  /**
   * Break Node
   */
  @Override
  public InstPair visit(Break brk) { return null; }


  /**
   * Continue Node
   */
  @Override
  public InstPair visit(Continue cnt) { return null; }


  /**
   * Control Structures Make sure to correctly connect the branches and the join block
   */
  @Override
  public InstPair visit(IfElseBranch ifElseBranch) { return null; }


  /**
   * Loop Structures Make sure to correctly connect the loop condition to the body
   */
  @Override
  public InstPair visit(WhileLoop loop) { return null; }
}
