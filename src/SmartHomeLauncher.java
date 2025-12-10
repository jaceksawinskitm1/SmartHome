import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

// IMPORTY Z TWOICH PAKIETÓW (Zakładam, że struktura pakietów istnieje)
import Network.*;
import Devices.*;

/**
 * Główna klasa uruchomieniowa.
 * Inicjalizuje środowisko (NetworkManager, SHManager) i uruchamia oba GUI.
 */
public class SmartHomeLauncher {

    // Globalne instancje (singletony w kontekście aplikacji)
    private static NetworkManager networkManager;
    private static SHManager shManager;
    private static UserDevice userDevice;

    public static void main(String[] args) {
        // 1. Inicjalizacja Backendu
        setupBackend();

        // 2. Ustawienie wyglądu (Look and Feel)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // 3. Uruchomienie okien w wątku Swinga
        SwingUtilities.invokeLater(() -> {
            // Okno Symulacji (Devices Package Access)
            SimulationFrame simFrame = new SimulationFrame(shManager, networkManager);
            simFrame.setVisible(true);

            // Okno Użytkownika (Network Package Access Only)
            UserControlFrame userFrame = new UserControlFrame(userDevice, networkManager);
            userFrame.setVisible(true);
        });
    }

    private static void setupBackend() {
        System.out.println("Inicjalizacja systemu...");
        try {
            networkManager = new NetworkManager();
            shManager = new SHManager(networkManager);
            // Tworzymy urządzenie użytkownika (podłączone do LAN)
            userDevice = new UserDevice(true, networkManager, shManager);
            userDevice.leaseIP(); // Pobranie IP dla użytkownika
            System.out.println("System gotowy. User IP: " + userDevice.getIP().getAddressString());
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Błąd inicjalizacji backendu: " + e.getMessage());
            System.exit(1);
        }
    }
}

// ==================================================================================
// OKNO 1: ŚRODOWISKO TESTOWE (SIMULATION)
// Pozwala tworzyć urządzenia i manipulować ich stanem "fizycznym"
// ==================================================================================
class SimulationFrame extends JFrame {
    private final SHManager shManager;
    private final NetworkManager networkManager;
    private final DefaultListModel<String> deviceListModel;
    private final Map<String, SHDevice> deviceMap; // Przechowuje referencje do obiektów

    // Panel właściwości wybranego urządzenia
    private JPanel propertiesPanel;

    public SimulationFrame(SHManager shManager, NetworkManager nm) {
        this.shManager = shManager;
        this.networkManager = nm;
        this.deviceMap = new HashMap<>();
        this.deviceListModel = new DefaultListModel<>();

        setTitle("Środowisko Symulacyjne (Admin/God Mode)");
        setSize(500, 600);
        setLocation(50, 50);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel lewy: Lista urządzeń
        JSplitPane splitPane = new JSplitPane();
        JList<String> deviceList = new JList<>(deviceListModel);
        deviceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        deviceList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String key = deviceList.getSelectedValue();
                if (key != null) showDeviceProperties(deviceMap.get(key));
            }
        });
        splitPane.setLeftComponent(new JScrollPane(deviceList));

        // Panel prawy: Właściwości symulacji
        propertiesPanel = new JPanel();
        propertiesPanel.setLayout(new BoxLayout(propertiesPanel, BoxLayout.Y_AXIS));
        splitPane.setRightComponent(new JScrollPane(propertiesPanel));
        splitPane.setDividerLocation(200);
        add(splitPane, BorderLayout.CENTER);

        // Panel dolny: Dodawanie urządzeń
        JPanel bottomPanel = new JPanel();
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{
                "Light", "Thermometer", "Blind", "Heater", "AirConditioner", "AudioDevice"
        });
        JTextField idField = new JTextField("dev_01", 10);
        JButton addButton = new JButton("Dodaj Urządzenie");

        addButton.addActionListener(e -> {
            String type = (String) typeCombo.getSelectedItem();
            String id = idField.getText();
            createAndRegisterDevice(type, id);
        });

        bottomPanel.add(new JLabel("ID:"));
        bottomPanel.add(idField);
        bottomPanel.add(typeCombo);
        bottomPanel.add(addButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void createAndRegisterDevice(String type, String id) {
        if (deviceMap.containsKey(id)) {
            JOptionPane.showMessageDialog(this, "ID zajęte!");
            return;
        }

        try {
            SHDevice newDevice = null;
            // Fabryka urządzeń
            switch (type) {
                case "Light": newDevice = new Light(); break;
                case "Thermometer": newDevice = new Thermometer(); break;
                case "Blind": newDevice = new Blind(); break;
                case "Heater": newDevice = new Heater(); break;
                case "AirConditioner": newDevice = new AirConditioner(); break;
                case "AudioDevice": newDevice = new AudioDevice(); break;
            }

            if (newDevice != null) {
                // Rejestracja w systemie (przydziela IP automatycznie)
                shManager.registerDevice(id, newDevice);

                deviceMap.put(id, newDevice);
                String listLabel = id + " [" + newDevice.getIP().getAddressString() + "]";
                deviceListModel.addElement(listLabel);

                // Mapujemy też etykietę listy na ID w mapie, dla uproszczenia tutaj przyjmijmy
                // że trzymamy w mapie ID, a lista wyświetla full string.
                // W produkcji lepiej użyć obiektu wrappera w JList.
                deviceMap.put(listLabel, newDevice);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Błąd tworzenia: " + ex.getMessage());
        }
    }

    private void showDeviceProperties(SHDevice device) {
        propertiesPanel.removeAll();
        propertiesPanel.setBorder(new TitledBorder(device.getClass().getSimpleName() + " Simulation"));

        // Generowanie kontrolek w zależności od typu (Rzutowanie w dół)
        if (device instanceof Thermometer) {
            Thermometer t = (Thermometer) device;
            JLabel valLabel = new JLabel("Temp: " + t.getTemperature());
            JSlider slider = new JSlider(0, 40, (int)t.getTemperature());
            slider.addChangeListener(e -> {
                t._changeTemp(slider.getValue());
                valLabel.setText("Temp: " + slider.getValue());
            });
            propertiesPanel.add(new JLabel("Symuluj Temperaturę otoczenia:"));
            propertiesPanel.add(slider);
            propertiesPanel.add(valLabel);
        }
        else if (device instanceof Light) {
            Light l = (Light) device;
            JCheckBox state = new JCheckBox("Fizyczny stan ON/OFF", false); // tu brakuje metody getState w Light publicznej?
            // Zakładam istnienie getterów symulacyjnych lub pól
            propertiesPanel.add(new JLabel("Symulacja fizyczna światła (podgląd)"));
        }
        // ... Implementacja dla innych typów ...

        propertiesPanel.revalidate();
        propertiesPanel.repaint();
    }
}

// ==================================================================================
// OKNO 2: PANEL UŻYTKOWNIKA (USER GUI)
// Używa tylko pakietu Network (NetworkManager, IP, Request)
// ==================================================================================
class UserControlFrame extends JFrame {
    private final NetworkManager networkManager;
    private final UserDevice userDevice;

    // UI Elements
    private final DefaultListModel<String> discoveredIPsModel;
    private final JPanel controlsPanel;
    private final JTextArea logArea;
    private final JLabel statusLabel;

    public UserControlFrame(UserDevice dev, NetworkManager nm) {
        this.userDevice = dev;
        this.networkManager = nm;

        setTitle("Panel Sterowania Smart Home (Klient)");
        setSize(800, 600);
        setLocation(600, 50);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- Pasek Statusu ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Status: Połączono (" + userDevice.getIP().getAddressString() + ")");
        JButton reconnectBtn = new JButton("Połącz/Rozłącz");
        reconnectBtn.addActionListener(e -> toggleConnection());
        topPanel.add(statusLabel);
        topPanel.add(reconnectBtn);
        add(topPanel, BorderLayout.NORTH);

        // --- Panel Lewy: Lista Urządzeń (Skaner) ---
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(new TitledBorder("Wykryte Urządzenia"));

        discoveredIPsModel = new DefaultListModel<>();
        JList<String> ipList = new JList<>(discoveredIPsModel);
        JButton scanBtn = new JButton("Skanuj sieć (Broadcast)");

        scanBtn.addActionListener(e -> performNetworkScan());

        ipList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && ipList.getSelectedValue() != null) {
                String ipStr = ipList.getSelectedValue();
                loadDeviceControls(ipStr);
            }
        });

        leftPanel.add(new JScrollPane(ipList), BorderLayout.CENTER);
        leftPanel.add(scanBtn, BorderLayout.SOUTH);
        leftPanel.setPreferredSize(new Dimension(200, 0));
        add(leftPanel, BorderLayout.WEST);

        // --- Panel Centralny: Kontrolki ---
        controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
        JScrollPane centerScroll = new JScrollPane(controlsPanel);
        centerScroll.setBorder(new TitledBorder("Sterowanie Urządzeniem"));
        add(centerScroll, BorderLayout.CENTER);

        // --- Panel Dolny: Logi ---
        logArea = new JTextArea(5, 50);
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(new TitledBorder("Logi sieciowe"));
        add(logScroll, BorderLayout.SOUTH);
    }

    private void toggleConnection() {
        if (userDevice.hasLanAccess()) {
            userDevice.disconnectFromLan();
            statusLabel.setText("Status: Rozłączono");
            log("Rozłączono z LAN.");
        } else {
            userDevice.connectToLan();
            statusLabel.setText("Status: Połączono (" + userDevice.getIP().getAddressString() + ")");
            log("Połączono z LAN.");
        }
    }

    /**
     * Skanowanie sieci. 
     * W prawdziwej sieci wysyła się broadcast ping.
     * Tutaj zrobimy pętlę po puli adresów 192.168.0.x i spróbujemy wysłać ping/ADVERT.
     */
    private void performNetworkScan() {
        discoveredIPsModel.clear();
        log("Rozpoczynanie skanowania...");

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                // Proste skanowanie zakresu (zakładamy mały zakres dla demo)
                // W Twojej klasie IP adresy są intami lub byte[]. Zakładam IP.getAddressString()
                // Tutaj robimy "hack" dla demo: iterujemy po 192.168.0.2 do .20

                String base = "192.168.0.";
                IP myIP = userDevice.getIP();

                for (int i = 2; i <= 20; i++) {
                    String targetIPStr = base + i;
                    // Pomiń własne IP
                    if (targetIPStr.equals(myIP.getAddressString())) continue;

                    try {
                        IP target = new IP(targetIPStr);
                        // Próba wysłania ADVERT, aby sprawdzić czy ktoś żyje
                        // Używamy metody createRequest z Managera
                        NetworkManager.Request req = networkManager.createRequest(myIP, target, "ADVERT", new String[]{});
                        req.send();
                        if (req.hasResult()) {
                            publish(targetIPStr);
                        }
                    } catch (Exception ignored) {
                        // Timeout lub brak urządzenia
                    }
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String ip : chunks) {
                    discoveredIPsModel.addElement(ip);
                }
            }

            @Override
            protected void done() {
                log("Skanowanie zakończone.");
            }
        };
        worker.execute();
    }

    /**
     * Kluczowa metoda: Buduje GUI dynamicznie na podstawie odpowiedzi ADVERT.
     */
    private void loadDeviceControls(String targetIpStr) {
        controlsPanel.removeAll();
        controlsPanel.add(new JLabel("Ładowanie interfejsu dla: " + targetIpStr));
        controlsPanel.revalidate();
        controlsPanel.repaint();

        new Thread(() -> {
            try {
                IP source = userDevice.getIP();
                IP target = new IP(targetIpStr);

                // 1. Wyślij ADVERT
                NetworkManager.Request req = networkManager.createRequest(source, target, "ADVERT", new String[]{});
                req.send();
                String resultRaw = req.getResult(); // np. "[GET_TEMP, SET_TEMP]"

                // Parsowanie wyniku
                String clean = resultRaw.replace("[", "").replace("]", "");
                String[] codes = clean.split(",");

                SwingUtilities.invokeLater(() -> buildDynamicPanel(codes, source, target));

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    controlsPanel.removeAll();
                    controlsPanel.add(new JLabel("Błąd połączenia: " + e.getMessage()));
                    controlsPanel.revalidate();
                });
            }
        }).start();
    }

    private void buildDynamicPanel(String[] codes, IP source, IP target) {
        controlsPanel.removeAll();
        controlsPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;

        controlsPanel.add(new JLabel("<html><h3>Dostępne Funkcje (Urządzenie " + target.getAddressString() + ")</h3></html>"), gbc);
        gbc.gridy++;

        for (String code : codes) {
            code = code.trim();
            if (code.isEmpty()) continue;

            JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            rowPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

            JLabel codeLabel = new JLabel(code);
            codeLabel.setPreferredSize(new Dimension(150, 20));
            rowPanel.add(codeLabel);

            // Jeśli to GETTER
            if (code.startsWith("GET_")) {
                JButton getBtn = new JButton("Pobierz");
                JLabel resultLabel = new JLabel("Wait...");
                String finalCode = code;

                getBtn.addActionListener(e -> {
                    sendNetworkRequest(source, target, finalCode, new String[]{}, resultLabel);
                });
                rowPanel.add(getBtn);
                rowPanel.add(resultLabel);
            }
            // Jeśli to SETTER lub AKCJA
            else {
                JTextField paramField = new JTextField(10);
                JButton setBtn = new JButton("Wyślij");
                String finalCode = code;

                setBtn.addActionListener(e -> {
                    String paramVal = paramField.getText();
                    String[] params = paramVal.isEmpty() ? new String[]{} : paramVal.split(",");
                    sendNetworkRequest(source, target, finalCode, params, null);
                });
                rowPanel.add(new JLabel("Param:"));
                rowPanel.add(paramField);
                rowPanel.add(setBtn);
            }

            gbc.gridy++;
            controlsPanel.add(rowPanel, gbc);
        }

        controlsPanel.revalidate();
        controlsPanel.repaint();
    }

    private void sendNetworkRequest(IP source, IP target, String code, String[] params, JLabel resultTarget) {
        new Thread(() -> {
            try {
                log("Wysyłanie: " + code + " do " + target.getAddressString());
                NetworkManager.Request req = networkManager.createRequest(source, target, code, params);
                req.send();

                String result = "OK";
                if (req.hasResult()) {
                    result = req.getResult();
                }

                String finalResult = result;
                log("Otrzymano: " + finalResult);

                if (resultTarget != null) {
                    SwingUtilities.invokeLater(() -> resultTarget.setText(finalResult));
                }
            } catch (Exception e) {
                log("Błąd: " + e.getMessage());
                if (resultTarget != null) {
                    SwingUtilities.invokeLater(() -> resultTarget.setText("Error"));
                }
            }
        }).start();
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> logArea.append(msg + "\n"));
    }
}