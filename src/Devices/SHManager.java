package Devices;

import Network.NetworkManager;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class SHManager {
    public static class DeviceLogic {
        public SHDevice source;
        public NetworkManager.Request sourceReq;
        public SHDevice target;
        public NetworkManager.Request targetReq;
        public String condition;

        public DeviceLogic(SHDevice source, SHDevice target, String condition) {
            this.source = source;
            this.target = target;
            this.condition = condition;
        }

        public void Try() {

        }
    }

    private NetworkManager networkManager;

    private ArrayList<SHDevice> devices = new ArrayList<>();

    //private ArrayList<DeviceLogic>

    public NetworkManager getNetworkManager() {
        return networkManager;
    }

    public void addDevice(SHDevice dev) {
        if (devices.contains(dev))
            return;

        devices.add(dev);
    }

    public void removeDevice(SHDevice dev) {
        devices.remove(dev);
    }

    public SHManager() {
        this(new NetworkManager());
    }

    public SHManager(NetworkManager networkManager) {
        Thread backgroundLoop = new Thread(this::loop);
        backgroundLoop.start();
        this.networkManager = networkManager;
    }

    private void loop() {
        // Pętla jest wykonywana co sekundę, żeby odświerzyć wszystkie wartości

        for (SHDevice dev : devices) {
            dev.refresh();
            dev.events();
        }

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        loop();
    }
}
