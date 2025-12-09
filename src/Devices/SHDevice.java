package Devices;

import Network.IP;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

public abstract class SHDevice {
    private IP ip;
    private SHManager manager;

    private final ArrayDeque<Runnable> actions = new ArrayDeque<>();
    private final ArrayList<Runnable> events = new ArrayList<>();

    private final HashMap<String, Function<String[], String>> networkCodes = new HashMap<>();

    void registerNetworkCode(String code, Function<String[], String> exec) {
        networkCodes.put(code, exec);
    }

    public void setManager(SHManager manager) {
        this.manager = manager;

        ip = this.manager.getNetworkManager().leaseIP(this);
    }

    public IP getIP() {
        return ip;
    }

    public String parseNetworkRequest(String code, String[] params) {
        if (!networkCodes.containsKey(code)) {
            throw new RuntimeException("Unknown network code " + code);
        }

        return networkCodes.get(code).apply(params);
    }

    public void queueAction(Runnable action) {
        actions.add(action);
    }

    public void addEvent(Runnable event) {
        if (events.contains(event))
            return;

        events.add(event);
    }

    public void removeEvent(Runnable event) {
        events.remove(event);
    }

    public void refresh() {}

    public void events() {
        for (Runnable event : events) {
            event.run();
        }

        while (!actions.isEmpty())
            actions.pop().run();
    }
}
