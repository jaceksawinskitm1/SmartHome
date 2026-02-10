import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.*;
import Network.*;
import Simulation.SimulationFrame;
import Devices.*;
import UI.*;

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
    } catch (Exception ignored) {
    }

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
}
