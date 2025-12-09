package Network;

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

    protected void registerNetworkCode(String code, BiFunction<IP[], String[], String> exec) {
        networkCodes.put(code, exec);
    }

    protected void registerNetworkCode(String code, Function<String[], String> exec) {
        networkCodes.put(code, (IP[] _, String[] params) -> exec.apply(params));
    }

    protected void registerNetworkCode(String code, Supplier<String> exec) {
        networkCodes.put(code, (IP[] _, String[] _) -> exec.get());
    }

    protected void registerNetworkCode(String code, BiConsumer<IP[], String[]> exec) {
        networkCodes.put(code, (IP[] ips, String[] params) -> {exec.accept(ips, params); return null;});
    }

    protected void registerNetworkCode(String code, Consumer<String[]> exec) {
        networkCodes.put(code, (IP[] _, String[] params) -> {exec.accept(params); return null;});
    }

    protected void registerNetworkCode(String code, Runnable exec) {
        networkCodes.put(code, (IP[] _, String[] _) -> {exec.run(); return null;});
    }

    public String parseNetworkRequest(String code, IP source, IP target, String[] params) {
        if (!networkCodes.containsKey(code)) {
            throw new RuntimeException("Unknown network code " + code);
        }

        return networkCodes.get(code).apply(new IP[]{source, target}, params);
    }

    public NetworkManager.Request createRequest(IP target, String code, String[] params) {
        return this.networkManager.createRequest(ip, target, code, params);
    }
}