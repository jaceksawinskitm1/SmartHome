package UI.Graph;

import java.awt.*;

public class Edge {
  public Node from;
  public Node to;

  public Edge(Node from, Node to) {
    this.from = from;
    this.to = to;
  }

  public void draw(Graphics2D g) {
    g.setColor(Color.GRAY);
    g.setStroke(new BasicStroke(2));

    g.drawLine(
        from.getCenterX(),
        from.getCenterY(),
        to.getCenterX(),
        to.getCenterY());
  }
}
