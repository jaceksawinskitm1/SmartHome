package UI.Graph;

import java.util.ArrayList;
import java.util.List;

public class GraphModel {
  private List<Node> nodes = new ArrayList<>();
  private List<Edge> edges = new ArrayList<>();

  public Node addNode(Node node) {
    for (Node n : nodes) {
      if (n.deviceIP.equals(node.deviceIP)) {
        return n;
      }
    }
    nodes.add(node);
    return node;
  }

  public Edge addEdge(Node from, Node to) {
    for (Edge e : edges) {
      if (e.from.equals(from) && e.to.equals(to)) {
        return null;
      }
    }
    Edge edge = new Edge(from, to);
    edges.add(edge);
    return edge;
  }

  public void clearEdges() {
    this.edges.clear();
  }

  public List<Node> getNodes() {
    return nodes;
  }

  public List<Edge> getEdges() {
    return edges;
  }
}
