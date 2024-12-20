package crux.ir;

import crux.ast.SymbolTable.Symbol;
import crux.ast.*;
import crux.ast.traversal.NodeVisitor;
import crux.ast.types.*;
import crux.ir.insts.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  public InstPair visit(DeclList declList) { return null; }


  /**
   * This visitor should create a Function instance for the functionDefinition node, add parameters
   * to the localVarMap, add the function to the program, and init the function start Instruction.
   */
  @Override
  public InstPair visit(FunctionDef functionDef) { return null; }


  @Override
  public InstPair visit(StatementList statementList) { return null; }


  /**
   * Declarations, could be either local or Global
   */
  @Override
  public InstPair visit(VarDecl varDecl) { return null; }


  /**
   * Create a declaration for array and connected it to the CFG
   */
  @Override
  public InstPair visit(ArrayDecl arrayDecl) { return null; }


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
