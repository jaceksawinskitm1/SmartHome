package Devices;

import Network.IP;
import Network.NetworkDevice;
import Network.NetworkManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class SHManager extends NetworkDevice {

  public static class DeviceLogic {
    private final Comparator comparator;
    private IP devA;
    private IP devB;
    private String successCode;
    private String[] successParams;
    private final SHManager manager;
    private int priority;

    public DeviceLogic(IP device, String code, String[] params, SHManager manager, Comparator comparator,
        int priority) {
      this.comparator = comparator;
      this.devB = device;
      this.devA = comparator.devA;
      this.successCode = code;
      this.successParams = params;
      this.priority = priority;

      this.manager = manager;
    }

    private String getSuccessRequest() {
      String successRequest = this.successCode.toUpperCase() + " " + manager.getIP().getAddressString() + " "
          + devB.getAddressString() + " [";
      if (successParams.length == 0)
        successRequest += " ";
      for (String p : successParams)
        successRequest += p + ",";
      successRequest = successRequest.substring(0, successRequest.length() - 1) + "]";
      return successRequest;
    }

    public void Try() {
      System.out.println("Testing logic: " + getSuccessRequest());
      if (!comparator.test()) {
        return;
      }

      manager.networkManager.createRequest(getSuccessRequest()).send();
    }
  }

  public static class Comparator {
    public enum Condition {
      EQUAL, GREATER_THAN, LESS_THAN, GREATER_EQUAL, LESS_EQUAL, NOT_EQUAL
    };

    private final IP devA;
    private final String codeA;

    private final Condition condition;

    private final String paramB;

    private final SHManager manager;

    public Comparator(IP deviceA, String codeA, Condition condition, String constB, SHManager manager) {
      this.devA = deviceA;
      this.codeA = codeA;
      // this.reqA =
      this.paramB = constB;

      this.condition = condition;
      this.manager = manager;
    }

    private String getTestRequest() {
      return codeA.toUpperCase() + " " + manager.getIP().getAddressString() + " " + devA.getAddressString() + " []";
    }

    public boolean test() {
      String valueA;

      NetworkManager.Request req = this.manager.networkManager.createRequest(getTestRequest());
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

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Comparator))
        return false;
      Comparator other = (Comparator) obj;

      if (!this.devA.equals(other.devA))
        return false;

      if (!this.codeA.equals(other.codeA))
        return false;

      if (!this.condition.name().equals(other.condition.name()))
        return false;

      if (!this.paramB.equals(other.paramB))
        return false;

      return true;
    }
  }

  // SHManager

  private final NetworkManager networkManager;

  private final HashMap<String, IP> devices = new HashMap<>();

  private final ArrayList<DeviceLogic> logics = new ArrayList<>();

  public NetworkManager getNetworkManager() {
    return networkManager;
  }

  public void renameDevice(String oldID, String newID) {
    if (!devices.containsKey(oldID))
      return;

    IP device = devices.get(oldID);
    devices.remove(oldID);
    devices.put(newID, device);
  }

  public void registerDevice(String id, IP dev) {
    if (devices.containsValue(dev))
      return;

    //dev.leaseIP(this.getNetworkManager());
    networkManager.createRequest(this.getIP(), dev, "_CONNECT_MANAGER", new String[] {}).send();
    devices.put(id, dev);
  }

  public void deregisterDevice(String id) {
    devices.remove(id);
  }

  public String getDeviceId(IP ip) {
    for (String devId : devices.keySet()) {
      if (devices.get(devId).equals(ip))
        return devId;
    }
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

  public void registerLogic(IP device, String code, String[] params, Comparator comparator, String priority) {
    DeviceLogic logic = new DeviceLogic(device, code, params, this, comparator, Integer.valueOf(priority));

    logics.add(logic);
  }

  public void deregisterLogic(IP device, String code, String[] params, Comparator comparator) {
    for (DeviceLogic logic : logics) {
      boolean found = true;
      if (!device.equals(logic.devB))
        found = false;

      if (!code.equals(logic.successCode))
        found = false;

      if (!Arrays.equals(params, logic.successParams))
        found = false;

      if (!comparator.equals(logic.comparator))
        found = false;

      if (found) {
        logics.remove(logic);
        return;
      }
    }
  }

  public void deregisterLogic(DeviceLogic logic) {
    logics.remove(logic);
  }

  private String[] parseStringArray(String str) {
    if (str.length() <= 2) {
      return new String[] {};
    }
    if (!str.startsWith("[") || !str.endsWith("]")) {
      // Not a list
      return new String[] { str };
    }
    String s = str.substring(1, str.length() - 1);
    return s.split(",");
  }

  private String createStringArray(Object[] objs) {
    if (objs.length == 0) {
      return "[]";
    }

    String res = "[";
    for (Object obj : objs) {
      res += obj.toString() + ",";
    }
    res = res.substring(0, res.length() - 1) + "]";
    return res;
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
      for (IP dev : devices.values()) {
        res += dev.getAddressString() + ",";
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
      // cond_type, cond_val, priority
      System.out.println("Adding logic: params: " + Arrays.toString(params));

      // Remove old duplicates (if exist)
      deregisterLogic(
          new IP(params[0]),
          params[1],
          parseStringArray(params[2]),
          createComparatorStringBased(params[3], params[4], params[5], params[6]));

      registerLogic(
          new IP(params[0]),
          params[1],
          parseStringArray(params[2]),
          // Conditions
          // TODO: Add multiple
          createComparatorStringBased(params[3], params[4], params[5], params[6]),
          params[7] 
      );

      return "";
    });

    registerNetworkCode("DEL_LOGIC", "NULL", (IP[] ips, String[] params) -> {
      // params: action_dev_ip, action_code, action_params, cond_dev_ip, cond_code,
      // cond_type, cond_val
      System.out.println("Deleting logic: " + Arrays.toString(params));

      deregisterLogic(
          new IP(params[0]),
          params[1],
          parseStringArray(params[2]),
          createComparatorStringBased(params[3], params[4], params[5], params[6]));

      return "";
    });

    registerNetworkCode("GET_LOGICS", "LOGICS", (IP[] ips, String[] params) -> {
      if (logics.isEmpty()) {
        return "[]";
      }

      // return: action_dev_ip, action_code, action_params, cond_dev_ip, cond_code,
      // cond_type, cond_val

      String res = "[";
      for (DeviceLogic logic : logics) {
        res += logic.devB.getAddressString() + ",";
        res += logic.successCode + ",";
        // Use only one paramater
        if (logic.successParams.length > 0)
          res += logic.successParams[0] + ",";
        else
          res += "!!!MISSING!!!,";
        res += logic.devA.getAddressString() + ",";
        res += logic.comparator.codeA + ",";
        res += logic.comparator.condition.name() + ",";
        res += logic.comparator.paramB + ",";
        res += logic.priority + ",";
      }
      res = res.substring(0, res.length() - 1) + "]";

      return res;
    });


    registerNetworkCode("ADD_DEVICE", "NULL", (IP[] ips, String[] params) -> {
      // params: device_ip, device_id
      registerDevice(params[1], new IP(params[0]));
    });

    registerNetworkCode("RENAME_DEVICE", "NULL", (IP[] ips, String[] params) -> {
      // params: device_id_old, device_id_new
      renameDevice(params[0], params[1]);
    });
  }

  private void loop() {
    // Runs every second
    
    // Try every logic connection
    logics.sort((a, b) -> {
      return Integer.compare(a.priority, b.priority);
    });


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
