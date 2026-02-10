package UI.Graph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GraphModel {
  private List<Node> nodes = new ArrayList<>();
  private List<Edge> edges = new ArrayList<>();
  private int nextEdgeID = 0;
  private int nextNodeID = 0;

  private ArrayList<Integer> garbage;

  public Node addNode(Node node) {
    for (Node n : nodes) {
      if (n.deviceIP.equals(node.deviceIP)) {
        n.setLabel(node.getLabel());
        if (garbage != null && garbage.contains(n.getNodeID()))
          garbage.remove(garbage.indexOf(n.getNodeID()));
        return n;
      }
    }
    node.setNodeID(nextNodeID);
    if (garbage != null && garbage.contains(node.getNodeID()))
      garbage.remove(garbage.indexOf(node.getNodeID()));
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

  public void startNodeGarbageCollect() {
    garbage = new ArrayList<>();
    for (Node n : getNodes()) {
      garbage.add(n.getNodeID());
    }
  }

  public void endNodeGarbageCollect() {
    for (int id : garbage) {
      for (Iterator<Node> n = nodes.iterator(); n.hasNext();) {
        if (n.next().getNodeID() == id)
          n.remove();
      }
    }
    garbage = null;
  }
}
