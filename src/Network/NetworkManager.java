package Network;

import Devices.SHDevice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetworkManager {
  public class Request {
    private final IP source;
    private final IP target;
    private final String code;
    private final String[] params;

    private boolean received = false;
    private String result = null;

    public Request send() {
      // TODO: handle invalid requests
      this.received = true;

      var targetDevices = lookupIP(target);
      if (targetDevices.length == 0) {
        throw new NetworkException(source, target, "Invalid target IP; non-existant host");
      }
      this.result = "";
      for (NetworkDevice dev : targetDevices) {
        String res = dev.parseNetworkRequest(code, source, target, params);
        if (res != null && !res.equals("")) {
          this.result += res + "\n";
        }
      }

      this.result = this.result.trim();
      return this;
    }

    public Request(IP source, IP target, String code, String[] params) {
      if (source == null || target == null)
        throw new NetworkException("Incomplete request: missing IP address/addresses");

      this.source = source;
      this.target = target;
      this.code = code;
      this.params = params;
    }

    public Request(String requestString) {
      // <CODE> <SOURCE> <TARGET> [<PARAM1>,<PARAM2>,...] (no spaces)
      // Nested requests: $(...some request...)
      // Example:
      // SET_POW 192.168.0.10 192.168.0.15 [5.0,$(GET_TEMP 192.168.0.15
      // 192.168.0.10 [])]

      // Substitute all nested requests:
      Pattern pattern = Pattern.compile("\\$\\(.*\\)");
      Matcher matcher = pattern.matcher(requestString);
      while (matcher.find()) {
        String subrequestString = requestString.substring(matcher.start(), matcher.end() - 1);

        Request subrequest = new Request(subrequestString);
        requestString = requestString.substring(0, matcher.start()) + subrequest.getResult()
            + requestString.substring(matcher.end());
        System.out.println("Replaced: " + requestString);

        matcher = pattern.matcher(requestString);
      }

      // Parse request
      String[] split = requestString.split("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

      if (split.length != 4)
        throw new RuntimeException("Request: invalid string!");

      String code = split[0];
      IP source = new IP(split[1]);
      IP target = new IP(split[2]);

      // Params:
      String paramsString = split[3];
      paramsString = paramsString.substring(1, paramsString.length() - 1);

      String[] params = paramsString.split(",+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

      // Constructor
      this.source = source;
      this.target = target;
      this.code = code;
      this.params = params;
    }

    public boolean hasResult() {
      return received;
    }

    public String getResult() {
      return result;
    }
  }

  private NetworkDevice[] lookupIP(IP target) {
    ArrayList<NetworkDevice> res = new ArrayList<>();
    for (IP lease : leases.keySet()) {
      if (lease.equals(target))
        res.add(leases.get(lease));
    }
    return res.toArray(new NetworkDevice[] {});
  }

  public Request createRequest(IP source, IP target, String code, String[] params) {
    return new Request(source, target, code, params);
  }

  public Request createRequest(String requestString) {
    return new Request(requestString);
  }

  private final IP poolStart = new IP("192.168.0.2");
  private final IP poolEnd = new IP("192.168.0.254");

  public static class NetworkException extends RuntimeException {
    public NetworkException(String message) {
      super("NetworkException -> " + message);
    }

    public NetworkException(IP source, IP target, String message) {
      super("NetworkException for request from: " + source.getAddressString() +
          " to: " + target.getAddressString() + " -> " + message);
    }
  }

  private final HashMap<IP, NetworkDevice> leases = new HashMap<>();

  public IP leaseIP(NetworkDevice device) throws NetworkException {
    MAIN: for (int i = poolStart.getAddressRaw(); i <= poolEnd.getAddressRaw(); i++) {
      for (IP lease : leases.keySet()) {
        if (lease.getAddressRaw() == i) {
          continue MAIN;
        }
      }

      IP ip = new IP();
      ip.setAddressRaw(i);
      leases.put(ip, device);
      return ip;
    }

    System.out.println(Arrays.toString(leases.keySet().toArray(new IP[0])));
    throw new NetworkException("IP Lease failed; no free addresses left in pool");
  }

  public void freeIP(IP ip) {
    leases.remove(ip);
  }
}
