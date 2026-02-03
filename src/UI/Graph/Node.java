package UI.Graph;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import Network.IP;

public class Node {
  private int x, y;
  private int width = 80;
  private int height = 40;
  private String label;

  public IP deviceIP;

  public Node(int x, int y, String label, IP deviceIP) {
    this.x = x;
    this.y = y;
    this.label = label;
    this.deviceIP = deviceIP;
  }

  public Rectangle getBounds() {
    return new Rectangle(x, y, width, height);
  }

  public boolean contains(Point p) {
    return getBounds().contains(p);
  }

  public void draw(Graphics2D g) {
    g.setColor(new Color(220, 235, 255));
    g.fillRoundRect(x, y, width, height, 12, 12);

    g.setColor(Color.DARK_GRAY);
    g.drawRoundRect(x, y, width, height, 12, 12);

    FontMetrics fm = g.getFontMetrics();
    Rectangle2D r = fm.getStringBounds(label, g);
    int textX = x + (width - (int) r.getWidth()) / 2;
    int textY = y + (height + fm.getAscent()) / 2 - 4;

    g.drawString(label, textX, textY);
  }

  // getters/setters
  public int getCenterX() {
    return x + width / 2;
  }

  public int getCenterY() {
    return y + height / 2;
  }

  public void moveBy(int dx, int dy) {
    x += dx;
    y += dy;
  }
}
