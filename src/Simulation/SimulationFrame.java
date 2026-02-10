package Simulation;

import java.awt.*;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.border.*;
import javax.swing.*;

import Devices.*;
import Network.NetworkManager;
import Network.UserDevice;

public class SimulationFrame extends JFrame {
  private final SHManager shManager;
  private final NetworkManager networkManager;
  private final DefaultListModel<String> deviceListModel;
  private final Map<String, SHDevice> deviceMap;
  private JPanel propertiesPanel;
  private UserDevice userDevice;

  public SimulationFrame(SHManager shManager, NetworkManager nm, UserDevice userDevice) {
    this.shManager = shManager;
    this.networkManager = nm;
    this.deviceMap = new HashMap<>();
    this.deviceListModel = new DefaultListModel<>();
    this.userDevice = userDevice;

    setTitle("Symulacja (Admin Environment)");
    setSize(500, 700);
    setLocation(50, 50);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLayout(new BorderLayout());

    JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JCheckBox networkAccess = new JCheckBox("Dostęp do sieci");
    networkAccess.setSelected(true);
    networkAccess.addActionListener(e -> {
      if (((JCheckBox)e.getSource()).isSelected())
        userDevice.connectToLan();
      else
        userDevice.disconnectFromLan();
    });
    topPanel.add(networkAccess);
    add(topPanel, BorderLayout.NORTH);

    // LISTA
    JSplitPane splitPane = new JSplitPane();
    JList<String> deviceList = new JList<>(deviceListModel);
    deviceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    deviceList.addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        String key = deviceList.getSelectedValue();
        if (key != null)
          showDeviceProperties(deviceMap.get(key));
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
    JComboBox<String> typeCombo = new JComboBox<>(new String[] {
        "Light", "Thermometer", "Blind", "Heater", "AirConditioner", "AudioDevice",
        "CCTV", "TV", "LightDetector", "GarageDoor", "CoffeeMachine", "Clock", "Alarm"
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
      if (selected != null)
        showDeviceProperties(deviceMap.get(selected));
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
      case "Clock" -> new Clock();
      case "Alarm" -> new Alarm();
      default -> null;
    };

    registerDevice(newDevice, autoConnect);
  }

  public void registerDevice(SHDevice device, boolean autoConnect) {
    if (device != null) {
      // shManager.registerDevice(id, newDevice);
      device.setAutoConnect(autoConnect);
      device.setNetworkManager(networkManager);
      deviceMap.put(device.getIP().getAddressString(), device);
      deviceListModel.addElement(device.getIP().getAddressString());
    }
  }


  public void deleteDevice(SHDevice device) {
    if (device != null) {
      shManager.deregisterDevice(device.getIP());
      deviceMap.remove(device.getIP().getAddressString(), device);
      deviceListModel.removeElement(device.getIP().getAddressString());
    }
  }

  SHDevice current = null;

  private void showDeviceProperties(SHDevice device) {
    if (device.equals(current))
      return;

    current = device;

    propertiesPanel.removeAll();
    JButton deleteButton = new JButton("Usuń urządzenie");
    deleteButton.addActionListener(e -> {
      deleteDevice(current);
    });

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
        JLabel lbl = new JLabel("Aktualna Temp: " + t.getTemperature());
        JSlider slider = new JSlider(-10, 40, (int) t.getTemperature());
        slider.setMajorTickSpacing(10);
        slider.setPaintTicks(true);
        slider.addChangeListener(e -> t._changeTemp(slider.getValue()));
        propertiesPanel.add(new JLabel("Symulacja otoczenia:"));
        propertiesPanel.add(slider);
        propertiesPanel.add(lbl);
      }
      case CCTV c -> {
        JCheckBox move = new JCheckBox("Symuluj RUCH", c.isMovementDetected());
        move.addActionListener(e -> c.setMovement(move.isSelected()));
        propertiesPanel.add(move);
      }
      case TV tv -> {
        JCheckBox on = new JCheckBox("Power", tv.isOn());
        on.addActionListener(e -> tv.setStatus(on.isSelected()));
        JSlider vol = new JSlider(0, 100, (int) (tv.getVolume() * 100));
        vol.addChangeListener(e -> tv.setVolume(vol.getValue()));

        propertiesPanel.add(on);
        propertiesPanel.add(new JLabel("Głośność: " + tv.getVolume()));
        propertiesPanel.add(vol);
        propertiesPanel.add(new JLabel("Kanał: " + tv.getChannel()));
      }
      case GarageDoor gd -> {
        JProgressBar vis = new JProgressBar(0, 100);
        vis.setValue(100 - (int) (gd.getPosition() * 100)); // 100=Closed(full), 0=Open

        vis.setString("Stan: " + gd.getState());
        vis.setStringPainted(true);

        JSlider pos = new JSlider(0, 100, (int) (gd.getPosition() * 100));
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
        p.setValue((int) (cm.getProgress() * 100));
        p.setStringPainted(true);
        JLabel st = new JLabel("Status: " + cm.getStatus());

        JButton takeButton = new JButton("Take coffee");
        takeButton.addActionListener(e -> {
          cm.takeCoffee();
        });

        propertiesPanel.add(st);
        propertiesPanel.add(p);
        propertiesPanel.add(takeButton);
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
      case Clock c -> {
        UI.TimeInput input = new UI.TimeInput();
        input.setTime(c.getTime());
        input.setMaximumSize(input.getPreferredSize());
        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> {
          c._setTime(input.getTime());
        });
        propertiesPanel.add(input);
        propertiesPanel.add(applyButton);
      }
      case Alarm a -> {
        JPanel durationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel durationLabel = new JLabel("Duration: ");
        JFormattedTextField duration = new JFormattedTextField(NumberFormat.getIntegerInstance());
        duration.setValue(a.getDuration());
        duration.setColumns(3);
        durationPanel.add(durationLabel);
        durationPanel.add(duration);
        duration.setMaximumSize(duration.getPreferredSize());
        durationPanel.setMaximumSize(durationPanel.getPreferredSize());

        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> {
          a.setDuration(Integer.parseInt(String.valueOf(duration.getValue())));
        });

        JButton ringButton = new JButton("Ring");
        ringButton.addActionListener(e -> {
          a.ring();
        });
        propertiesPanel.add(durationPanel);
        propertiesPanel.add(ringButton);
        propertiesPanel.add(applyButton);
      }
      default -> {
        // Default fallback for simple devices
        propertiesPanel.add(new JLabel("Generic Device: "
            + networkManager.createRequest(shManager.getIP(), device.getIP(), "ADVERT", new String[] {})));
      }
    }


    propertiesPanel.add(deleteButton);
    propertiesPanel.revalidate();
    propertiesPanel.repaint();
  }

  public NetworkManager getNetworkManager() {
    return this.networkManager;
  }

  public SHManager getSHManager() {
    return this.shManager;
  }
}
