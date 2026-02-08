import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import Network.*;
import Devices.*;
import UI.*;

/**
 * Główna klasa uruchomieniowa.
 * Inicjalizuje środowisko (NetworkManager, SHManager) i uruchamia oba GUI.
 */
public class SmartHomeLauncher {

    // Globalne instancje symulujące środowisko
    private static NetworkManager networkManager;
    private static SHManager shManager;
    private static UserDevice userDevice;



    public static void main(String[] args) {
        // 1. Inicjalizacja Backendu
        setupBackend();

        // 2. Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // 3. Uruchomienie GUI
        SwingUtilities.invokeLater(() -> {
            // Okno Admina/Boga (dostęp bezpośredni do obiektów)
            SimulationFrame simFrame = new SimulationFrame(shManager, networkManager);
            simFrame.setVisible(true);

            // Okno Użytkownika (dostęp tylko przez sieć)
            UserUI userFrame = new UserUI(userDevice, networkManager);
            userFrame.setVisible(true);
        });
    }

    private static void setupBackend() {
        networkManager = new NetworkManager();
        shManager = new SHManager(networkManager);
        userDevice = new UserDevice(true, networkManager, shManager);
        userDevice.leaseIP(); // User dostaje zazwyczaj .254
        System.out.println("Backend initialized.");
    }


    // ==================================================================================
    // GUI 1: SYMULACJA (Środowisko Testowe)
    // ==================================================================================

    static class SimulationFrame extends JFrame {
        private final SHManager shManager;
        private final NetworkManager networkManager;
        private final DefaultListModel<String> deviceListModel;
        private final Map<String, SHDevice> deviceMap;
        private JPanel propertiesPanel;

        public SimulationFrame(SHManager shManager, NetworkManager nm) {
            this.shManager = shManager;
            this.networkManager = nm;
            this.deviceMap = new HashMap<>();
            this.deviceListModel = new DefaultListModel<>();

            setTitle("Symulacja (Admin Environment)");
            setSize(500, 700);
            setLocation(50, 50);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new BorderLayout());

            // LISTA
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

            // PANEL WŁAŚCIWOŚCI
            propertiesPanel = new JPanel();
            propertiesPanel.setLayout(new BoxLayout(propertiesPanel, BoxLayout.Y_AXIS));
            splitPane.setRightComponent(new JScrollPane(propertiesPanel));
            splitPane.setDividerLocation(200);
            add(splitPane, BorderLayout.CENTER);

            // DODAWANIE URZĄDZEŃ
            JPanel bottomPanel = new JPanel();
            JComboBox<String> typeCombo = new JComboBox<>(new String[]{
                    "Light", "Thermometer", "Blind", "Heater", "AirConditioner", "AudioDevice",
                    "CCTV", "TV", "LightDetector", "GarageDoor", "CoffeeMachine"
            });
            JButton addButton = new JButton("Dodaj");
            JCheckBox autoConnect = new JCheckBox("Łączenie automatyczne");
            autoConnect.setSelected(true);

            addButton.addActionListener(e -> {
                String type = (String) typeCombo.getSelectedItem();
                createAndRegisterDevice(type, autoConnect.isSelected());
            });

            bottomPanel.add(typeCombo);
            bottomPanel.add(addButton);
            bottomPanel.add(autoConnect);
            add(bottomPanel, BorderLayout.SOUTH);

            // Timer do odświeżania UI (podgląd stanu na żywo)
            new Timer(500, e -> {
                String selected = deviceList.getSelectedValue();
                if(selected != null) showDeviceProperties(deviceMap.get(selected));
            }).start();
        }

        private void createAndRegisterDevice(String type, boolean autoConnect) {
            SHDevice newDevice = switch (type) {
                case "Light" -> new Light();
                case "Thermometer" -> new Thermometer();
                case "Blind" -> new Blind();
                case "Heater" -> new Heater();
                case "AirConditioner" -> new AirConditioner();
                case "AudioDevice" -> new AudioDevice();
                case "CCTV" -> new CCTV();
                case "TV" -> new TV();
                case "LightDetector" -> new LightDetector();
                case "GarageDoor" -> new GarageDoor();
                case "CoffeeMachine" -> new CoffeeMachine();
                default -> null;
            };

            if (newDevice != null) {
                //shManager.registerDevice(id, newDevice);
                newDevice.setAutoConnect(autoConnect);
                newDevice.setNetworkManager(networkManager);
                deviceMap.put(newDevice.getIP().getAddressString(), newDevice);
                deviceListModel.addElement(newDevice.getIP().getAddressString());
            }
        }

        SHDevice current = null;
        private void showDeviceProperties(SHDevice device) {
            if (device.equals(current))
                return;

            current = device;

            propertiesPanel.removeAll();
            propertiesPanel.setBorder(new TitledBorder(device.getClass().getSimpleName()));

            // --- RENDEROWANIE ZALEŻNE OD TYPU ---

            switch (device) {
                case Light l -> {
                    JCheckBox on = new JCheckBox("Włączony", l.getState());
                    on.addActionListener(e -> l.setState(on.isSelected()));
                    propertiesPanel.add(on);
                    propertiesPanel.add(new JLabel("Jasność: " + l.getBrightness()));
                    propertiesPanel.add(new JLabel("Kolor: " + l.getColor()));
                }
                case Thermometer t -> {
                    JLabel lbl = new JLabel("Aktualna Temp: " + String.format("%.1f", t.getTemperature()));
                    JSlider slider = new JSlider(-10, 40, (int) t.getTemperature());
                    slider.setMajorTickSpacing(10);
                    slider.setPaintTicks(true);
                    slider.addChangeListener(e -> t._changeTemp(slider.getValue()));
                    propertiesPanel.add(new JLabel("Symulacja otoczenia:"));
                    propertiesPanel.add(slider);
                    propertiesPanel.add(lbl);
                }
                case CCTV c -> {
                    JCheckBox on = new JCheckBox("Zasilanie", c.isOn());
                    on.addActionListener(e -> c.setStatus(on.isSelected()));
                    JCheckBox move = new JCheckBox("Symuluj RUCH", c.isMovementDetected());
                    move.addActionListener(e -> c.setMovement(move.isSelected()));

                    JPanel led = new JPanel();
                    led.setBackground(c.isOn() && c.isMovementDetected() ? Color.RED : Color.GRAY);
                    led.setPreferredSize(new Dimension(20, 20));
                    led.setBorder(BorderFactory.createLineBorder(Color.BLACK));

                    propertiesPanel.add(on);
                    propertiesPanel.add(move);
                    propertiesPanel.add(new JLabel("LED Detekcji:"));
                    propertiesPanel.add(led);
                }
                case TV tv -> {
                    JCheckBox on = new JCheckBox("Power", tv.isOn());
                    on.addActionListener(e -> tv.setStatus(on.isSelected()));
                    JSlider vol = new JSlider(0, 100, (int)(tv.getVolume() * 100));
                    vol.addChangeListener(e -> tv.setVolume(vol.getValue()));

                    propertiesPanel.add(on);
                    propertiesPanel.add(new JLabel("Głośność: " + tv.getVolume()));
                    propertiesPanel.add(vol);
                    propertiesPanel.add(new JLabel("Kanał: " + tv.getChannel()));
                }
                case GarageDoor gd -> {
                    JProgressBar vis = new JProgressBar(0, 100);
                    vis.setValue(100 - (int)(gd.getPosition() * 100)); // 100=Closed(full), 0=Open

                    vis.setString("Stan: " + gd.getState());
                    vis.setStringPainted(true);

                    JSlider pos = new JSlider(0, 100, (int)(gd.getPosition() * 100));
                    pos.setInverted(true); // 100 (zamknięte) na dole

                    pos.addChangeListener(e -> gd.setPosition(pos.getValue()));

                    JButton open = new JButton("Otwórz");
                    open.addActionListener(e -> gd.setState(GarageDoor.DoorState.OPEN));
                    JButton close = new JButton("Zamknij");
                    close.addActionListener(e -> gd.setState(GarageDoor.DoorState.CLOSED));

                    propertiesPanel.add(new JLabel("Wizualizacja:"));
                    propertiesPanel.add(vis);
                    propertiesPanel.add(new JLabel("Sterowanie ręczne:"));
                    propertiesPanel.add(pos);
                    JPanel btns = new JPanel();
                    btns.add(open);
                    btns.add(close);
                    propertiesPanel.add(btns);
                }
                case CoffeeMachine cm -> {
                    JProgressBar p = new JProgressBar(0, 100);
                    p.setValue((int)(cm.getProgress() * 100));
                    p.setStringPainted(true);
                    JLabel st = new JLabel("Status: " + cm.getStatus());

                    propertiesPanel.add(st);
                    propertiesPanel.add(p);

                }
                case LightDetector ld -> {
                    JCheckBox cb = new JCheckBox();
                    cb.setSelected(ld.getState());
                    cb.addActionListener(e -> ld.setState(cb.isSelected()));

                    JLabel icon = new JLabel((!ld.getState()) ? "☀️" : "🌙");
                    icon.setFont(new Font("SansSerif", Font.PLAIN, 40));
                    icon.setAlignmentX(Component.CENTER_ALIGNMENT);

                    propertiesPanel.add(new JLabel("Symulacja Pory Dnia:"));
                    propertiesPanel.add(cb);
                    propertiesPanel.add(icon);
                }
                default -> {
                    // Default fallback for simple devices
                    propertiesPanel.add(new JLabel("Generic Device: " + networkManager.createRequest(shManager.getIP(), device.getIP(), "ADVERT", new String[]{})));
                }
            }
            propertiesPanel.revalidate();
            propertiesPanel.repaint();
        }
    }
}
