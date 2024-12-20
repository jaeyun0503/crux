package crux.ast;

import crux.ast.traversal.NodeVisitor;
import java.util.List;

/**
 * AST node for if stmt.
 */
public final class WhileLoop extends BaseNode implements Statement, java.io.Serializable {
  private final Expression condition;
  private final StatementList body;

  /**
   * @param position is the location of the statement in the source,
   * @param condition is the expression for the if condition.
   * @param body is the body of the loop if the condition is true.
   */

  public WhileLoop(Position position, Expression condition, StatementList body) {
    super(position);
    this.condition = condition;
    this.body = body;
  }

  public Expression getCondition() {
    return condition;
  }

  public StatementList getBody() {
    return body;
  }

  @Override
  public List<Node> getChildren() {
    return List.of(condition, body);
  }

  @Override
  public <T> T accept(NodeVisitor<? extends T> visitor) {
    return visitor.visit(this);
  }
}
