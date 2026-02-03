package UI.Graph;

import java.util.ArrayList;
import java.util.List;

public class GraphModel {
  private List<Node> nodes = new ArrayList<>();
  private List<Edge> edges = new ArrayList<>();

  public void addNode(Node node) {
    nodes.add(node);
  }

  public Edge addEdge(Node from, Node to) {
    Edge edge = new Edge(from, to);
    edges.add(edge);
    return edge;
  }

  public List<Node> getNodes() {
    return nodes;
  }

  public List<Edge> getEdges() {
    return edges;
  }
}
