package Devices;

import Network.IP;
import Network.NetworkDevice;
import Network.NetworkManager;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public abstract class SHDevice extends NetworkDevice {
  private IP manager;
  private boolean autoConnect = true;

  private final ArrayList<Runnable> events = new ArrayList<>();

  public void setAutoConnect(boolean autoConnect) {
    this.autoConnect = autoConnect;
  }

  @Override
  public void setNetworkManager(NetworkManager networkManager) {
    super.setNetworkManager(networkManager);
    leaseIP();

    // Network:
    registerNetworkCode("_CONNECT_MANAGER", "NULL", (IP[] ips, String[] params) -> {
      setManager(ips[0]);
    });

    registerNetworkCode("_DISCONNECT_MANAGER", "NULL", (IP[] ips, String[] params) -> {
      setManager(null);
    });
    // Wait before advertising to allow for manual connecting
    Thread thread = new Thread(() -> {
      try {
        TimeUnit.SECONDS.sleep(1);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      setManager(new IP(networkManager
          .createRequest(this.getIP(), networkManager.getBroadcastAddress(), "FINDSHMANAGER", new String[] {}).send()
          .getResult()));

      networkManager.createRequest(this.getIP(), manager, "ADD_DEVICE", new String[] {
          this.getIP().getAddressString(), this.getIP().getAddressString()
      }).send();
    });

    if (autoConnect)
      thread.start();
  }

  public void setManager(IP manager) {
    this.manager = manager;
  }

  @Deprecated
  public void addEvent(Runnable event) {
    if (events.contains(event))
      return;

    events.add(event);
  }

  @Deprecated
  public void removeEvent(Runnable event) {
    events.remove(event);
  }

  public void refresh() {
  }

  public void events() {
    for (Runnable event : events) {
      event.run();
    }
  }
}
