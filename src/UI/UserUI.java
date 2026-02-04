package UI;

import Network.IP;
import Network.NetworkDevice;
import Network.NetworkManager;
import Network.UserDevice;
import UI.Graph.*;
import UI.Graph.GraphPanel.EdgeConfigHandler;
import UI.Graph.GraphPanel.NodeConfigHandler;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class UserUI extends JFrame {
  private final NetworkManager networkManager;
  private final UserDevice userDevice;
  private final ArrayList<String> discoveredIPs = new ArrayList<String>();
  protected final GraphModel model = new GraphModel();
  protected GraphPanel graphPanel;

  protected JComponent actionValue;
  protected JComponent condValue;

  private IP shmanagerIP;

  public UserUI(UserDevice dev, NetworkManager nm) {
    this.userDevice = dev;
    this.networkManager = nm;

    findSHManager();

    setTitle("Panel Użytkownika (Smart Home App)");
    setSize(450, 700);
    setLocation(600, 50);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLayout(new BorderLayout());

    scanNetwork();
    generateGraph();

    graphPanel = new GraphPanel(model);

    graphPanel.setNodeConfigHandler(node -> {
      JOptionPane.showMessageDialog(
          graphPanel,
          "Configure node: " + node,
          "Node Config",
          JOptionPane.INFORMATION_MESSAGE);
    });

    graphPanel.setEdgeConfigHandler((edge, isNew) -> {
      if (isNew) {
        refreshGraph();
      }
      SwingUtilities.invokeLater(() -> {
        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
        JPanel conditionPanel = new JPanel(new FlowLayout());
        JPanel actionPanel = new JPanel();
        // conditionPanel.setPreferredSize(new Dimension(600, 250));
        JOptionPane pane = new JOptionPane(dialogPanel,
            JOptionPane.PLAIN_MESSAGE,
            JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = pane.createDialog("Logic Config");

        NetworkManager.Request r = networkManager.createRequest(userDevice.getIP(),
            edge.from.deviceIP, "ADVERT", new String[] {});
        r.send();
        String rawFrom = r.getResult();

        String fromDevID = networkManager
            .createRequest(userDevice.getIP(), shmanagerIP, "GET_DEVID",
                new String[] { edge.from.deviceIP.getAddressString() })
            .send()
            .getResult();
        String toDevID = networkManager
            .createRequest(userDevice.getIP(), shmanagerIP, "GET_DEVID",
                new String[] { edge.to.deviceIP.getAddressString() })
            .send()
            .getResult();

        Font labelFont = new Font("Serif", Font.BOLD, 20);
        JLabel ifLabel = new JLabel("If (" + fromDevID + ")");
        ifLabel.setFont(labelFont);
        conditionPanel.add(ifLabel);

        JLabel thenLabel = new JLabel("Then (" + toDevID + ")");
        thenLabel.setFont(labelFont);
        actionPanel.add(thenLabel);

        JComboBox parameter = generateValuesCombo(rawFrom);

        condValue = generateCondValueField((String) parameter.getSelectedItem(), rawFrom);

        JComboBox condType = new JComboBox(new String[] {
            "Equals",
            "Not equals",
            "Greater than",
            "Less than",
            "Greater than or equal to",
            "Less than or equal to"
        });

        parameter.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            conditionPanel.removeAll();

            System.out.println("raw: " + rawFrom);
            conditionPanel.add(ifLabel);
            conditionPanel.add(parameter);
            conditionPanel.add(condType);
            condValue = generateCondValueField((String) parameter.getSelectedItem(), rawFrom);
            conditionPanel.add(condValue);

            conditionPanel.revalidate();
            conditionPanel.repaint();
            pane.revalidate();
            pane.repaint();
            dialog.pack();
          }
        });

        conditionPanel.add(parameter);
        conditionPanel.add(condType);
        conditionPanel.add(condValue);

        r = networkManager.createRequest(userDevice.getIP(),
            edge.to.deviceIP, "ADVERT", new String[] {});
        r.send();
        String rawTo = r.getResult();

        JComboBox actionCode = generateActionCodeCombo(rawTo);
        actionValue = generateCondValueField((String) actionCode.getSelectedItem(), rawTo);
        actionCode.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            actionPanel.removeAll();

            actionPanel.add(thenLabel);
            actionPanel.add(actionCode);
            actionValue = generateCondValueField((String) actionCode.getSelectedItem(), rawTo);
            actionPanel.add(actionValue);

            actionPanel.revalidate();
            actionPanel.repaint();
            pane.revalidate();
            pane.repaint();
            dialog.pack();
          }
        });

        actionPanel.add(actionCode);
        actionPanel.add(actionValue);

        dialogPanel.add(conditionPanel);
        dialogPanel.add(actionPanel);

        // Save changes when closing
        pane.addPropertyChangeListener(evt -> {
          if (evt.getPropertyName().equals(JOptionPane.VALUE_PROPERTY)
              && evt.getNewValue() != null
              && evt.getNewValue() != JOptionPane.UNINITIALIZED_VALUE) {

            dialog.dispose();

            if (evt.getNewValue().equals(JOptionPane.OK_OPTION)) {
              // OK
              networkManager.createRequest(userDevice.getIP(), shmanagerIP, "ADD_LOGIC", new String[] {
                  edge.to.deviceIP.getAddressString(),
                  "SET_" + (String) actionCode.getSelectedItem(),
                  "[" + getInputValueString(actionValue) + "]",

                  edge.from.deviceIP.getAddressString(),
                  "GET_" + (String) parameter.getSelectedItem(),
                  switch ((String) condType.getSelectedItem()) {
                    case "Equals" -> "EQUAL";
                    case "Not equals" -> "NOT_EQUALS";
                    case "Greater than" -> "GREATER_THAN";
                    case "Greater than or equal to" -> "GREATER_EQUAL";
                    case "Less than" -> "LESS_THAN";
                    case "Less than or equal to" -> "LESS_EQUAL";
                    default -> "EQUAL";
                  },
                  getInputValueString(condValue),
              }).send();

              refreshGraph();
            }
            // Cancelled
          }
        });

        dialog.pack();
        dialog.setVisible(true);
      });
    });

    JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JButton refreshButton = new JButton("Refresh");
    refreshButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        refreshGraph();
      }
    });
    refreshPanel.add(refreshButton);
    refreshPanel.setSize(100, 50);

    add(refreshPanel, BorderLayout.NORTH);
    add(graphPanel, BorderLayout.CENTER);
  }

  protected void refreshGraph() {
    SwingUtilities.invokeLater(() -> {
      scanNetwork();
      System.out.println(discoveredIPs);
      model.clearEdges();
      generateGraph();
      remove(graphPanel);
      NodeConfigHandler nch = graphPanel.getNodeConfigHandler();
      EdgeConfigHandler ech = graphPanel.getEdgeConfigHandler();
      graphPanel = new GraphPanel(model);
      graphPanel.setNodeConfigHandler(nch);
      graphPanel.setEdgeConfigHandler(ech);
      add(graphPanel, BorderLayout.CENTER);
      revalidate();
      repaint();
    });
  }

  private String[] parseStringArray(String str) {
    if (str.length() <= 2) {
      return new String[] {};
    }
    String s = str.substring(1, str.length() - 1);
    return s.split(",");
  }

  private void findSHManager() {
    shmanagerIP = new IP(networkManager.createRequest(userDevice.getIP(), new IP(
        new byte[] {
            userDevice.getIP().getAddress()[0],
            userDevice.getIP().getAddress()[1],
            userDevice.getIP().getAddress()[2],
            (byte) 255
        }),
        "FINDSHMANAGER", new String[] {}).send().getResult());
  };

  private void generateGraph() {
    HashMap<String, Node> nodes = new HashMap<>();
    for (int i = 0; i < discoveredIPs.size(); i++) {
      double angle = ((double) i / discoveredIPs.size()) * (3.14159 * 2);

      int x = (int) Math.round(Math.cos(angle) * 100) + 200;
      int y = (int) Math.round(Math.sin(angle) * 100) + 200;

      String devID = networkManager
          .createRequest(userDevice.getIP(), shmanagerIP, "GET_DEVID", new String[] { discoveredIPs.get(i) }).send()
          .getResult();

      Node node = new Node(x, y, devID, new IP(discoveredIPs.get(i)));
      node = model.addNode(node);
      nodes.put(discoveredIPs.get(i), node);
    }

    String logics = networkManager
        .createRequest(userDevice.getIP(), shmanagerIP, "GET_LOGICS", new String[] {}).send()
        .getResult();

    String[] ips = parseStringArray(logics);

    for (int i = 0; i < ips.length; i += 2) {
      if (i >= ips.length - 1)
        break;

      model.addEdge(nodes.get(ips[i]), nodes.get(ips[i + 1]));
    }
    ;
  }

  private void scanNetwork() {
    discoveredIPs.clear();
    log("Rozpoczynanie skanowania...");
    String devIps = networkManager.createRequest(userDevice.getIP(), shmanagerIP,
        "GET_DEVICES", new String[] {}).send().getResult();
    String clean = devIps.replace("[", "").replace("]", "");
    String[] ips = clean.split(",");

    for (String targetIP : ips) {
      if (targetIP.trim().length() == 0) {
        continue;
      }
      System.out.println("ip: " + targetIP);

      NetworkManager.Request r = networkManager.createRequest(userDevice.getIP(), new IP(targetIP), "ADVERT",
          new String[] {});
      r.send();
      if (Objects.equals(r.getResult(), "")) // Device has no netcodes
        return;
      if (!discoveredIPs.contains(targetIP))
        discoveredIPs.add(targetIP);
    }
  }

  private void addValue(String code, ArrayList<Value> values) {
    String c = code;
    boolean getter = false;
    boolean setter = false;
    if (c.startsWith("GET_")) {
      c = c.replaceFirst("^GET_", "");
      getter = true;
    }

    if (c.startsWith("SET_")) {
      c = c.replaceFirst("^SET_", "");
      setter = true;
    }

    String type = c.replaceAll(">$", "");
    type = type.replaceAll("^.*<", "");

    c = c.replaceAll("<.*>$", "");

    for (Value v : values) {
      if (Objects.equals(v.name, c)) {
        v.setter |= setter;
        v.getter |= getter;
        v.type = type;
        return;
      }
    }

    values.add(new Value(c, type, setter, getter));
  }

  private JComboBox generateValuesCombo(String rawCodes) {
    String clean = rawCodes.replace("[", "").replace("]", "");
    String[] codes = clean.split(",");

    ArrayList<Value> values = new ArrayList<>();
    for (String c : codes) {
      String code = c.trim();
      if (code.isEmpty())
        continue;

      addValue(code, values);
    }
    ArrayList<String> res = new ArrayList<>();

    for (Value val : values) {
      if (val.getter) {
        res.add(val.name);
      }
    }

    JComboBox box = new JComboBox(res.toArray(new String[] {}));
    return box;
  }

  private String getInputValueString(JComponent input) {
    if (input instanceof JColorChooser)
      return String.format("#%06X", (0xFFFFFF & ((JColorChooser) input).getColor().getRGB()));
    if (input instanceof JTextField) {
      return ((JTextField) input).getText();
    }
    if (input instanceof JSlider) {
      return String.valueOf(((JSlider) input).getValue() / 100.0);
    }
    if (input instanceof JCheckBox) {
      return ((JCheckBox) input).isSelected() ? "true" : "false";
    }

    return "MISSING TYPE!!!";
  }

  private JComponent generateCondValueField(String paramName, String rawCodes) {
    String clean = rawCodes.replace("[", "").replace("]", "");
    String[] codes = clean.split(",");

    ArrayList<Value> values = new ArrayList<>();
    for (String c : codes) {
      String code = c.trim();
      if (code.isEmpty())
        continue;

      addValue(code, values);
    }

    for (Value val : values) {
      if (val.getter && val.name.equals(paramName)) {
        switch (val.type) {
          case "INT":
            return new JTextField(10);
          case "FLOAT":
            return new JTextField(10);
          case "STRING":
            return new JTextField(10);
          case "COLOR":
            JComponent comp = new JColorChooser();
            comp.setSize(200, 200);
            return comp;
          case "RANGE":
            // Range from 0 to 1
            JSlider slider = new JSlider();
            return slider;
          case "BOOL":
            JCheckBox checkBox = new JCheckBox();
            return checkBox;
          default:
            return new JTextField(10);
        }
      }
    }

    return new JTextField(10);
  }

  private JComboBox generateActionCodeCombo(String rawCodes) {
    String clean = rawCodes.replace("[", "").replace("]", "");
    String[] codes = clean.split(",");

    ArrayList<Value> values = new ArrayList<>();
    for (String c : codes) {
      String code = c.trim();
      if (code.isEmpty())
        continue;

      addValue(code, values);
    }
    ArrayList<String> res = new ArrayList<>();

    for (Value val : values) {
      if (val.setter) {
        res.add(val.name);
      }
    }

    JComboBox box = new JComboBox(res.toArray(new String[] {}));
    return box;
  }

  private class Value {
    String name;
    boolean setter;
    boolean getter;
    String type;

    public Value(String name, String type, boolean setter, boolean getter) {
      this.name = name;
      this.getter = getter;
      this.setter = setter;
      this.type = type;
    }

  }

  private void log(String msg) {
    SwingUtilities.invokeLater(() -> {
      // logArea.append(msg + "\n");
      // logArea.setCaretPosition(logArea.getDocument().getLength());
    });
    System.out.println(msg);
  }
}
