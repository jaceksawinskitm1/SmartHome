package UI;

import Network.IP;
import Network.NetworkManager;
import Network.UserDevice;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Objects;

public class UserUI extends JFrame {
    private final NetworkManager networkManager;
    private final UserDevice userDevice;
    private final DefaultListModel<String> discoveredIPsModel;
    private final JPanel controlsPanel;
    private final JTextArea logArea;

    public UserUI(UserDevice dev, NetworkManager nm) {
        this.userDevice = dev;
        this.networkManager = nm;

        setTitle("Panel Użytkownika (Smart Home App)");
        setSize(450, 600);
        setLocation(600, 50);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // GÓRA - STATUS
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        JLabel status = new JLabel("Moje IP: " + userDevice.getIP().getAddressString());
        JButton scanBtn = new JButton("Skanuj Sieć");
        scanBtn.addActionListener(e -> scanNetwork());
        top.add(status, BorderLayout.WEST);
        top.add(scanBtn, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // LEWO - LISTA IP
        discoveredIPsModel = new DefaultListModel<>();
        JList<String> ipList = new JList<>(discoveredIPsModel);
        ipList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ipList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && ipList.getSelectedValue() != null) {
                loadDeviceUI(ipList.getSelectedValue());
            }
        });
        JScrollPane listScroll = new JScrollPane(ipList);
        listScroll.setPreferredSize(new Dimension(120, 0));
        listScroll.setBorder(new TitledBorder("Wykryte"));
        add(listScroll, BorderLayout.WEST);

        // ŚRODEK - DYNAMICZNE STEROWANIE
        controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
        JScrollPane centerScroll = new JScrollPane(controlsPanel);
        centerScroll.setBorder(new TitledBorder("Panel Sterowania"));
        add(centerScroll, BorderLayout.CENTER);

        // DÓŁ - LOGI
        logArea = new JTextArea(5, 40);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(logArea), BorderLayout.SOUTH);
    }

    private void scanNetwork() {
        discoveredIPsModel.clear();
        log("Rozpoczynanie skanowania...");
        // Skanujemy zakres .2 do .20 dla demo
        for (int i = 2; i <= 20; i++) {
            String targetIP = "192.168.0." + i;
            new Thread(() -> {
                try {
                    NetworkManager.Request r = networkManager.createRequest(userDevice.getIP(), new IP(targetIP), "ADVERT", new String[]{});
                    r.send();
                    if (!r.getResult().startsWith("ERROR")) {
                        SwingUtilities.invokeLater(() -> {
                            if(!discoveredIPsModel.contains(targetIP)) discoveredIPsModel.addElement(targetIP);
                        });
                    }
                } catch (Exception e) {}
            }).start();
        }
    }

    private void loadDeviceUI(String ipStr) {
        controlsPanel.removeAll();
        controlsPanel.add(new JLabel("Ładowanie interfejsu..."));
        controlsPanel.revalidate();
        controlsPanel.repaint();

        new Thread(() -> {
            IP target = new IP(ipStr);
            NetworkManager.Request r = networkManager.createRequest(userDevice.getIP(), target, "ADVERT", new String[]{});
            r.send();
            String raw = r.getResult();

            SwingUtilities.invokeLater(() -> {
                controlsPanel.removeAll();
                if (raw.startsWith("ERROR") || raw.equals("TIMEOUT")) {
                    controlsPanel.add(new JLabel("Błąd połączenia."));
                } else {
                    buildControls(raw, target);
                }
                controlsPanel.revalidate();
                controlsPanel.repaint();
            });
        }).start();
    }

    private class Value {
        String name;
        boolean setter;
        boolean getter;

        public Value(String name, boolean setter, boolean getter) {
            this.name = name;
            this.getter = getter;
            this.setter = setter;
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

        for (Value v : values) {
            if (Objects.equals(v.name, c)) {
                v.setter |= setter;
                v.getter |= getter;
                return;
            }
        }

        values.add(new Value(c, setter, getter));
    }

    private void buildControls(String rawCodes, IP target) {
        String clean = rawCodes.replace("[", "").replace("]", "");
        String[] codes = clean.split(",");

        controlsPanel.add(new JLabel("<html><b>Urządzenie: " + target.getAddressString() + "</b></html>"));
        controlsPanel.add(Box.createVerticalStrut(10));

        ArrayList<Value> values = new ArrayList<>();
        for (String c : codes) {
            String code = c.trim();
            if (code.isEmpty()) continue;

            addValue(code, values);
        }

        for (Value val : values) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JPanel row = new JPanel(new GridLayout(2,2));
            row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
            JLabel lbl = new JLabel(val.name);
            lbl.setPreferredSize(new Dimension(100, 10));
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 10));

            panel.add(lbl);

            if (val.getter) {
                JButton btn = new JButton("Get");
                JLabel valLbl = new JLabel("?");
                btn.addActionListener(e -> sendRequest(target, "GET_" + val.name, new String[]{}, valLbl));
                row.add(btn);
                row.add(valLbl);
            }

            if (val.setter) {
                JTextField paramField = new JTextField(5);
                JButton setBtn = new JButton("Set");
                setBtn.addActionListener(e -> {
                    String txt = paramField.getText();
                    String[] params = txt.isEmpty() ? new String[]{} : txt.split(",");
                    sendRequest(target, "SET_" + val.name, params, null);
                });
                row.add(setBtn);
                row.add(paramField);
            }
            panel.add(row);
            controlsPanel.add(panel);
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
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}