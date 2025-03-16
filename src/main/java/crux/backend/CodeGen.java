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

  private HashMap<Variable, Integer> varIndexes = new HashMap<Variable, Integer>();
  private HashMap<Instruction, String> currLabelMap;
  private int varIndex;

  Integer getStackIndex(Variable var) {
    if (!varIndexes.containsKey(var)) {
      varIndex++;
      varIndexes.put(var, varIndex);
    }
    return varIndexes.get(var);
  }

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
    for (Iterator<GlobalDecl> globalIterator = p.getGlobals(); globalIterator.hasNext();) {
      GlobalDecl globalDecl = globalIterator.next();
      out.printCode(".comm" + globalDecl.getSymbol().getName() + "," + globalDecl.getNumElement().getValue()*8 + ", 8");
    }

    int count[] = new int[1];
    for (Iterator<Function> functionIterator = p.getFunctions(); functionIterator.hasNext();) {
      Function function = functionIterator.next();
      genCode(function, count);
    }

    out.close();
  }

  private void genCode(Function f, int[] count) {
    out.printCode(".globl " + f.getName());
    out.printLabel(f.getName() + ":");

    int totalSlots = f.getNumTempVars() + f.getNumTempAddressVars();
    totalSlots += (totalSlots % 2 == 1) ? 1 : 0;
    out.printCode("enter $(8 * " + totalSlots + "), $0");

    List<LocalVar> args = f.getArguments();
    String[] argRegisters = {"%rdi", "%rsi", "%rdx", "%rcx", "%r8", "%r9"};

    for (int idx = 0; idx < args.size(); idx++) {
      int slot = getStackIndex(args.get(idx));
      out.printCode(String.format("movq %s, -%d(%%rbp)", argRegisters[idx], slot * 8));
    }

    currLabelMap = f.assignLabels(count);
    if (f.getStart() != null) {
      Set<Instruction> visited = new HashSet<>();
      Deque<Instruction> worklist = new ArrayDeque<>();
      worklist.push(f.getStart());

      while (!worklist.isEmpty()) {
        Instruction inst = worklist.pop();

        if (!visited.add(inst)) continue;

        String label = currLabelMap.get(inst);
        if (label != null) {
          out.printLabel(label + ":");
        }

        inst.accept(this);

        for (int i = inst.numNext() - 1; i >= 0; i--) {
          worklist.push(inst.getNext(i));
        }
      }
    }

    out.printCode("leave");
    out.printCode("ret");
  }


  private void printInstructionInfo(Instruction i) {
    var info = String.format("/* %s */", i.format(irFormat));
    out.printCode(info);
  }

  @Override
  public void visit(AddressAt i) {
    AddressVar dest = i.getDst();
    Symbol base = i.getBase();
    LocalVar offset = i.getOffset();
    int destSlot = getStackIndex(dest);

    out.printCode(String.format("movq %s@GOTPCREL(%%rip), %%r11", base.getName()));

    if (offset != null) {
      int offsetSlot = getStackIndex(offset);
      out.printCode(String.format("movq -%d(%%rbp), %%r10", offsetSlot * 8));
      out.printCode("imulq $8, %r10");
      out.printCode("addq %r10, %r11");
    }

    out.printCode(String.format("movq %%r11, -%d(%%rbp)", destSlot * 8));
  }

  @Override
  public void visit(BinaryOperator i) {
    LocalVar dest = i.getDst();
    int destSlot = getStackIndex(dest);

    LocalVar left = i.getLeftOperand();
    int lhsSlot = getStackIndex(left);

    LocalVar right = i.getRightOperand();
    int rhsSlot = getStackIndex(right);

    out.printCode(String.format("movq -%d(%%rbp), %%r10", lhsSlot * 8));

    switch (i.getOperator()) {
      case Add:
        out.printCode(String.format("addq -%d(%%rbp), %%r10", rhsSlot * 8));
        break;
      case Sub:
        out.printCode(String.format("subq -%d(%%rbp), %%r10", rhsSlot * 8));
        break;
      case Mul:
        out.printCode(String.format("imulq -%d(%%rbp), %%r10", rhsSlot * 8));
        break;
      case Div:
        out.printCode("movq %r10, %rax");
        out.printCode("cqto");
        out.printCode(String.format("idivq -%d(%%rbp)", rhsSlot * 8));
        out.printCode("movq %rax, %r10");
        break;
    }

    out.printCode(String.format("movq %%r10, -%d(%%rbp)", destSlot * 8));
  }

  @Override
  public void visit(CompareInst i) {
    LocalVar dest = i.getDst();
    int destSlot = getStackIndex(dest);
    LocalVar left = i.getLeftOperand();
    int lhsSlot = getStackIndex(left);
    LocalVar right = i.getRightOperand();
    int rhsSlot = getStackIndex(right);
    CompareInst.Predicate predicate = i.getPredicate();

    out.printCode(String.format("movq -%d(%%rbp), %%r10", lhsSlot * 8));
    out.printCode(String.format("movq -%d(%%rbp), %%r11", rhsSlot * 8));

    out.printCode("cmpq %r11, %r10");

    switch (predicate) {
      case EQ: out.printCode("sete %al"); break;
      case NE: out.printCode("setne %al"); break;
      case LT: out.printCode("setl %al"); break;
      case LE: out.printCode("setle %al"); break;
      case GT: out.printCode("setg %al"); break;
      case GE: out.printCode("setge %al"); break;
    }

    out.printCode("movzbq %al, %rax");
    out.printCode(String.format("movq %%rax, -%d(%%rbp)", destSlot * 8));
  }

  @Override
  public void visit(JumpInst i) {
    LocalVar predicate = i.getPredicate();
    int predSlot = getStackIndex(predicate);

    out.printCode(String.format("movq -%d(%%rbp), %%r10", predSlot * 8));

    out.printCode("cmpq $1, %r10");

    Instruction trueTarget = i.getNext(0);
    Instruction falseTarget = i.getNext(1);

    String trueLabel = currLabelMap.get(trueTarget);
    String falseLabel = currLabelMap.get(falseTarget);

    out.printCode(String.format("je %s", trueLabel));
    out.printCode(String.format("jmp %s", falseLabel));
  }

  @Override
  public void visit(LoadInst i) {
    LocalVar destination = i.getDst();
    AddressVar sourceAddress = i.getSrcAddress();
    int destinationSlot = getStackIndex(destination);

    out.printCode(String.format("movq 0(%s), %%rax", sourceAddress.getName()));
    out.printCode(String.format("movq %%rax, -%d(%%rbp)", destinationSlot * 8));
  }

  @Override
  public void visit(NopInst i) {
    out.printCode("nop");
  }

  @Override
  public void visit(StoreInst i) {
    LocalVar srcValue = i.getSrcValue();
    AddressVar destAddress = i.getDestAddress();
    int slot = getStackIndex(srcValue);

    out.printCode(String.format("movq -%d(%%rbp), %%rax", slot * 8));
    out.printCode(String.format("movq %%rax, (%s)", destAddress.getName()));
  }

  @Override
  public void visit(ReturnInst i) {
    LocalVar returnValue = i.getReturnValue();
    if (returnValue != null) {
      int slot = getStackIndex(returnValue);
      out.printCode(String.format("movq -%d(%%rbp), %%rax", slot * 8));
    }

    out.printCode("leave");
    out.printCode("ret");
  }

  @Override
  public void visit(CallInst i) {
    Symbol callee = i.getCallee();
    List<LocalVar> params = i.getParams();

    String[] argRegisters = {"%rdi", "%rsi", "%rdx", "%rcx", "%r8", "%r9"};

    int numParams = params.size();
    for (int idx = 0; idx < Math.min(numParams, argRegisters.length); idx++) {
      int slot = getStackIndex(params.get(idx));
      out.printCode(String.format("movq -%d(%%rbp), %s", slot * 8, argRegisters[idx]));
    }

    for (int idx = numParams - 1; idx >= argRegisters.length; idx--) {
      int slot = getStackIndex(params.get(idx));
      out.printCode(String.format("movq -%d(%%rbp), %%rax", slot * 8));
      out.printCode("pushq %rax");
    }

    out.printCode("call " + callee.getName());

    if (numParams > argRegisters.length) {
      int numPushed = numParams - argRegisters.length;
      out.printCode(String.format("addq $%d, %%rsp", numPushed * 8));
    }
  }

  @Override
  public void visit(UnaryNotInst i) {
    LocalVar dest = i.getDst();
    LocalVar operand = i.getInner();

    int destSlot = getStackIndex(dest);
    int operandSlot = getStackIndex(operand);

    out.printCode(String.format("movq -%d(%%rbp), %%rax", operandSlot * 8));
    out.printCode("movq $1, %r10");
    out.printCode("subq %rax, %r10");
    out.printCode(String.format("movq %%r10, -%d(%%rbp)", destSlot * 8));
  }

  @Override
  public void visit(CopyInst i) {
    LocalVar destination = i.getDstVar();
    Value source = i.getSrcValue();
    int destSlot = getStackIndex(destination);

    if (source instanceof LocalVar) {
      int srcSlot = getStackIndex((LocalVar) source);
      out.printCode(String.format("movq -%d(%%rbp), %%rax", srcSlot * 8));
    } else if (source instanceof IntegerConstant) {
      long value = ((IntegerConstant) source).getValue();
      out.printCode(String.format("movq $%d, %%rax", value));
    } else if (source instanceof BooleanConstant) {
      boolean boolValue = ((BooleanConstant) source).getValue();
      int value = boolValue ? 1 : 0;
      out.printCode(String.format("movq $%d, %%rax", value));
    }

    out.printCode(String.format("movq %%rax, -%d(%%rbp)", destSlot * 8));
  }
}
