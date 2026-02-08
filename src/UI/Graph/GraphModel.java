package UI.Graph;

import java.util.ArrayList;
import java.util.List;

public class GraphModel {
  private List<Node> nodes = new ArrayList<>();
  private List<Edge> edges = new ArrayList<>();
  private int nextEdgeID = 0;
  private int nextNodeID = 0;

  public Node addNode(Node node) {
    for (Node n : nodes) {
      if (n.deviceIP.equals(node.deviceIP)) {
        return n;
      }
    }
    node.setNodeID(nextNodeID);
    nextNodeID++;
    nodes.add(node);
    return node;
  }

  public Edge addEdge(Node from, Node to) {
    Edge edge = new Edge(from, to, nextEdgeID);
    nextEdgeID++;
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
