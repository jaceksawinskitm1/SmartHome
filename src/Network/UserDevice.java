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
    return lanAccess && this.getIP() != null;
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
      while (queuedRequests.size() > 0) {
        queuedRequests.removeFirst().setSource(this.getIP()).send();
      }
    }

    try {
      TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    loop();
  }

  public String sendRequest(IP target, String code, String[] params) {
    NetworkManager.Request req = this.createRequest(target, code, params);

    if (!hasLanAccess()) {
      // Queue request
      queuedRequests.add(req);
    } else {
      return req.setSource(this.getIP()).send().getResult();
    }
    return "";
  }
}
