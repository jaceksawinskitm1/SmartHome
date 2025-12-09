package Network;

import Devices.SHDevice;

import java.util.HashMap;

public class NetworkManager {
    private IP poolStart = new IP("192.168.0.1");
    private IP poolEnd = new IP("192.168.0.255");
    private IP lastLease = null;

    private HashMap<IP, SHDevice> leases = new HashMap<>();

    public Object sendRequest(IP source, IP target, String code, Object[] params) {
        // TODO: handle invalid requests
        return leases.get(target).parseNetworkRequest(code, params);
    }

    public byte[] leaseIP(SHDevice device) {
        if (lastLease == null) {
            lastLease = poolStart.getNext();
        } else {
            lastLease = lastLease.getNext();
        }

        leases.put(lastLease, device);

        return lastLease.getAddress();
    }
}
