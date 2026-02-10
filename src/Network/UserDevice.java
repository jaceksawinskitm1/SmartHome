package Network;

import Devices.SHManager;
import UI.UserUI;

import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;

public class UserDevice extends NetworkDevice {
  private boolean lanAccess = true;
  private final IP shManager;

  private UserUI ui;

  private final ArrayDeque<NetworkManager.Request> queuedRequests = new ArrayDeque<>();

  public boolean hasLanAccess() {
    return lanAccess;
  }

  public void disconnectFromLan() {
    this.lanAccess = false;
    this.freeIP();
  }

  public void connectToLan() {
    this.lanAccess = true;
    this.leaseIP();
  }

  public UserDevice(boolean hasLanAccess, NetworkManager networkManager, SHManager shManager) {
    this.lanAccess = hasLanAccess;
    this.setNetworkManager(networkManager);

    this.shManager = shManager.getIP();

    if (this.lanAccess) {
      connectToLan();
    }

    Thread backgroundLoop = new Thread(this::loop);
    backgroundLoop.start();

    // Network
    registerNetworkCode("_LOGIC_RAN", "NULL", (String[] params) -> {
      ui.highlightLogic(Integer.parseInt(params[0]));
    });
  }

  public void showUI() {

    this.ui = new UserUI(this, this.getNetworkManager());
    this.ui.setVisible(true);
  }

  private void loop() {
    // Runs every second

    if (hasLanAccess()) {
      // Send all queued requests
      while (!queuedRequests.isEmpty()) {
        queuedRequests.pop().send();
      }
    }

    try {
      TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    loop();
  }

  public void sendAction(String devID, String code, String[] params) {
    // Create request
    String[] p = new String[2 + params.length];
    p[0] = devID;
    p[1] = code;
    System.arraycopy(params, 0, p, 2, params.length);
    NetworkManager.Request req = this.createRequest(shManager, "ACTION", p);

    if (!hasLanAccess()) {
      // Queue request
      queuedRequests.add(req);
    } else {
      // Send request
      req.send();
    }
  }
}
