package Devices;

import Network.IP;
import Network.NetworkDevice;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

public abstract class SHDevice extends NetworkDevice {
    private SHManager manager;

    private final ArrayList<Runnable> events = new ArrayList<>();

    public void setManager(SHManager manager) {
        this.manager = manager;

        leaseIP(this.manager.getNetworkManager());
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

    public void refresh() {}

    public void events() {
        for (Runnable event : events) {
            event.run();
        }
    }
}
