package crux.ast.types;

/**
 * The variable base is the type of the array element. This could be int or bool. The extent
 * variable is number of elements in the array.
 *
 */
public final class ArrayType extends Type implements java.io.Serializable {
  static final long serialVersionUID = 12022L;
  private final Type base;
  private final long extent;

  public ArrayType(long extent, Type base) {
    this.extent = extent;
    this.base = base;
  }

  public Type getBase() {
    return base;
  }

  public long getExtent() {
    return extent;
  }

  @Override
  Type index(Type that) {
    if (that instanceof IntType) {
      return base;
    }
    return super.index(that);
  }

  @Override
  Type assign(Type source) {
    if (source.equivalent(this)) {
      return this;
    }
    return super.assign(source);
  }

  @Override
  public boolean equivalent(Type that) {
    if (!(that instanceof ArrayType)) {
      return false;
    }
    ArrayType temp = (ArrayType) that;
    return this.base.equivalent(temp.base);
  }

  @Override
  public String toString() {
    return String.format("array[%d,%s]", extent, base);
  }
}
