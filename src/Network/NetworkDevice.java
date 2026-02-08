package Network;

import java.util.Arrays;
import java.util.HashMap;
import java.util.function.*;

public class NetworkDevice {
  private IP ip;

  private NetworkManager networkManager;

  public IP getIP() {
    return ip;
  }

  public void setIP(IP ip) {
    this.ip = ip;
  }

  public NetworkManager getNetworkManager() {
    return networkManager;
  }

  public void setNetworkManager(NetworkManager networkManager) {
    this.networkManager = networkManager;
  }

  public void leaseIP(NetworkManager networkManager) {
    this.networkManager = networkManager;
    this.setIP(this.networkManager.leaseIP(this));
  }

  public void leaseIP() {
    leaseIP(this.networkManager);
  }

  public void freeIP(NetworkManager networkManager) {
    this.networkManager = networkManager;
    this.networkManager.freeIP(this.getIP());
    this.setIP(null);
  }

  public void freeIP() {
    freeIP(this.networkManager);
  }

  private final HashMap<String, BiFunction<IP[], String[], String>> networkCodes = new HashMap<>();

  protected void registerNetworkCode(String code, String type, BiFunction<IP[], String[], String> exec) {
    networkCodes.put(code + "<" + type + ">", exec);
  }

  protected void registerNetworkCode(String code, String type, Function<String[], String> exec) {
    networkCodes.put(code + "<" + type + ">", (IP[] _, String[] params) -> exec.apply(params));
  }

  protected void registerNetworkCode(String code, String type, Supplier<String> exec) {
    networkCodes.put(code + "<" + type + ">", (IP[] _, String[] _) -> exec.get());
  }

  protected void registerNetworkCode(String code, String type, BiConsumer<IP[], String[]> exec) {
    networkCodes.put(code + "<" + type + ">", (IP[] ips, String[] params) -> {
      exec.accept(ips, params);
      return null;
    });
  }

  protected void registerNetworkCode(String code, String type, Consumer<String[]> exec) {
    networkCodes.put(code + "<" + type + ">", (IP[] _, String[] params) -> {
      exec.accept(params);
      return null;
    });
  }

  protected void registerNetworkCode(String code, String type, Runnable exec) {
    networkCodes.put(code + "<" + type + ">", (IP[] _, String[] _) -> {
      exec.run();
      return null;
    });
  }

  public String parseNetworkRequest(String code, IP source, IP target, String[] params) {
    // ADVERT check
    if (code.equals("ADVERT")) {
      if (networkCodes.size() == 0) {
        return "";
      }

      String res = "[";
      for (String netcode : networkCodes.keySet()) {
        // _SOMETHING is a private netcode
        if (!netcode.startsWith("_"))
          res += netcode + ",";
      }
      res = res.substring(0, res.length() - 1) + "]";

      return res;
    }

    for (String nc : networkCodes.keySet()) {
      if (nc.replaceAll("<.*>$", "").equals(code)) {
        return networkCodes.get(nc).apply(new IP[] { source, target }, params);
      }
    }

    if (target.getAddress()[3] != (byte) 255)
      throw new RuntimeException("Unknown network code " + code);

    return null;
  }

  public NetworkManager.Request createRequest(IP target, String code, String[] params) {
    return this.networkManager.createRequest(ip, target, code, params);
  }
}
