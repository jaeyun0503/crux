package crux.ir;

import crux.ast.SymbolTable.Symbol;
import crux.ast.*;
import crux.ast.traversal.NodeVisitor;
import crux.ast.types.*;
import crux.ir.insts.*;

import java.beans.Expression;
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
    InstPair bodyPair = functionDef.getStatements().accept(this);
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
      LocalVar localVar = mCurrentFunction.getTempVar(varDecl.getSymbol().getType());
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
  public InstPair visit(VarAccess name) {
    Symbol symbol = name.getSymbol();
    if (mCurrentLocalVarMap.containsKey(symbol)) {    // Local
      LocalVar src = mCurrentLocalVarMap.get(symbol);
      Instruction inst = new NopInst();
      return new InstPair(inst, inst, src);
    } else {     // Global
      LocalVar temp = mCurrentFunction.getTempVar(symbol.getType());
      AddressVar addr = mCurrentFunction.getTempAddressVar(symbol.getType());
      AddressAt addrAt = new AddressAt(addr, symbol);
      LoadInst load = new LoadInst(temp, addr);
      addrAt.setNext(0, load);
      return new InstPair(addrAt, load, temp);
    }
  }


  /**
   * If the location is a VarAccess to a LocalVar, copy the value to it. If the location is a
   * VarAccess to a global, store the value. If the location is ArrayAccess, store the value.
   */
  @Override
  public InstPair visit(Assignment assignment) {
    Node target = assignment.getLocation();

    if (target instanceof VarAccess) {
      VarAccess varAccess = (VarAccess) target;
      Symbol sym = varAccess.getSymbol();

      if (mCurrentLocalVarMap.containsKey(sym)) {
        InstPair rhsPair = assignment.getValue().accept(this);
        LocalVar dest = mCurrentLocalVarMap.get(sym);
        CopyInst copyInst = new CopyInst(dest, rhsPair.getValue());
        rhsPair.getEnd().setNext(0, copyInst);
        return new InstPair(rhsPair.getStart(), copyInst);
      } else {
        AddressVar addr = mCurrentFunction.getTempAddressVar(sym.getType());
        AddressAt addrAt = new AddressAt(addr, sym);
        InstPair rhsPair = assignment.getValue().accept(this);
        addrAt.setNext(0, rhsPair.getStart());
        StoreInst storeInst = new StoreInst((LocalVar) rhsPair.getValue(), addr);
        rhsPair.getEnd().setNext(0, storeInst);
        return new InstPair(addrAt, storeInst);
      }
    } else if (target instanceof ArrayAccess) {
      ArrayAccess arrayAccess = (ArrayAccess) target;
      InstPair indexPair = ((Node) arrayAccess.getIndex()).accept(this);
      InstPair rhsPair = assignment.getValue().accept(this);
      Type baseType = ((ArrayType) arrayAccess.getBase().getType()).getBase();
      AddressVar addrVar = mCurrentFunction.getTempAddressVar(baseType);
      AddressAt addrAt = new AddressAt(addrVar, arrayAccess.getBase(), (LocalVar) indexPair.getValue());
      indexPair.getEnd().setNext(0, addrAt);
      addrAt.setNext(0, rhsPair.getStart());
      StoreInst storeInst = new StoreInst((LocalVar) rhsPair.getValue(), addrVar);
      rhsPair.getEnd().setNext(0, storeInst);
      return new InstPair(indexPair.getStart(), storeInst);
    }

    return null;
  }

  /**
   * Lower a FunctionCall.
   */
  @Override
  public InstPair visit(FunctionCall functionCall) {
    List<LocalVar> argValues = new ArrayList<>();
    Instruction start = null;
    Instruction last = null;

    for (Node arg : functionCall.getArguments()) {
      InstPair argPair = arg.accept(this);
      if (start == null) {
        start = argPair.getStart();
      } else {
        last.setNext(0, argPair.getStart());
      }
      last = argPair.getEnd();
      argValues.add((LocalVar) argPair.getValue());
    }
    LocalVar result = mCurrentFunction.getTempVar(functionCall.getType());
    CallInst callInst = new CallInst(result, functionCall.getCallee(), argValues);
    if (last != null) {
      last.setNext(0, callInst);
      return new InstPair(start, callInst, result);
    } else {
      return new InstPair(callInst, callInst, result);
    }
  }


  /**
   * to Handle Operations like Arithmetics and Comparisons Also to handle logical operations (and,
   * or, not)
   */
  @Override
  public InstPair visit(OpExpr operation) {
    OpExpr.Operation op = operation.getOp();
    if (op == OpExpr.Operation.LOGIC_NOT) {
      InstPair operandPair = operation.getLeft().accept(this);
      LocalVar result = mCurrentFunction.getTempVar(operation.getType());
      UnaryNotInst notInst = new UnaryNotInst(result, (LocalVar) operandPair.getValue());
      operandPair.getEnd().setNext(0, notInst);
      return new InstPair(operandPair.getStart(), notInst, result);
    } else if (op == OpExpr.Operation.LOGIC_OR) {
      InstPair leftPair = operation.getLeft().accept(this);
      LocalVar result = mCurrentFunction.getTempVar(operation.getType());
      JumpInst jump = new JumpInst((LocalVar) leftPair.getValue());
      leftPair.getEnd().setNext(0, jump);

      NopInst thenBlock = new NopInst();
      CopyInst copyTrue = new CopyInst(result, BooleanConstant.get(mCurrentProgram, true));
      thenBlock.setNext(0, copyTrue);

      InstPair rightPair = operation.getRight().accept(this);
      CopyInst copyRight = new CopyInst(result, (LocalVar) rightPair.getValue());
      rightPair.getEnd().setNext(0, copyRight);

      NopInst join = new NopInst();
      jump.setNext(1, thenBlock);
      jump.setNext(0, rightPair.getStart());

      copyTrue.setNext(0, join);
      copyRight.setNext(0, join);
      return new InstPair(leftPair.getStart(), join, result);
    } else if (op == OpExpr.Operation.LOGIC_AND) {
      InstPair leftPair = operation.getLeft().accept(this);
      LocalVar result = mCurrentFunction.getTempVar(operation.getType());
      JumpInst jump = new JumpInst((LocalVar) leftPair.getValue());
      leftPair.getEnd().setNext(0, jump);

      InstPair rightPair = operation.getRight().accept(this);
      CopyInst copyRight = new CopyInst(result, (LocalVar) rightPair.getValue());
      rightPair.getEnd().setNext(0, copyRight);

      NopInst elseBlock = new NopInst();
      CopyInst copyFalse = new CopyInst(result, BooleanConstant.get(mCurrentProgram, false));
      elseBlock.setNext(0, copyFalse);

      NopInst join = new NopInst();
      jump.setNext(1, rightPair.getStart());
      jump.setNext(0, elseBlock);

      copyRight.setNext(0, join);
      copyFalse.setNext(0, join);
      return new InstPair(leftPair.getStart(), join, result);

    } else {
      InstPair leftPair = operation.getLeft().accept(this);
      InstPair rightPair = operation.getRight().accept(this);
      leftPair.getEnd().setNext(0, rightPair.getStart());
      LocalVar result = mCurrentFunction.getTempVar(operation.getType());

      if (op == OpExpr.Operation.ADD || op == OpExpr.Operation.SUB || op == OpExpr.Operation.MULT || op == OpExpr.Operation.DIV) {
        BinaryOperator.Op temp;
        if (op == OpExpr.Operation.ADD) {temp = BinaryOperator.Op.Add;}
        else if (op == OpExpr.Operation.SUB) {temp = BinaryOperator.Op.Sub;}
        else if (op == OpExpr.Operation.MULT) {temp = BinaryOperator.Op.Mul;}
        else {temp = BinaryOperator.Op.Div;}
        BinaryOperator binOp = new BinaryOperator(temp, result, (LocalVar) leftPair.getValue(), (LocalVar) rightPair.getValue());
        rightPair.getEnd().setNext(0, binOp);
        return new InstPair(leftPair.getStart(), binOp, result);
      } else {
        CompareInst.Predicate temp;
        if (op == OpExpr.Operation.GE) {temp = CompareInst.Predicate.GE;}
        else if (op == OpExpr.Operation.GT) {temp = CompareInst.Predicate.GT;}
        else if (op == OpExpr.Operation.LE) {temp = CompareInst.Predicate.LE;}
        else if (op == OpExpr.Operation.LT) {temp = CompareInst.Predicate.LT;}
        else if (op == OpExpr.Operation.EQ) {temp = CompareInst.Predicate.EQ;}
        else {temp = CompareInst.Predicate.NE;}

        CompareInst cmpInst = new CompareInst(result, temp, (LocalVar) leftPair.getValue(), (LocalVar) rightPair.getValue());
        rightPair.getEnd().setNext(0, cmpInst);
        return new InstPair(leftPair.getStart(), cmpInst, result);
      }
    }
  }


  /**
   * It should compute the address into the array, do the load, and return the value in a LocalVar.
   */
  @Override
  public InstPair visit(ArrayAccess access) {
    Type arrayType = ((ArrayType)access.getBase().getType()).getBase();
    AddressVar baseAddr = mCurrentFunction.getTempAddressVar(arrayType);
    LocalVar localVar = mCurrentFunction.getTempVar(arrayType);
    InstPair indexPair = access.getIndex().accept(this);
    AddressAt addrAt = new AddressAt(baseAddr, access.getBase(), (LocalVar) indexPair.getValue());
    indexPair.getEnd().setNext(0, addrAt);
    LoadInst loadInst = new LoadInst(localVar, baseAddr);
    addrAt.setNext(0, loadInst);
    return new InstPair(indexPair.getStart(), loadInst, localVar);
  }


  /**
   * Copy the literal into a tempVar
   */
  @Override
  public InstPair visit(LiteralBool literalBool) {
    LocalVar result = mCurrentFunction.getTempVar(literalBool.getType());
    BooleanConstant constant = BooleanConstant.get(mCurrentProgram, literalBool.getValue());
    CopyInst copy = new CopyInst(result, constant);
    return new InstPair(copy, copy, result);
  }


  /**
   * Copy the literal into a tempVar
   */
  @Override
  public InstPair visit(LiteralInt literalInt) {
    LocalVar result = mCurrentFunction.getTempVar(literalInt.getType());
    IntegerConstant constant = IntegerConstant.get(mCurrentProgram, literalInt.getValue());
    CopyInst copy = new CopyInst(result, constant);
    return new InstPair(copy, copy, result);
  }


  /**
   * Lower a Return.
   */
  @Override
  public InstPair visit(Return ret) {
    if (ret.getValue() != null) {
      InstPair exprPair = ret.getValue().accept(this);
      ReturnInst retInst = new ReturnInst((LocalVar) exprPair.getValue());
      exprPair.getEnd().setNext(0, retInst);
      return new InstPair(exprPair.getStart(), retInst);
    } else {
      ReturnInst retInst = new ReturnInst(null);
      return new InstPair(retInst, retInst);
    }
  }


  /**
   * Break Node
   */
  @Override
  public InstPair visit(Break brk) {
    LocalVar trueVar = mCurrentFunction.getTempVar(new BoolType());
    BooleanConstant trueConst = BooleanConstant.get(mCurrentProgram, true);
    CopyInst copyInst = new CopyInst(trueVar, trueConst);
    JumpInst jump = new JumpInst(trueVar);
    copyInst.setNext(0, jump);
    return new InstPair(copyInst, jump);
  }


  /**
   * Continue Node
   */
  @Override
  public InstPair visit(Continue cnt) {
    LocalVar trueVar = mCurrentFunction.getTempVar(new BoolType());
    BooleanConstant trueConst = BooleanConstant.get(mCurrentProgram, true);
    CopyInst copyInst = new CopyInst(trueVar, trueConst);
    JumpInst jump = new JumpInst(trueVar);
    copyInst.setNext(0, jump);
    return new InstPair(copyInst, jump);
  }


  /**
   * Control Structures Make sure to correctly connect the branches and the join block
   */
  @Override
  public InstPair visit(IfElseBranch ifElseBranch) {
    InstPair condPair = ifElseBranch.getCondition().accept(this);
    JumpInst jump = new JumpInst((LocalVar) condPair.getValue());
    condPair.getEnd().setNext(0, jump);
    InstPair thenPair = ifElseBranch.getThenBlock().accept(this);
    jump.setNext(1, thenPair.getStart());

    InstPair elsePair = null;
    if (ifElseBranch.getElseBlock() != null) {
      elsePair = ifElseBranch.getElseBlock().accept(this);
      jump.setNext(0, elsePair.getStart());
    } else {
      Instruction noElseBlock = new NopInst();
      jump.setNext(0, noElseBlock);
      elsePair = new InstPair(noElseBlock, noElseBlock);
    }

    NopInst join = new NopInst();
    thenPair.getEnd().setNext(0, join);
    elsePair.getEnd().setNext(0, join);

    return new InstPair(condPair.getStart(), join);
  }


  /**
   * Loop Structures Make sure to correctly connect the loop condition to the body
   */
  @Override
  public InstPair visit(WhileLoop loop) {
    NopInst condStart = new NopInst();
    InstPair condPair = loop.getCondition().accept(this);
    condStart.setNext(0, condPair.getStart());

    JumpInst jumpCond = new JumpInst((LocalVar) condPair.getValue());
    condPair.getEnd().setNext(0, jumpCond);
    NopInst join = new NopInst();

    InstPair bodyPair = loop.getBody().accept(this);
    bodyPair.getEnd().setNext(0, condStart);
    jumpCond.setNext(1, bodyPair.getStart());
    jumpCond.setNext(0, join);

    return new InstPair(condStart, join);
  }
}
