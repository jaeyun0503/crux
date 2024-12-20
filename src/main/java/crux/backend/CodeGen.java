package crux.backend;

import crux.ast.SymbolTable.Symbol;
import crux.ir.*;
import crux.ir.insts.*;
import crux.printing.IRValueFormatter;

import java.util.*;

/**
 * Convert the CFG into Assembly Instructions
 */
public final class CodeGen extends InstVisitor {
  private final IRValueFormatter irFormat = new IRValueFormatter();

  private final Program p;
  private final CodePrinter out;

  public CodeGen(Program p) {
    this.p = p;
    // Do not change the file name that is outputted or it will
    // break the grader!
    out = new CodePrinter("a.s");
  }

  /**
   * It should allocate space for globals call genCode for each Function
   */
  public void genCode() {
    out.close();
  }

  private HashMap<Instruction, String> currLabelMap;

  private void genCode(Function f, int[] count) {
    currLabelMap = f.assignLabels(count);
  }

  private void printInstructionInfo(Instruction i) {
    var info = String.format("/* %s */", i.format(irFormat));
    out.printCode(info);
  }

  @Override
  public void visit(AddressAt i) {}

  @Override
  public void visit(BinaryOperator i) {}

  @Override
  public void visit(CompareInst i) {}

  @Override
  public void visit(JumpInst i) {}

  @Override
  public void visit(LoadInst i) {}

  @Override
  public void visit(NopInst i) {}

  @Override
  public void visit(StoreInst i) {}

  @Override
  public void visit(ReturnInst i) {}

  @Override
  public void visit(CallInst i) {}
}
