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
import java.util.Objects;

public class UserUI extends JFrame {
  private final NetworkManager networkManager;
  private final UserDevice userDevice;
  private final ArrayList<String> discoveredIPs = new ArrayList<String>();
  protected GraphPanel graphPanel;

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
    GraphModel model = generateGraph();

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

      JPanel dialogPanel = new JPanel();

      NetworkManager.Request r = networkManager.createRequest(userDevice.getIP(),
          edge.from.deviceIP, "ADVERT", new String[] {});
      r.send();
      String raw = r.getResult();

      // TODO: FINISH
      JComboBox parameter = new JComboBox();
      JComboBox condType = new JComboBox(new String[] {
          "Equals",
          "Not equals",
          "Greater than",
          "Less than",
          "Greater than or equal to",
          "Less than or equal to"
      });
      dialogPanel.add(condType);

      JOptionPane.showMessageDialog(
          graphPanel,
          dialogPanel,
          "Logic Config",
          JOptionPane.INFORMATION_MESSAGE);
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
      GraphModel m = generateGraph();
      remove(graphPanel);
      NodeConfigHandler nch = graphPanel.getNodeConfigHandler();
      EdgeConfigHandler ech = graphPanel.getEdgeConfigHandler();
      graphPanel = new GraphPanel(m);
      graphPanel.setNodeConfigHandler(nch);
      graphPanel.setEdgeConfigHandler(ech);
      add(graphPanel, BorderLayout.CENTER);
      revalidate();
      repaint();
    });
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

  private GraphModel generateGraph() {
    GraphModel model = new GraphModel();

    for (int i = 0; i < discoveredIPs.size(); i++) {
      double angle = ((double) i / discoveredIPs.size()) * (3.14159 * 2);

      int x = (int) Math.round(Math.cos(angle) * 100) + 200;
      int y = (int) Math.round(Math.sin(angle) * 100) + 200;

      String devID = networkManager
          .createRequest(userDevice.getIP(), shmanagerIP, "GET_DEVID", new String[] { discoveredIPs.get(i) }).send()
          .getResult();

      Node node = new Node(x, y, devID, new IP(discoveredIPs.get(i)));
      model.addNode(node);
    }

    return model;
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

  /*
   * private void loadDeviceUI(String ipStr) {
   * controlsPanel.removeAll();
   * controlsPanel.add(new JLabel("Ładowanie interfejsu..."));
   * controlsPanel.revalidate();
   * controlsPanel.repaint();
   * 
   * new Thread(() -> {
   * IP target = new IP(ipStr);
   * NetworkManager.Request r = networkManager.createRequest(userDevice.getIP(),
   * target, "ADVERT", new String[]{});
   * r.send();
   * String raw = r.getResult();
   * 
   * SwingUtilities.invokeLater(() -> {
   * controlsPanel.removeAll();
   * if (raw.startsWith("ERROR") || raw.equals("TIMEOUT")) {
   * controlsPanel.add(new JLabel("Błąd połączenia."));
   * } else {
   * buildControls(raw, target);
   * }
   * controlsPanel.revalidate();
   * controlsPanel.repaint();
   * });
   * }).start();
   * }
   */

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

  private void buildControls(String rawCodes, IP target) {
    String clean = rawCodes.replace("[", "").replace("]", "");
    String[] codes = clean.split(",");

    // controlsPanel.add(new JLabel("<html><b>Urządzenie: " +
    // target.getAddressString() + "</b></html>"));
    // controlsPanel.add(Box.createVerticalStrut(10));

    ArrayList<Value> values = new ArrayList<>();
    for (String c : codes) {
      String code = c.trim();
      if (code.isEmpty())
        continue;

      addValue(code, values);
    }

    for (Value val : values) {
      JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      JPanel row = new JPanel(new GridLayout(2, 2));
      row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
      JLabel lbl = new JLabel(val.name);
      lbl.setPreferredSize(new Dimension(100, 10));
      lbl.setFont(new Font("SansSerif", Font.PLAIN, 10));

      panel.add(lbl);

      if (val.getter) {
        JButton btn = new JButton("Get");
        JLabel valLbl = new JLabel("?");
        btn.addActionListener(e -> sendRequest(target, "GET_" + val.name, new String[] {}, valLbl));
        row.add(btn);
        row.add(valLbl);
        btn.doClick();
      }

      if (val.setter) {
        JTextField paramField = new JTextField(5);
        JButton btn = new JButton("Set");
        btn.addActionListener(e -> {
          String txt = paramField.getText();
          String[] params = txt.isEmpty() ? new String[] {} : txt.split(",");
          sendRequest(target, "SET_" + val.name, params, null);
        });
        row.add(btn);
        row.add(paramField);
      }
      panel.add(row);
      // controlsPanel.add(panel);
    }
  }

  private void sendRequest(IP target, String code, String[] params, JLabel outLabel) {
    new Thread(() -> {
      log("-> " + code + " to " + target.getAddressString());
      NetworkManager.Request r = networkManager.createRequest(userDevice.getIP(), target, code, params);
      r.send();
      String res = r.getResult();
      log("<- " + res);
      if (outLabel != null) {
        SwingUtilities.invokeLater(() -> outLabel.setText(res));
      }
    }).start();
  }

  private void log(String msg) {
    SwingUtilities.invokeLater(() -> {
      // logArea.append(msg + "\n");
      // logArea.setCaretPosition(logArea.getDocument().getLength());
    });
    System.out.println(msg);
  }
}
