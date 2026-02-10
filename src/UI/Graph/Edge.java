package UI.Graph;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.concurrent.TimeUnit;

public class Edge {
  public class LogicData {
    public int id;
    public String actionCode;
    public String actionParams;
    public String conditionCode;
    public String conditionType;
    public String conditionValue;
    public String priority;

    public boolean isEmpty() {
      return actionCode == null || actionParams == null || conditionCode == null || conditionType == null
          || conditionValue == null;
    }
  }

  public LogicData logicData = new LogicData();

  public Node from;

  public Node to;

  int edgeID;
  int lastDrawOffsetX;

  int lastDrawOffsetY;

  private int highlighted = 0;

  public Edge(Node from, Node to, int edgeID) {
    this.from = from;
    this.to = to;
    this.edgeID = edgeID;
  }

  public void highlight() {
    highlighted++;
    try {
      TimeUnit.MILLISECONDS.sleep(510);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    highlighted--;
  }

  public void draw(Graphics2D g, int index, int total, boolean selected) {
    int x1 = from.getCenterX();
    int y1 = from.getCenterY();
    int x2 = to.getCenterX();
    int y2 = to.getCenterY();

    boolean reversed = getFrom().getNodeID() > getTo().getNodeID();

    double dirX = x2 - x1;
    double dirY = y2 - y1;
    double length = Math.hypot(dirX, dirY);

    if (length == 0)
      return;

    // perpendicular unit vector
    double px = -dirY / length;
    double py = dirX / length;

    double spacing = 14;
    double offset = (index - (total - 1) / 2.0) * spacing;
    if (reversed)
      offset *= -1;

    int ox = (int) (px * offset);
    int oy = (int) (py * offset);
    lastDrawOffsetX = ox;
    lastDrawOffsetY = oy;

    int sx1 = x1 + ox;
    int sy1 = y1 + oy;
    int sx2 = x2 + ox;
    int sy2 = y2 + oy;

    g.setStroke(new BasicStroke(selected ? 3 : 2));
    g.setColor(selected ? Color.BLUE : Color.GRAY);
    if (highlighted > 0)
      g.setColor(Color.YELLOW);
    g.drawLine(sx1, sy1, sx2, sy2);

    int midX = (x1 + x2) / 2;
    int midY = (y1 + y2) / 2;

    drawArrow(g, midX + ox, midY + oy, px, py, 13);
  }

  public boolean contains(Point p) {
    Line2D line = new Line2D.Double(
        from.getCenterX() + lastDrawOffsetX,
        from.getCenterY() + lastDrawOffsetY,
        to.getCenterX() + lastDrawOffsetX,
        to.getCenterY() + lastDrawOffsetY);

    // tolerance in pixels (world space)
    return line.ptSegDist(p) <= 5.0;
  }

  public Node getFrom() {
    return from;
  }

  public void setFrom(Node from) {
    this.from = from;
  }

  public Node getTo() {
    return to;
  }

  public int getEdgeID() {
    return edgeID;
  }

  public void setTo(Node to) {
    this.to = to;
  }

  private void drawArrow(
      Graphics2D g,
      int posX, int posY,
      double dirX, double dirY,
      int size) {
    double len = Math.hypot(dirX, dirY);

    if (len == 0)
      return;

    // normalize direction
    dirX /= len;
    dirY /= len;

    // perpendicular
    double px = dirY;
    double py = -dirX;

    int tipX = (int) (posX + px * size * 0.5);
    int tipY = (int) (posY + py * size * 0.5);

    int backX = (int) Math.floor(posX - px * size * 0.5);
    int backY = (int) Math.floor(posY - py * size * 0.5);

    int leftX = (int) Math.floor(backX + dirX * size * 0.6);
    int leftY = (int) Math.floor(backY + dirY * size * 0.6);

    int rightX = (int) Math.floor(backX - dirX * size * 0.6);
    int rightY = (int) Math.floor(backY - dirY * size * 0.6);

    Polygon arrow = new Polygon();
    arrow.addPoint(tipX, tipY);
    arrow.addPoint(leftX, leftY);
    arrow.addPoint(rightX, rightY);

    // g.setColor(Color.GRAY);
    g.fillPolygon(arrow);
  }
}
