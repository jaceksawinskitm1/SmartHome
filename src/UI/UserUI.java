package UI;

import Network.IP;
import Network.NetworkManager;
import Network.UserDevice;
import UI.Graph.*;
import UI.Graph.GraphPanel.EdgeConfigHandler;
import UI.Graph.GraphPanel.NodeConfigHandler;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.Format;
import java.text.NumberFormat;
import java.util.*;

public class UserUI extends JFrame {
  private final NetworkManager networkManager;
  private final UserDevice userDevice;
  private final ArrayList<String> discoveredIPs = new ArrayList<String>();
  protected final GraphModel model = new GraphModel();
  protected GraphPanel graphPanel;

  protected ConditionStructure action;
  protected ConditionStructure condition;

  private IP shmanagerIP;

  void refreshValues(ArrayList<Value> values, String rawCodes, IP deviceIP, JPanel parent,
      HashMap<Value, JComponent> valueMap) {
    parent.removeAll();
    valueMap.clear();
    for (Value val : values) {
      if (val.getter) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel lbl = new JLabel(capitalizeString(val.name) + ": ");
        panel.add(lbl);
        String currentValue = networkManager
            .createRequest(userDevice.getIP(), deviceIP, "GET_" + val.name, new String[] {}).send()
            .getResult();

        JComponent valueComp = generateCondValueField(val.name, rawCodes, currentValue, new ArrayList<>());

        if (!val.setter) {
          // Readonly value
          valueComp.setEnabled(false);
        }

        valueMap.put(val, valueComp);
        panel.add(valueComp);

        parent.add(panel);
      } else if (!val.setter) {
        // No getter and setter -> action
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton valueComp = new JButton(capitalizeString(val.name));
        valueComp.addActionListener(e -> {
          networkManager.createRequest(userDevice.getIP(), deviceIP, val.name, new String[] {}).send();
        });

        panel.add(valueComp);

        parent.add(panel);
      }
    }
    parent.revalidate();
    parent.repaint();
  };

  public UserUI(UserDevice dev, NetworkManager nm) {
    this.userDevice = dev;
    this.networkManager = nm;

    findSHManager();

    setTitle("Smart Home App");
    setSize(450, 700);
    setLocation(600, 50);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLayout(new BorderLayout());

    scanNetwork();
    generateGraph();

    graphPanel = new GraphPanel(model);

    graphPanel.setNodeConfigHandler(node -> {
      JPanel configPanel = new JPanel();
      configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));

      NetworkManager.Request r = networkManager.createRequest(userDevice.getIP(),
          node.deviceIP, "ADVERT", new String[] {});
      r.send();

      String rawCodes = r.getResult();
      String clean = rawCodes.replace("[", "").replace("]", "");
      String[] codes = clean.split(",");

      String devID = networkManager
          .createRequest(userDevice.getIP(), shmanagerIP, "GET_DEVID",
              new String[] { node.deviceIP.getAddressString() })
          .send()
          .getResult();

      // configPanel.add(Box.createVerticalStrut(10));

      ArrayList<Value> values = new ArrayList<>();
      for (String c : codes) {
        String code = c.trim();
        if (code.isEmpty())
          continue;

        addValue(code, values);
      }

      JPanel valuesPanel = new JPanel();
      valuesPanel.setLayout(new BoxLayout(valuesPanel, BoxLayout.Y_AXIS));

      HashMap<Value, JComponent> valueMap = new HashMap<>();
      refreshValues(values, rawCodes, node.deviceIP, valuesPanel, valueMap);
      configPanel.add(valuesPanel);

      JPanel renamePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      JButton renameButton = new JButton("Rename device");
      renameButton.addActionListener(e -> {
        String newID = JOptionPane.showInputDialog(null, "Input the new device ID/Name");

        if (newID != null && !newID.isBlank()) {
          networkManager.createRequest(userDevice.getIP(), shmanagerIP, "RENAME_DEVICE", new String[] {
              devID, newID
          }).send();

          refreshGraph();
          JOptionPane.getRootFrame().dispose();
        }
      });
      renamePanel.add(renameButton);
      configPanel.add(renamePanel);

      JButton refreshButton = new JButton("Refresh");
      refreshButton.addActionListener(e -> {
        refreshValues(values, rawCodes, node.deviceIP, valuesPanel, valueMap);
        JOptionPane.getRootFrame().pack();
        JOptionPane.getRootFrame().setVisible(true);
      });

      JButton applyButton = new JButton("Apply");
      applyButton.addActionListener(e -> {
        for (Value val : values) {
          if (val.setter && valueMap.containsKey(val)) {
            String txt = getInputValueString(valueMap.get(val));
            networkManager.createRequest(userDevice.getIP(), node.deviceIP, "SET_" +
                val.name, new String[] { txt }).send();
          }
        }
        JOptionPane.getRootFrame().dispose();
      });

      JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(e -> {
        JOptionPane.getRootFrame().dispose();
      });

      Object[] options = new Object[] {
          cancelButton, applyButton, refreshButton
      };

      JOptionPane.showOptionDialog(
          null,
          configPanel,
          "Configure " + devID + " (" + node.deviceIP.getAddressString() + ")",
          JOptionPane.DEFAULT_OPTION,
          JOptionPane.INFORMATION_MESSAGE,
          null,
          options,
          options[0]);
    });

    graphPanel.setEdgeConfigHandler((edge, isNew) -> {
      if (isNew) {
        refreshGraph();
      }
      SwingUtilities.invokeLater(() -> {
        JButton deleteBtn = new JButton("Delete");
        JButton okBtn = new JButton("Ok");
        JButton cancelBtn = new JButton("Cancel");

        JOptionPane pane = new JOptionPane(null,
            JOptionPane.QUESTION_MESSAGE,
            JOptionPane.DEFAULT_OPTION);

        okBtn.addActionListener(e -> pane.setValue(okBtn));
        cancelBtn.addActionListener(e -> pane.setValue(cancelBtn));
        deleteBtn.addActionListener(e -> pane.setValue(deleteBtn));

        if (isNew) {
          pane.setOptions(new Object[] { cancelBtn, okBtn });
        } else {
          pane.setOptions(new Object[] { cancelBtn, okBtn, deleteBtn });
        }

        JDialog dialog = pane.createDialog("Logic Config");
        JPanel dialogPanel = createLogicConfigPanel(edge, pane, dialog);
        if (dialogPanel == null) {
          return;
        }

        pane.setMessage(dialogPanel);

        dialog.pack();
        dialog.setVisible(true);
      });
    });

    JPanel topPanel = new JPanel(new BorderLayout());
    JButton refreshButton = new JButton("Refresh");
    refreshButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        refreshGraph();
      }
    });
    JButton addDeviceButton = new JButton("Add device");

    JPanel addDevicePanel = new JPanel();
    addDevicePanel.setLayout(new BoxLayout(addDevicePanel, BoxLayout.Y_AXIS));
    JPanel addIpPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    String ipPrefix = networkManager.getBroadcastAddress().getAddressString().substring(0, networkManager.getBroadcastAddress().getAddressString().length() - 3);
    JLabel ipLabel = new JLabel("IP: " + ipPrefix);
    JFormattedTextField ipField = new JFormattedTextField(NumberFormat.getIntegerInstance());
    ipField.setColumns(3);
    addIpPanel.add(ipLabel);
    addIpPanel.add(ipField);

    JPanel addIdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JLabel idLabel = new JLabel("ID/Name: ");
    JTextField idField = new JTextField(12);
    addIdPanel.add(idLabel);
    addIdPanel.add(idField);

    addDevicePanel.add(addIpPanel);
    addDevicePanel.add(addIdPanel);


    JButton addButton = new JButton("Add");
    addButton.addActionListener(e -> {
      System.out.println(ipPrefix + Integer.parseInt(ipField.getText()));
      networkManager.createRequest(userDevice.getIP(), shmanagerIP, "ADD_DEVICE", new String[] {
              new IP(ipPrefix + Integer.parseInt(ipField.getText())).getAddressString(), idField.getText()
      }).send();
      refreshGraph();
      JOptionPane.getRootFrame().dispose();
    });

    JButton cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(e -> {
      JOptionPane.getRootFrame().dispose();
    });

    Object[] options = new Object[] {
            addButton, cancelButton
    };

    addDeviceButton.addActionListener(e -> {
      JOptionPane.showOptionDialog(
              null,
              addDevicePanel,
              "Add device",
              JOptionPane.DEFAULT_OPTION,
              JOptionPane.INFORMATION_MESSAGE,
              null,
              options,
              options[0]);
    });
    topPanel.add(refreshButton, BorderLayout.WEST);
    topPanel.add(addDeviceButton, BorderLayout.EAST);
    topPanel.setSize(100, 100);

    add(topPanel, BorderLayout.NORTH);
    add(graphPanel, BorderLayout.CENTER);
  }

  protected void refreshGraph() {
    SwingUtilities.invokeLater(() -> {
      scanNetwork();
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

  private String capitalizeString(String str) {
    return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
  }

  private void findSHManager() {
    shmanagerIP = new IP(networkManager.createRequest(userDevice.getIP(), networkManager.getBroadcastAddress(),
        "FINDSHMANAGER", new String[] {}).send().getResult());
  };

  private JPanel createLogicConfigPanel(Edge edge, JOptionPane pane, JDialog dialog) {
    JPanel dialogPanel = new JPanel();
    dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
    JPanel conditionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    // conditionPanel.setPreferredSize(new Dimension(600, 250));

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

    // Font labelFont = new Font("Serif", Font.BOLD, 20);
    JLabel ifLabel = new JLabel("If (" + fromDevID + "): ");
    // ifLabel.setFont(labelFont);
    conditionPanel.add(ifLabel);

    JLabel thenLabel = new JLabel("Then (" + toDevID + "): ");
    // thenLabel.setFont(labelFont);
    actionPanel.add(thenLabel);

    condition = generateCondition(rawFrom, edge.logicData.conditionCode, edge.logicData.conditionValue, false,
        fromDevID);
    if (condition == null) {
      return null;
    }

    JComboBox condType = new JComboBox(condition.availableTypes);

    if (edge.logicData.conditionType != null) {
      condType.setSelectedItem(
          switch (edge.logicData.conditionType) {
            case "EQUAL" -> "Equals";
            case "NOT_EQUAL" -> "Not equals";
            case "GREATER_THAN" -> "Greater than";
            case "GREATER_EQUAL" -> "Greater than or equal to";
            case "LESS_THAN" -> "Less than";
            case "LESS_EQUAL" -> "Less than or equal to";
            default -> "Equals";
          });
    }

    condition.code.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        conditionPanel.removeAll();

        conditionPanel.add(ifLabel);

        updateCondition(rawFrom, condition);

        JComboBox condType = new JComboBox(condition.availableTypes);
        if (edge.logicData.conditionType != null) {
          condType.setSelectedItem(
              switch (edge.logicData.conditionType) {
                case "EQUAL" -> "Equals";
                case "NOT_EQUAL" -> "Not equals";
                case "GREATER_THAN" -> "Greater than";
                case "GREATER_EQUAL" -> "Greater than or equal to";
                case "LESS_THAN" -> "Less than";
                case "LESS_EQUAL" -> "Less than or equal to";
                default -> "Equals";
              });
        }

        conditionPanel.add(condition.code);
        conditionPanel.add(condType);
        conditionPanel.add(condition.value);

        conditionPanel.revalidate();
        conditionPanel.repaint();
        pane.revalidate();
        pane.repaint();
        dialog.pack();
      }
    });

    conditionPanel.add(condition.code);
    conditionPanel.add(condType);
    conditionPanel.add(condition.value);

    r = networkManager.createRequest(userDevice.getIP(),
        edge.to.deviceIP, "ADVERT", new String[] {});
    r.send();
    String rawTo = r.getResult();

    action = generateCondition(rawTo, edge.logicData.actionCode, edge.logicData.actionParams, true, toDevID);
    if (action == null) {
      return null;
    }

    action.code.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        actionPanel.removeAll();

        actionPanel.add(thenLabel);
        updateCondition(rawTo, action);
        actionPanel.add(action.code);
        actionPanel.add(action.value);

        actionPanel.revalidate();
        actionPanel.repaint();
        pane.revalidate();
        pane.repaint();
        dialog.pack();
      }
    });

    actionPanel.add(action.code);
    actionPanel.add(action.value);

    dialogPanel.add(conditionPanel);
    dialogPanel.add(actionPanel);

    JPanel prioPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JLabel prioLabel = new JLabel("Priority: ");
    JFormattedTextField prioVal = new JFormattedTextField(NumberFormat.getIntegerInstance());
    prioVal.setColumns(3);
    if (edge.logicData.priority != null)
      prioVal.setValue(Integer.valueOf(edge.logicData.priority));
    else
      prioVal.setValue(0);

    prioPanel.add(prioLabel);
    prioPanel.add(prioVal);

    dialogPanel.add(prioPanel);

    // Save changes when closing
    pane.addPropertyChangeListener(evt -> {
      if (evt.getPropertyName().equals(JOptionPane.VALUE_PROPERTY)
          && evt.getNewValue() != null
          && evt.getNewValue() != JOptionPane.UNINITIALIZED_VALUE) {

        if (evt.getNewValue().equals(pane.getOptions()[1] /* OK button */)) {
          // OK
          if (!edge.logicData.isEmpty()) {
            networkManager.createRequest(userDevice.getIP(), shmanagerIP, "DEL_LOGIC", new String[] {
                edge.to.deviceIP.getAddressString(),
                "SET_" + ((String) edge.logicData.actionCode).toUpperCase(),
                edge.logicData.actionParams,

                edge.from.deviceIP.getAddressString(),
                "GET_" + ((String) edge.logicData.conditionCode).toUpperCase(),
                edge.logicData.conditionType,
                edge.logicData.conditionValue,
            }).send();
          }

          networkManager.createRequest(userDevice.getIP(), shmanagerIP, "ADD_LOGIC", new String[] {
              edge.to.deviceIP.getAddressString(),
              "SET_" + ((String) action.code.getSelectedItem()).toUpperCase(),
              "[" + getInputValueString(action.value) + "]",

              edge.from.deviceIP.getAddressString(),
              "GET_" + ((String) condition.code.getSelectedItem()).toUpperCase(),
              switch ((String) condType.getSelectedItem()) {
                case "Equals" -> "EQUAL";
                case "Not equals" -> "NOT_EQUAL";
                case "Greater than" -> "GREATER_THAN";
                case "Greater than or equal to" -> "GREATER_EQUAL";
                case "Less than" -> "LESS_THAN";
                case "Less than or equal to" -> "LESS_EQUAL";
                default -> "EQUAL";
              },
              getInputValueString(condition.value),
              String.valueOf(prioVal.getValue())
          }).send();

          refreshGraph();
        } else if (evt.getNewValue().equals(pane.getOptions()[0] /* Cancel button */)) {
          // Cancelled
        } else if (pane.getOptions().length >= 3
            && evt.getNewValue().equals(pane.getOptions()[2] /* Delete button */)) {
          // Delete
          // TODO: CHANGE TO USE THE edge.logicData values instead of the current
          // temporary
          networkManager.createRequest(userDevice.getIP(), shmanagerIP, "DEL_LOGIC", new String[] {
              edge.to.deviceIP.getAddressString(),
              "SET_" + ((String) edge.logicData.actionCode).toUpperCase(),
              edge.logicData.actionParams,

              edge.from.deviceIP.getAddressString(),
              "GET_" + ((String) edge.logicData.conditionCode).toUpperCase(),
              edge.logicData.conditionType,
              edge.logicData.conditionValue,
          }).send();

          refreshGraph();
        }

        dialog.dispose();
      }
    });

    return dialogPanel;
  }

  private void generateGraph() {
    model.clearEdges();
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

    // The logics stored one after the other in 6 element chunks
    String[] combined = parseStringArray(logics);

    for (int i = 0; i < combined.length; i += 8) {
      if (i >= combined.length - 7)
        break;

      String ipA = combined[i + 3];
      String ipB = combined[i];

      Edge edge = model.addEdge(nodes.get(ipA), nodes.get(ipB));

      edge.logicData.actionCode = capitalizeString(combined[i + 1].replaceFirst("^SET_", ""));
      edge.logicData.actionParams = combined[i + 2];

      edge.logicData.conditionCode = capitalizeString(combined[i + 4].replaceFirst("^GET_", ""));
      edge.logicData.conditionType = combined[i + 5];
      edge.logicData.conditionValue = combined[i + 6];
      edge.logicData.priority = combined[i + 7];
    }
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
    String c = code.toUpperCase();
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

  private class ConditionStructure {
    String[] availableTypes;
    JComboBox code;
    JComponent value;
  }

  private ConditionStructure generateCondition(String rawCodes, String code, String defaultValue, boolean setter,
      String devID) {
    if (code != null)
      code = code.toUpperCase();
    ConditionStructure struct = new ConditionStructure();
    struct.code = generateValuesCombo(rawCodes, code, setter, devID);
    if (struct.code == null) {
      return null;
    }

    ArrayList<String> availableConditionTypes = new ArrayList<String>(Arrays.asList(new String[] {
        "Equals",
        "Not equals",
        "Greater than",
        "Less than",
        "Greater than or equal to",
        "Less than or equal to"
    }));

    struct.value = generateCondValueField(((String) struct.code.getSelectedItem()).toUpperCase(), rawCodes,
        defaultValue,
        availableConditionTypes);
    struct.availableTypes = availableConditionTypes.toArray(new String[] {});
    return struct;
  }

  private void updateCondition(String rawCodes, ConditionStructure condition) {
    ArrayList<String> availableConditionTypes = new ArrayList<String>(Arrays.asList(new String[] {
        "Equals",
        "Not equals",
        "Greater than",
        "Less than",
        "Greater than or equal to",
        "Less than or equal to"
    }));

    condition.value = generateCondValueField(((String) condition.code.getSelectedItem()).toUpperCase(), rawCodes,
        null,
        availableConditionTypes);
    condition.availableTypes = availableConditionTypes.toArray(new String[] {});
  }

  private JComboBox generateValuesCombo(String rawCodes, String defaultValue, boolean setter, String devID) {
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
      if (setter ? val.setter : val.getter) {
        res.add(capitalizeString(val.name));
      }
    }
    if (res.size() == 0) {
      if (setter) {
        JOptionPane.showMessageDialog(null, "Device " + devID
            + " does not have any parameters that can be changed by a logic.\nUse a different device.");
      } else {
        JOptionPane.showMessageDialog(null, "Device " + devID
            + " does not have any parameters that can be used as the condition for a logic.\nUse a different device.");
      }
      return null;
    }

    JComboBox box = new JComboBox(res.toArray(new String[] {}));
    if (defaultValue != null) {
      box.setSelectedItem(capitalizeString(defaultValue));
    }
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
    if (input instanceof JButton) {
      // Button for color choosing
      return ((JButton) input).getText();
    }

    return "MISSING TYPE!!!";
  }

  private JComponent generateCondValueField(String paramName, String rawCodes, String defaultValue,
      ArrayList<String> comparisonTypes) {
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
            JFormattedTextField tf = new JFormattedTextField(NumberFormat.getIntegerInstance());
            tf.setColumns(10);
            if (defaultValue != null)
              tf.setValue(Integer.parseInt(defaultValue));
            return tf;
          case "FLOAT":
            tf = new JFormattedTextField(NumberFormat.getNumberInstance());
            tf.setColumns(10);
            if (defaultValue != null)
              tf.setValue(Float.parseFloat(defaultValue));
            return tf;
          case "STRING":
            JTextField field = new JTextField(10);
            if (defaultValue != null)
              field.setText(defaultValue);
            comparisonTypes.remove("Greater than");
            comparisonTypes.remove("Greater than or equal to");
            comparisonTypes.remove("Less than");
            comparisonTypes.remove("Less than or equal to");
            return field;
          case "COLOR":
            JButton colorButton = new JButton("#FFFFFF");

            if (defaultValue != null)
              colorButton.setText(defaultValue);

            colorButton.setBackground(Color.decode(colorButton.getText()));
            colorButton.setContentAreaFilled(false);
            colorButton.setOpaque(true);

            colorButton.addActionListener(e -> {
              Color chosen = JColorChooser.showDialog(
                  colorButton,
                  "Choose Color",
                  Color.decode(colorButton.getText()));

              if (chosen != null) {
                String color = String.format("#%06X", (0xFFFFFF & chosen.getRGB()));
                colorButton.setText(color);
                colorButton.setBackground(Color.decode(color));
              }
            });

            comparisonTypes.remove("Greater than");
            comparisonTypes.remove("Greater than or equal to");
            comparisonTypes.remove("Less than");
            comparisonTypes.remove("Less than or equal to");
            return colorButton;
          case "RANGE":
            // Range from 0 to 1
            JSlider slider = new JSlider();
            slider.setPaintTrack(true);
            slider.setPaintTicks(true);
            Hashtable<Integer, JLabel> table = new Hashtable<>();
            table.put(0, new JLabel("0"));
            table.put(50, new JLabel("50"));
            table.put(100, new JLabel("100"));
            slider.setLabelTable(table);
            slider.setPaintLabels(true);
            if (defaultValue != null)
              slider.setValue((int) (Double.parseDouble(defaultValue) * 100));
            return slider;
          case "BOOL":
            JCheckBox checkBox = new JCheckBox();
            if (defaultValue != null)
              checkBox.setSelected(defaultValue.equals("true"));
            comparisonTypes.remove("Greater than");
            comparisonTypes.remove("Greater than or equal to");
            comparisonTypes.remove("Less than");
            comparisonTypes.remove("Less than or equal to");
            return checkBox;
          default:
            field = new JTextField(10);
            if (defaultValue != null)
              field.setText(defaultValue);
            return field;
        }
      }
    }

    return new JTextField(10);
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
