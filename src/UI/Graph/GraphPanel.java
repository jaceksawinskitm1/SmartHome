package UI.Graph;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class GraphPanel extends JPanel {
  public interface NodeConfigHandler {
    void configure(Node node);
  };

  public interface EdgeConfigHandler {
    void configure(Edge edge, boolean isNew);
  };

  private GraphModel model;

  private Node draggedNode;
  private Node edgeStartNode;
  private Point lastMouse;

  private Edge selectedEdge;

  private double scale = 1.0;
  private double offsetX = 0;
  private double offsetY = 0;

  private NodeConfigHandler nodeConfigHandler;
  private EdgeConfigHandler edgeConfigHandler;

  public GraphPanel(GraphModel model) {
    this.model = model;
    setBackground(Color.WHITE);

    MouseAdapter mouse = new MouseAdapter() {

      @Override
      public void mousePressed(MouseEvent e) {
        Point world = toWorld(e.getPoint());

        // Right / middle mouse = pan
        if (SwingUtilities.isMiddleMouseButton(e)
            || SwingUtilities.isRightMouseButton(e)) {
          lastMouse = e.getPoint();
          return;
        }

        for (Node node : model.getNodes()) {
          if (node.contains(world)) {
            if (e.isShiftDown()) {
              edgeStartNode = node;
            } else {
              draggedNode = node;
            }
            lastMouse = world;
            return;
          }
        }

        selectedEdge = null;

        for (Edge edge : model.getEdges()) {
          if (edge.contains(world)) {
            selectedEdge = edge;
            repaint();
            return;
          }
        }
      }

      @Override
      public void mouseDragged(MouseEvent e) {
        Point world = toWorld(e.getPoint());

        if (draggedNode != null) {
          draggedNode.moveBy(
              world.x - lastMouse.x,
              world.y - lastMouse.y);
          lastMouse = world;
          repaint();
        } else if (edgeStartNode != null) {
          lastMouse = world;
          repaint();
        } else if (lastMouse != null) {
          // panning
          offsetX += e.getX() - lastMouse.x;
          offsetY += e.getY() - lastMouse.y;
          lastMouse = e.getPoint();
          repaint();
        }
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        Point world = toWorld(e.getPoint());

        if (edgeStartNode != null) {
          for (Node node : model.getNodes()) {
            if (node != edgeStartNode && node.contains(world)) {
              Edge edge = model.addEdge(edgeStartNode, node);
              if (edgeConfigHandler != null) {
                edgeConfigHandler.configure(edge, true);
              }
              break;
            }
          }
        }

        draggedNode = null;
        edgeStartNode = null;
        lastMouse = null;

        repaint();
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          Point world = toWorld(e.getPoint());
          for (Node node : model.getNodes()) {
            if (node.contains(world) && nodeConfigHandler != null) {
              nodeConfigHandler.configure(node);
              return;
            }
          }

          for (Edge edge : model.getEdges()) {
            if (edge.contains(world) && edgeConfigHandler != null) {
              edgeConfigHandler.configure(edge, false);
              return;
            }
          }
        }
      }

      @Override
      public void mouseWheelMoved(MouseWheelEvent e) {
        double delta = e.getPreciseWheelRotation();
        double zoomFactor = Math.pow(1.1, -delta);
        scale *= zoomFactor;
        repaint();
      }
    };

    addMouseListener(mouse);
    addMouseMotionListener(mouse);
    addMouseWheelListener(mouse);
  }

  public void highlightEdge(int id) {
    new Thread(() -> {
      for (Edge e : model.getEdges()) {
        if (e.logicData.id == id) {
          repaint();
          e.highlight();
          repaint();
          return;
        }
      }
    }).start();
  }

  private ArrayList<Edge> getParallelEdges(Edge target) {
    ArrayList<Edge> result = new ArrayList<>();
    for (Edge e : model.getEdges()) {
      if ((e.getFrom() == target.getFrom() && e.getTo() == target.getTo())
          || (e.getFrom() == target.getTo() && e.getTo() == target.getFrom())) {
        result.add(e);
      }
    }
    return result;
  }

  public NodeConfigHandler getNodeConfigHandler() {
    return this.nodeConfigHandler;
  }

  public void setNodeConfigHandler(NodeConfigHandler handler) {
    this.nodeConfigHandler = handler;
  }

  public EdgeConfigHandler getEdgeConfigHandler() {
    return this.edgeConfigHandler;
  }

  public void setEdgeConfigHandler(EdgeConfigHandler handler) {
    this.edgeConfigHandler = handler;
  }

  private Point toWorld(Point screen) {
    return new Point(
        (int) ((screen.x - offsetX) / scale),
        (int) ((screen.y - offsetY) / scale));
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);

    AffineTransform old = g2.getTransform();
    g2.translate(offsetX, offsetY);
    g2.scale(scale, scale);

    for (Edge edge : model.getEdges()) {
      ArrayList<Edge> parallels = getParallelEdges(edge);
      parallels.sort(java.util.Comparator.comparingInt(e -> e.getEdgeID()));
      int index = parallels.indexOf(edge);
      edge.draw(g2, index, parallels.size(), edge == selectedEdge);
    }

    if (edgeStartNode != null && lastMouse != null) {
      g2.setColor(Color.LIGHT_GRAY);
      g2.setStroke(new BasicStroke(2));
      g2.drawLine(
          edgeStartNode.getCenterX(),
          edgeStartNode.getCenterY(),
          lastMouse.x,
          lastMouse.y);
    }

    for (Node node : model.getNodes()) {
      node.draw(g2);
    }

    g2.setTransform(old);
  }
}
