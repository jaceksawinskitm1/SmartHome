package Network;

import Devices.SHDevice;

import java.util.Arrays;
import java.util.HashMap;

public class NetworkManager {
    public static void main(String[] args) {
        NetworkManager nm = new NetworkManager();
        Request req = nm.sendRequest("SET_POW 192.168.0.10 192.168.0.15 [$(0)]", "GET_TEMP 192.168.0.15 192.168.0.10 []");
    }

    public class Request {
        private IP source;
        private IP target;
        private String code;
        private String[] params;

        private boolean received = false;
        private String result = null;

        private void perform() {
            // TODO: handle invalid requests
            this.received = true;
            this.result = leases.get(target).parseNetworkRequest(code, params);
        }

        public Request(IP source, IP target, String code, String[] params) {
            this.source = source;
            this.target = target;
            this.code = code;
            this.params = params;

            perform();
        }

        public Request(String requestString, String... additional) {
            // <CODE> <SOURCE> <TARGET> [<PARAM1>,<PARAM2>,...] (no spaces)
            // Nested requests: $(request id)
            // Example:
            // SET_POW 192.168.0.10 192.168.0.15 [5.0, $(0)], GET_TEMP 192.168.0.15 192.168.0.10 []
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
            System.out.println(Arrays.toString(params));

            for (int i = 0; i < params.length; i++) {
                if (!(params[i] instanceof String))
                    continue;
                String str = (String)params[i];
                if (str.matches("\\$\\(\\d+\\)")) {
                    // Nested request:
                    int id = Integer.parseInt(str.substring(2, str.length() - 1));
                    params[i] = new Request(additional[id]);
                }

            }

            // Constructor
            this.source = source;
            this.target = target;
            this.code = code;
            this.params = params;

            perform();
        }

        public boolean hasResult() {
            return received;
        }

        public Object getResult() {
            return result;
        }
    }

    public Request sendRequest(IP source, IP target, String code, String[] params) {
        return new Request(source, target, code, params);
    }

    public Request sendRequest(String requestString, String... additional) {
        return new Request(requestString);
    }

    private IP poolStart = new IP("192.168.0.2");
    private IP poolEnd = new IP("192.168.0.254");

    public static class NetworkException extends RuntimeException {
        public NetworkException(IP source, IP target, String message)
        {
            super("NetworkException for request from: " + source.getAddressString() + " to: " + target.getAddressString() + " -> " + message);
        }
    }

    private HashMap<IP, SHDevice> leases = new HashMap<>();



    public IP leaseIP(SHDevice device) throws NetworkException {
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

        throw new NetworkException(null, null, "IP Lease failed; no free addresses left in pool");
    }
}
