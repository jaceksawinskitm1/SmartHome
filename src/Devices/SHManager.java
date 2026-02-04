package Devices;

import Network.IP;
import Network.NetworkDevice;
import Network.NetworkManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class SHManager extends NetworkDevice {

  public static class DeviceLogic {
    private final Comparator comparator;
    private IP devA;
    private IP devB;
    private String successRequest;
    private final SHManager manager;

    public DeviceLogic(IP device, String code, String[] params, SHManager manager, Comparator comparator) {
      this.comparator = comparator;
      this.devB = device;
      this.devA = comparator.devA;

      this.successRequest = code.toUpperCase() + " " + manager.getIP().getAddressString() + " "
          + device.getAddressString() + " [";
      if (params.length == 0)
        this.successRequest += " ";
      for (String p : params)
        this.successRequest += p + ",";
      this.successRequest = this.successRequest.substring(0, this.successRequest.length() - 1) + "]";

      this.manager = manager;
    }

    public DeviceLogic(IP device, String code, String params, SHManager manager, Comparator comparator) {
      this.comparator = comparator;
      this.devB = device;
      this.devA = comparator.devA;

      this.successRequest = code.toUpperCase() + " " + manager.getIP().getAddressString() + " "
          + device.getAddressString() + " " + params;

      this.manager = manager;
    }

    public void Try() {
      System.out.println("Testing logic: " + successRequest);
      if (!comparator.test()) {
        return;
      }

      manager.networkManager.createRequest(successRequest).send();
    }
  }

  public static class Comparator {
    public enum Condition {
      EQUAL, GREATER_THAN, LESS_THAN, GREATER_EQUAL, LESS_EQUAL, NOT_EQUAL
    };

    private final IP devA;
    private final String reqA;

    private final Condition condition;

    private final String paramB;

    private final SHManager manager;

    public Comparator(IP deviceA, String codeA, Condition condition, String constB, SHManager manager) {
      this.devA = deviceA;
      this.reqA = codeA.toUpperCase() + " " + manager.getIP().getAddressString() + " "
          + deviceA.getAddressString() + " []";
      this.paramB = constB;

      this.condition = condition;
      this.manager = manager;
    }

    public boolean test() {
      String valueA;

      NetworkManager.Request req = this.manager.networkManager.createRequest(reqA);
      req.send();
      valueA = req.getResult();

      switch (this.condition) {
        case EQUAL -> {
          return Objects.equals(valueA, paramB);
        }
        case NOT_EQUAL -> {
          return !Objects.equals(valueA, paramB);
        }
      }

      // Cast to double for further comparisons
      double dA, dB;

      try {
        dA = Double.parseDouble(valueA);
        dB = Double.parseDouble(paramB);
      } catch (NumberFormatException e) {
        throw new RuntimeException("Comparator: invalid data, can't cast " + valueA + " and " + paramB + " to double.");
      }

      switch (this.condition) {
        case GREATER_THAN -> {
          return dA > dB;
        }
        case GREATER_EQUAL -> {
          return dA >= dB;
        }
        case LESS_THAN -> {
          return dA < dB;
        }
        case LESS_EQUAL -> {
          return dA <= dB;
        }
        default -> {
          return false;
        }
      }
    }
  }

  // SHManager

  private final NetworkManager networkManager;

  private final HashMap<String, SHDevice> devices = new HashMap<>();

  private final ArrayList<DeviceLogic> logics = new ArrayList<>();

  public NetworkManager getNetworkManager() {
    return networkManager;
  }

  public void registerDevice(String id, SHDevice dev) {
    if (devices.containsValue(dev))
      return;

    dev.leaseIP(this.getNetworkManager());
    devices.put(id, dev);
  }

  public void deregisterDevice(String id) {
    devices.remove(id);
  }

  public String getDeviceId(IP ip) {
    for (String devId : devices.keySet()) {
      if (devices.get(devId).getIP().equals(ip))
        return devId;
    }
    ;
    return "";
  };

  public Comparator createComparatorStringBased(String devA, String codeA, String condition, String constB) {
    Comparator.Condition cond = Comparator.Condition.valueOf(condition.toUpperCase());
    return new Comparator(new IP(devA), codeA, cond, constB, this);
  }

  // Variable <-> Constant comparison
  public Comparator createComparator(SHDevice deviceA, String codeA, Comparator.Condition condition, String constB) {
    return new Comparator(deviceA.getIP(), codeA, condition, constB, this);
  }


  public void registerLogic(IP device, String code, String params, Comparator comparator) {
    DeviceLogic logic = new DeviceLogic(device, code, params, this, comparator);

    logics.add(logic);
  }

  public void deregisterLogic(DeviceLogic logic) {
    logics.remove(logic);
  }

  public SHManager() {
    this(new NetworkManager());
  }


  public SHManager(NetworkManager networkManager) {
    Thread backgroundLoop = new Thread(this::loop);
    backgroundLoop.start();
    this.networkManager = networkManager;
    this.leaseIP(this.getNetworkManager());

    // Network
    registerNetworkCode("FINDSHMANAGER", "NULL", (IP[] ips, String[] params) -> this.getIP().getAddressString());
    registerNetworkCode("GET_DEVICES", "IPS", (IP[] ips, String[] params) -> {
      if (devices.isEmpty()) {
        return "[]";
      }

      String res = "[";
      for (SHDevice dev : devices.values()) {
        res += dev.getIP().getAddressString() + ",";
      }
      res = res.substring(0, res.length() - 1) + "]";

      return res;
    });

    registerNetworkCode("GET_DEVID", "STRING", (IP[] ips, String[] params) -> {
      IP targ = new IP(params[0]);

      return getDeviceId(targ);
    });

    registerNetworkCode("ADD_LOGIC", "NULL", (IP[] ips, String[] params) -> {
      // params: action_dev_ip, action_code, action_params, cond_dev_ip, cond_code,
      // cond_type, cond_val
      System.out.println("Adding logic: params: " + params[2]);
      registerLogic(
          new IP(params[0]),
          params[1],
          params[2],
          // Conditions
          // TODO: Add multiple
          createComparatorStringBased(params[3], params[4], params[5], params[6]));

      return "";
    });

    registerNetworkCode("GET_LOGICS", "LOGICS", (IP[] ips, String[] params) -> {
      if (logics.isEmpty()) {
        return "[]";
      }

      String res = "[";
      for (DeviceLogic logic : logics) {
        res += logic.devA.getAddressString() + ",";
        res += logic.devB.getAddressString() + ",";
      }
      res = res.substring(0, res.length() - 1) + "]";

      return res;
    });
  }

  private void loop() {
    // Runs every second

    // Depracated, use logics instead
    for (SHDevice dev : devices.values()) {
      dev.refresh();
      dev.events();
    }

    // Try every logic connection
    for (DeviceLogic logic : logics) {
      logic.Try();
    }

    try {
      TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    loop();
  }
}
