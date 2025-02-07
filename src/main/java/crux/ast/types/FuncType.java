package crux.ast.types;

import java.util.Iterator;

/**
 * The field args is a TypeList with a type for each param. The type ret is the type of the function
 * return. The function return could be int, bool, or void. This class should implement the call
 * method.
 */
public final class FuncType extends Type implements java.io.Serializable {
  static final long serialVersionUID = 12022L;

  private TypeList args;
  private Type ret;

  public FuncType(TypeList args, Type returnType) {
    this.args = args;
    this.ret = returnType;
  }

  public Type getRet() {
    return ret;
  }

  public TypeList getArgs() {
    return args;
  }

  @Override
  Type call(Type args) {
    if (!(args instanceof TypeList)) {
      return new ErrorType("Arguments need to be a TypeList. Check " + args);
    }
    TypeList actualArgs = (TypeList) args;

    if (this.args.size() != actualArgs.size()) {
      return new ErrorType(String.format(
              "Wrong number of arguments. Expected %d but got %d.",
              this.args.size(), actualArgs.size()
      ));
    }
    Iterator<Type> expected = this.args.iterator();
    Iterator<Type> actual = actualArgs.iterator();

    for (int x = 0; x < this.args.size(); x++) {
      Type expectedType = expected.next();
      Type actualType = actual.next();

      if (!(expectedType.equivalent(actualType))) {
        return new ErrorType("Mismatch in Types of Argument. Expected " + expectedType + " but got " +
                actualType);
      }
    }
    return ret;
  }

  @Override
  public boolean equivalent(Type that) {
    if (!(that instanceof FuncType)) {
      return false;
    }
    FuncType temp = (FuncType) that;
    return temp.getRet().equivalent(this.getRet()) && temp.getArgs().equivalent(this.getArgs());
  }

  @Override
  public String toString() {
    return "func(" + args + "):" + ret;
  }
}
