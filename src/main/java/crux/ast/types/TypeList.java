package crux.ast.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * TypeList is a list of other types useful to model the types of functions params
 */
public final class TypeList extends Type implements Iterable<Type>, java.io.Serializable {
  static final long serialVersionUID = 12022L;
  private final List<Type> list;

  public TypeList() {
    list = new ArrayList<>();
  }

  public TypeList(List<Type> types) {
    list = types;
  }

  public static TypeList of(Type... types) {
    var typeList = new TypeList();
    typeList.list.addAll(Arrays.asList(types));
    return typeList;
  }

  public void append(Type type) {
    list.add(type);
  }

  public boolean isEmpty() {
    return list.isEmpty();
  }

  public int size() {
    return list.size();
  }

  @Override
  public boolean equivalent(Type that) {
    if (!(that instanceof TypeList)) {
      return false;
    }
    TypeList temp = (TypeList) that;

    if (this.size() != temp.size()) {
      return false;
    }
    Iterator<Type> expected = this.iterator();
    Iterator<Type> actual = temp.iterator();

    for (int x = 0; x < this.size(); x++) {
      Type expectedType = expected.next();
      Type actualType     = actual.next();

      if (!(expectedType.equivalent(actualType))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Iterator<Type> iterator() {
    return list.iterator();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("TypeList(");
    for (int i = 0; i < list.size() - 1; ++i)
      sb.append(list.get(i) + ", ");
    if (list.size() >= 1)
      sb.append(list.get(list.size() - 1));
    sb.append(")");
    return sb.toString();
  }
}
