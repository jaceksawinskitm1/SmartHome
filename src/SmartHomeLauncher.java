import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

// IMPORTY Z TWOICH PAKIETÓW (Zakładam, że struktura pakietów istnieje)
import Network.*;
import Devices.*;

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
            UserControlFrame userFrame = new UserControlFrame(userDevice, networkManager);
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
    // PAKIET: NETWORK (MOCK)
    // ==================================================================================

    static class IP {
        private String addr;
        public IP(String s) { this.addr = s; }
        public IP() { this.addr = "0.0.0.0"; }
        public String getAddressString() { return addr; }
        @Override public String toString() { return addr; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IP ip = (IP) o;
            return Objects.equals(addr, ip.addr);
        }
    }

    static class Request {
        private String result = "";
        private boolean done = false;
        private final String code;
        private final String[] params;
        private final NetworkManager nm;
        private final IP target;

        public Request(IP src, IP target, String code, String[] params, NetworkManager nm) {
            this.code = code;
            this.params = params;
            this.nm = nm;
            this.target = target;
        }

        public Request send() {
            new Thread(() -> {
                try {
                    Thread.sleep(20); // Symulacja opóźnienia sieci
                    SHDevice dev = nm.getDeviceByIP(target);
                    if (dev != null) {
                        if (code.equals("ADVERT")) {
                            result = dev.getAdvertisedCodes();
                        } else {
                            result = dev.handleRequest(code, params);
                        }
                    } else {
                        result = "ERROR: HOST UNREACHABLE";
                    }
                } catch (Exception e) {
                    result = "ERROR: " + e.getMessage();
                }
                done = true;
            }).start();
            return this;
        }

        public boolean hasResult() { return done; }
        public String getResult() {
            // Proste czekanie aktywne na potrzeby demo (w produkcji: callback/future)
            long start = System.currentTimeMillis();
            while (!done) {
                if (System.currentTimeMillis() - start > 2000) return "TIMEOUT";
                try { Thread.sleep(10); } catch (Exception e) {}
            }
            return result;
        }
    }

    static class NetworkManager {
        private Map<String, SHDevice> devices = new ConcurrentHashMap<>();
        private int ipCounter = 2; // DHCP start

        public Request createRequest(IP src, IP target, String code, String[] params) {
            return new Request(src, target, code, params, this);
        }

        public void registerDevice(SHDevice dev) {
            String ip = "192.168.0." + ipCounter++;
            dev.setIP(new IP(ip));
            devices.put(ip, dev);
        }

        public SHDevice getDeviceByIP(IP ip) {
            return devices.get(ip.getAddressString());
        }
    }

    static class UserDevice {
        private boolean lanAccess;
        private IP ip = new IP("192.168.0.254");
        public UserDevice(boolean lan, NetworkManager nm, SHManager shm) { this.lanAccess = lan; }
        public boolean hasLanAccess() { return lanAccess; }
        public void connectToLan() { lanAccess = true; }
        public void disconnectFromLan() { lanAccess = false; }
        public void leaseIP() {}
        public IP getIP() { return ip; }
    }

    static class SHManager {
        private NetworkManager nm;
        public SHManager(NetworkManager nm) { this.nm = nm; }
        public void registerDevice(String id, SHDevice dev) {
            nm.registerDevice(dev);
        }
    }

    // ==================================================================================
    // PAKIET: DEVICES (Logika biznesowa urządzeń)
    // ==================================================================================

    static abstract class SHDevice {
        protected IP ip;
        protected List<String> codes = new ArrayList<>();

        public void setIP(IP ip) { this.ip = ip; }
        public IP getIP() { return ip; }

        protected void registerCode(String code) { codes.add(code); }
        public String getAdvertisedCodes() { return codes.toString(); }

        // Metoda do obsługi żądań sieciowych (parsowanie parametrów)
        public abstract String handleRequest(String code, String[] params);

        // Pomocnicza metoda do parsowania double/int z params
        protected double parseDouble(String[] params, int idx, double def) {
            if (params == null || params.length <= idx) return def;
            try { return Double.parseDouble(params[idx]); } catch (Exception e) { return def; }
        }
        protected int parseInt(String[] params, int idx, int def) {
            if (params == null || params.length <= idx) return def;
            try { return Integer.parseInt(params[idx]); } catch (Exception e) { return def; }
        }
        protected boolean parseBool(String[] params, int idx, boolean def) {
            if (params == null || params.length <= idx) return def;
            return Boolean.parseBoolean(params[idx]);
        }
    }

    // --- STARE URZĄDZENIA ---

    static class Light extends SHDevice {
        private boolean state = false;
        private double brightness = 1.0;
        private String color = "#FFFFFF";

        public Light() {
            registerCode("GET_STATE"); registerCode("SET_STATE");
            registerCode("GET_BRIGHTNESS"); registerCode("SET_BRIGHTNESS");
            registerCode("GET_COLOR"); registerCode("SET_COLOR");
        }

        // Metody symulacyjne
        public void setState(boolean s) { state = s; }
        public boolean isState() { return state; }
        public void setBrightness(double b) { brightness = b; }
        public double getBrightness() { return brightness; }
        public void setColor(String c) { color = c; }
        public String getColor() { return color; }

        @Override
        public String handleRequest(String code, String[] params) {
            switch (code) {
                case "GET_STATE": return state ? "ON" : "OFF";
                case "SET_STATE": state = parseBool(params, 0, false); return "OK";
                case "GET_BRIGHTNESS": return String.valueOf(brightness);
                case "SET_BRIGHTNESS": brightness = parseDouble(params, 0, 1.0); return "OK";
                case "GET_COLOR": return color;
                case "SET_COLOR": if(params.length>0) color = params[0]; return "OK";
                default: return "UNKNOWN";
            }
        }
    }

    static class Thermometer extends SHDevice {
        private double temperature = 21.5;

        public Thermometer() { registerCode("GET_TEMPERATURE"); }
        public void _changeTemp(double t) { temperature = t; } // Symulacja
        public double getTemperature() { return temperature; }

        @Override
        public String handleRequest(String code, String[] params) {
            if (code.equals("GET_TEMPERATURE")) return String.format("%.1f", temperature).replace(',', '.');
            return "UNKNOWN";
        }
    }

    static class Blind extends SHDevice {
        private boolean open = false;

        public Blind() {
            registerCode("GET_STATUS"); registerCode("SET_STATUS");
        }
        public void setStatus(boolean o) { open = o; }
        public boolean isOpen() { return open; }

        @Override
        public String handleRequest(String code, String[] params) {
            if (code.equals("GET_STATUS")) return String.valueOf(open);
            if (code.equals("SET_STATUS")) { open = parseBool(params, 0, false); return "OK"; }
            return "UNKNOWN";
        }
    }

    static class Heater extends SHDevice {
        private double power = 0.0;
        public Heater() { registerCode("GET_POWER"); registerCode("SET_POWER"); }
        public void setPower(double p) { power = Math.max(0, Math.min(100, p)); }
        public double getPower() { return power; }

        @Override
        public String handleRequest(String code, String[] params) {
            if(code.equals("GET_POWER")) return String.valueOf(power);
            if(code.equals("SET_POWER")) { setPower(parseDouble(params, 0, 0)); return "OK"; }
            return "UNKNOWN";
        }
    }

    static class AirConditioner extends SHDevice {
        private boolean on = false;
        private double power = 0;
        public AirConditioner() { registerCode("GET_STATE"); registerCode("GET_POWER"); registerCode("SET_COOLING"); }
        public void _setCooling(double p) { power = p; on = (p > 0); }
        public double getCoolingPower() { return power; }
        public boolean isOn() { return on; }

        @Override
        public String handleRequest(String code, String[] params) {
            if(code.equals("GET_STATE")) return on ? "ON" : "OFF";
            if(code.equals("GET_POWER")) return String.valueOf(power);
            if(code.equals("SET_COOLING")) { _setCooling(parseDouble(params, 0, 0)); return "OK"; }
            return "UNKNOWN";
        }
    }

    static class AudioDevice extends SHDevice {
        private double volume = 0;
        public AudioDevice() { registerCode("GET_VOLUME"); registerCode("SET_VOLUME"); registerCode("MUTE"); }
        public void _setVolume(double v) { volume = Math.max(0, Math.min(100, v)); }
        public double getVolume() { return volume; }

        @Override
        public String handleRequest(String code, String[] params) {
            if(code.equals("GET_VOLUME")) return String.valueOf(volume);
            if(code.equals("SET_VOLUME")) { _setVolume(parseDouble(params, 0, 0)); return "OK"; }
            if(code.equals("MUTE")) { _setVolume(0); return "MUTED"; }
            return "UNKNOWN";
        }
    }

    // --- NOWE URZĄDZENIA ---

    static class CCTV extends SHDevice {
        private boolean on = false;
        private boolean movement = false;

        public CCTV() {
            registerCode("GET_STATUS"); registerCode("SET_STATUS");
            registerCode("GET_MOVEMENT"); registerCode("SET_MOVEMENT"); // SetMovement do testów
        }

        public void setStatus(boolean s) { this.on = s; }
        public boolean isOn() { return on; }
        public void setMovement(boolean m) { this.movement = m; }
        public boolean isMovementDetected() { return movement; }

        @Override
        public String handleRequest(String code, String[] params) {
            if (code.equals("GET_STATUS")) return String.valueOf(on);
            if (code.equals("GET_MOVEMENT")) return String.valueOf(movement);
            if (code.equals("SET_STATUS")) { setStatus(parseBool(params, 0, false)); return "OK"; }
            if (code.equals("SET_MOVEMENT")) { setMovement(parseBool(params, 0, false)); return "OK"; }
            return "UNKNOWN";
        }
    }

    static class TV extends SHDevice {
        private boolean on = false;
        private int volume = 20;
        private int channel = 1;

        public TV() {
            registerCode("GET_STATUS"); registerCode("SET_STATUS");
            registerCode("GET_VOLUME"); registerCode("SET_VOLUME");
            registerCode("GET_CHANNEL"); registerCode("SET_CHANNEL");
        }

        public void setStatus(boolean s) { on = s; }
        public boolean isOn() { return on; }
        public void setVolume(int v) { volume = Math.max(0, Math.min(100, v)); }
        public int getVolume() { return volume; }
        public void setChannel(int c) { if(c>0) channel = c; }
        public int getChannel() { return channel; }

        @Override
        public String handleRequest(String code, String[] params) {
            if (code.equals("GET_STATUS")) return String.valueOf(on);
            if (code.equals("SET_STATUS")) { setStatus(parseBool(params, 0, false)); return "OK"; }
            if (code.equals("GET_VOLUME")) return String.valueOf(volume);
            if (code.equals("SET_VOLUME")) { setVolume(parseInt(params, 0, volume)); return "OK"; }
            if (code.equals("GET_CHANNEL")) return String.valueOf(channel);
            if (code.equals("SET_CHANNEL")) { setChannel(parseInt(params, 0, channel)); return "OK"; }
            return "UNKNOWN";
        }
    }

    static class LightDetector extends SHDevice {
        public enum LightState { DAY, NIGHT }
        private LightState state = LightState.DAY;

        public LightDetector() {
            registerCode("GET_STATE");
            registerCode("SET_STATE"); // Do symulacji
        }

        public void setState(LightState s) { this.state = s; }
        public LightState getState() { return state; }

        @Override
        public String handleRequest(String code, String[] params) {
            if (code.equals("GET_STATE")) return state.toString();
            if (code.equals("SET_STATE")) {
                try {
                    state = LightState.valueOf(params[0].toUpperCase());
                    return "OK";
                } catch(Exception e) { return "ERROR"; }
            }
            return "UNKNOWN";
        }
    }

    static class GarageDoor extends SHDevice {
        public enum DoorState { OPEN, CLOSED, PARTIALLY_OPENED }
        private int position = 100; // 100 = Closed
        private DoorState state = DoorState.CLOSED;

        public GarageDoor() {
            registerCode("GET_POSITION"); registerCode("SET_POSITION");
            registerCode("GET_STATE"); registerCode("SET_STATE");
        }

        public void setPosition(int p) {
            this.position = Math.max(0, Math.min(100, p));
            updateState();
        }
        public int getPosition() { return position; }

        public void setState(DoorState s) {
            this.state = s;
            if (s == DoorState.OPEN) position = 0;
            else if (s == DoorState.CLOSED) position = 100;
            else position = 50;
        }
        public DoorState getState() { return state; }

        private void updateState() {
            if (position == 0) state = DoorState.OPEN;
            else if (position == 100) state = DoorState.CLOSED;
            else state = DoorState.PARTIALLY_OPENED;
        }

        @Override
        public String handleRequest(String code, String[] params) {
            if (code.equals("GET_POSITION")) return String.valueOf(position);
            if (code.equals("SET_POSITION")) { setPosition(parseInt(params, 0, position)); return "OK"; }
            if (code.equals("GET_STATE")) return state.toString();
            if (code.equals("SET_STATE")) {
                try { setState(DoorState.valueOf(params[0].toUpperCase())); return "OK"; }
                catch(Exception e) { return "ERROR"; }
            }
            return "UNKNOWN";
        }
    }

    static class CoffeeMachine extends SHDevice {
        private String status = "IDLE";
        private int progress = 0;
        private int timer = 0;

        public CoffeeMachine() {
            registerCode("GET_STATUS"); registerCode("GET_PROGRESS");
            registerCode("MAKE_COFFEE"); registerCode("SET_TIMER");
            registerCode("TAKE_COFFEE");
        }

        public void _startProcess() {
            if (!status.equals("IDLE") && !status.equals("READY")) return;
            new Thread(() -> {
                try {
                    status = "GRINDING"; progress = 10; Thread.sleep(800);
                    status = "BREWING";
                    for(int i=20; i<=100; i+=10) {
                        progress = i; Thread.sleep(300);
                    }
                    status = "READY";
                } catch (Exception e) {}
            }).start();
        }

        public void _takeCoffee() {
            if(status.equals("READY")) { status = "IDLE"; progress = 0; }
        }

        public void setTimer(int ticks) { this.timer = ticks; }

        // Gettery dla GUI symulacji
        public String getStatus() { return status; }
        public int getProgress() { return progress; }
        public int getTimer() { return timer; }

        @Override
        public String handleRequest(String code, String[] params) {
            if (code.equals("GET_STATUS")) return status;
            if (code.equals("GET_PROGRESS")) return String.valueOf(progress);
            if (code.equals("MAKE_COFFEE")) { _startProcess(); return "STARTED"; }
            if (code.equals("TAKE_COFFEE")) { _takeCoffee(); return "YUMMY"; }
            if (code.equals("SET_TIMER")) { setTimer(parseInt(params, 0, 0)); return "TIMER_SET"; }
            return "UNKNOWN";
        }
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
            JTextField idField = new JTextField("dev_01", 8);
            JButton addButton = new JButton("Dodaj");

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

            // Timer do odświeżania UI (podgląd stanu na żywo)
            new javax.swing.Timer(500, e -> {
                String selected = deviceList.getSelectedValue();
                if(selected != null) showDeviceProperties(deviceMap.get(selected));
            }).start();
        }

        private void createAndRegisterDevice(String type, String id) {
            if (deviceMap.containsKey(id)) return;

            SHDevice newDevice = null;
            switch (type) {
                case "Light": newDevice = new Light(); break;
                case "Thermometer": newDevice = new Thermometer(); break;
                case "Blind": newDevice = new Blind(); break;
                case "Heater": newDevice = new Heater(); break;
                case "AirConditioner": newDevice = new AirConditioner(); break;
                case "AudioDevice": newDevice = new AudioDevice(); break;
                case "CCTV": newDevice = new CCTV(); break;
                case "TV": newDevice = new TV(); break;
                case "LightDetector": newDevice = new LightDetector(); break;
                case "GarageDoor": newDevice = new GarageDoor(); break;
                case "CoffeeMachine": newDevice = new CoffeeMachine(); break;
            }

            if (newDevice != null) {
                shManager.registerDevice(id, newDevice);
                deviceMap.put(id, newDevice);
                deviceListModel.addElement(id);
            }
        }

        private void showDeviceProperties(SHDevice device) {
            propertiesPanel.removeAll();
            propertiesPanel.setBorder(new TitledBorder(device.getClass().getSimpleName()));

            // --- RENDEROWANIE ZALEŻNE OD TYPU ---

            if (device instanceof Light) {
                Light l = (Light) device;
                JCheckBox on = new JCheckBox("Włączony", l.isState());
                on.addActionListener(e -> l.setState(on.isSelected()));
                propertiesPanel.add(on);
                propertiesPanel.add(new JLabel("Jasność: " + l.getBrightness()));
                propertiesPanel.add(new JLabel("Kolor: " + l.getColor()));
            }
            else if (device instanceof Thermometer) {
                Thermometer t = (Thermometer) device;
                JLabel lbl = new JLabel("Aktualna Temp: " + String.format("%.1f", t.getTemperature()));
                JSlider slider = new JSlider(-10, 40, (int)t.getTemperature());
                slider.setMajorTickSpacing(10);
                slider.setPaintTicks(true);
                slider.addChangeListener(e -> t._changeTemp(slider.getValue()));
                propertiesPanel.add(new JLabel("Symulacja otoczenia:"));
                propertiesPanel.add(slider);
                propertiesPanel.add(lbl);
            }
            else if (device instanceof CCTV) {
                CCTV c = (CCTV) device;
                JCheckBox on = new JCheckBox("Zasilanie", c.isOn());
                on.addActionListener(e -> c.setStatus(on.isSelected()));
                JCheckBox move = new JCheckBox("Symuluj RUCH", c.isMovementDetected());
                move.addActionListener(e -> c.setMovement(move.isSelected()));

                JPanel led = new JPanel();
                led.setBackground(c.isOn() && c.isMovementDetected() ? Color.RED : Color.GRAY);
                led.setPreferredSize(new Dimension(20,20));
                led.setBorder(BorderFactory.createLineBorder(Color.BLACK));

                propertiesPanel.add(on);
                propertiesPanel.add(move);
                propertiesPanel.add(new JLabel("LED Detekcji:"));
                propertiesPanel.add(led);
            }
            else if (device instanceof TV) {
                TV tv = (TV) device;
                JCheckBox on = new JCheckBox("Power", tv.isOn());
                on.addActionListener(e -> tv.setStatus(on.isSelected()));
                JSlider vol = new JSlider(0, 100, tv.getVolume());
                vol.addChangeListener(e -> tv.setVolume(vol.getValue()));

                propertiesPanel.add(on);
                propertiesPanel.add(new JLabel("Głośność: " + tv.getVolume()));
                propertiesPanel.add(vol);
                propertiesPanel.add(new JLabel("Kanał: " + tv.getChannel()));
            }
            else if (device instanceof GarageDoor) {
                GarageDoor gd = (GarageDoor) device;
                JProgressBar vis = new JProgressBar(0, 100);
                vis.setValue(100 - gd.getPosition()); // 100=Closed(full), 0=Open
                vis.setString("Stan: " + gd.getState());
                vis.setStringPainted(true);

                JSlider pos = new JSlider(0, 100, gd.getPosition());
                pos.setInverted(true); // 100 (zamknięte) na dole
                pos.addChangeListener(e -> gd.setPosition(pos.getValue()));

                JButton open = new JButton("Otwórz"); open.addActionListener(e -> gd.setState(GarageDoor.DoorState.OPEN));
                JButton close = new JButton("Zamknij"); close.addActionListener(e -> gd.setState(GarageDoor.DoorState.CLOSED));

                propertiesPanel.add(new JLabel("Wizualizacja:"));
                propertiesPanel.add(vis);
                propertiesPanel.add(new JLabel("Sterowanie ręczne:"));
                propertiesPanel.add(pos);
                JPanel btns = new JPanel(); btns.add(open); btns.add(close);
                propertiesPanel.add(btns);
            }
            else if (device instanceof CoffeeMachine) {
                CoffeeMachine cm = (CoffeeMachine) device;
                JProgressBar p = new JProgressBar(0, 100);
                p.setValue(cm.getProgress());
                p.setStringPainted(true);
                JLabel st = new JLabel("Status: " + cm.getStatus());

                JButton brew = new JButton("Zrób Kawę");
                brew.addActionListener(e -> cm._startProcess());
                JButton take = new JButton("Odbierz");
                take.addActionListener(e -> cm._takeCoffee());
                take.setEnabled(cm.getStatus().equals("READY"));

                propertiesPanel.add(st);
                propertiesPanel.add(p);
                propertiesPanel.add(brew);
                propertiesPanel.add(take);
            }
            else if (device instanceof LightDetector) {
                LightDetector ld = (LightDetector) device;
                JComboBox<LightDetector.LightState> cb = new JComboBox<>(LightDetector.LightState.values());
                cb.setSelectedItem(ld.getState());
                cb.addActionListener(e -> ld.setState((LightDetector.LightState)cb.getSelectedItem()));

                JLabel icon = new JLabel(ld.getState() == LightDetector.LightState.DAY ? "☀️" : "🌙");
                icon.setFont(new Font("SansSerif", Font.PLAIN, 40));
                icon.setAlignmentX(Component.CENTER_ALIGNMENT);

                propertiesPanel.add(new JLabel("Symulacja Pory Dnia:"));
                propertiesPanel.add(cb);
                propertiesPanel.add(icon);
            }
            else {
                // Default fallback for simple devices
                propertiesPanel.add(new JLabel("Generic Device: " + device.getAdvertisedCodes()));
            }

            propertiesPanel.revalidate();
            propertiesPanel.repaint();
        }
    }

    // ==================================================================================
    // GUI 2: PANEL UŻYTKOWNIKA (Klient)
    // ==================================================================================

    static class UserControlFrame extends JFrame {
        private final NetworkManager networkManager;
        private final UserDevice userDevice;
        private final DefaultListModel<String> discoveredIPsModel;
        private final JPanel controlsPanel;
        private final JTextArea logArea;

        public UserControlFrame(UserDevice dev, NetworkManager nm) {
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
                        Request r = networkManager.createRequest(userDevice.getIP(), new IP(targetIP), "ADVERT", new String[]{});
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
                Request r = networkManager.createRequest(userDevice.getIP(), target, "ADVERT", new String[]{});
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

        private void buildControls(String rawCodes, IP target) {
            String clean = rawCodes.replace("[", "").replace("]", "");
            String[] codes = clean.split(",");

            controlsPanel.add(new JLabel("<html><b>Urządzenie: " + target.getAddressString() + "</b></html>"));
            controlsPanel.add(Box.createVerticalStrut(10));

            for (String c : codes) {
                String code = c.trim();
                if (code.isEmpty()) continue;

                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
                row.setBorder(BorderFactory.createMatteBorder(0,0,1,0, Color.LIGHT_GRAY));
                JLabel lbl = new JLabel(code);
                lbl.setPreferredSize(new Dimension(100, 20));
                lbl.setFont(new Font("SansSerif", Font.PLAIN, 10));
                row.add(lbl);

                if (code.startsWith("GET")) {
                    JButton getBtn = new JButton("Get");
                    JLabel valLbl = new JLabel("?");
                    getBtn.addActionListener(e -> sendRequest(target, code, new String[]{}, valLbl));
                    row.add(getBtn);
                    row.add(valLbl);
                } else {
                    JTextField paramField = new JTextField(5);
                    JButton setBtn = new JButton("Set");
                    setBtn.addActionListener(e -> {
                        String txt = paramField.getText();
                        String[] params = txt.isEmpty() ? new String[]{} : txt.split(",");
                        sendRequest(target, code, params, null);
                    });
                    row.add(paramField);
                    row.add(setBtn);
                }
                controlsPanel.add(row);
            }
        }

        private void sendRequest(IP target, String code, String[] params, JLabel outLabel) {
            new Thread(() -> {
                log("-> " + code + " to " + target.getAddressString());
                Request r = networkManager.createRequest(userDevice.getIP(), target, code, params);
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
}