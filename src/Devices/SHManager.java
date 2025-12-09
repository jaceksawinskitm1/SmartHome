package Devices;

import Network.NetworkManager;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class SHManager {
    private NetworkManager networkManager;

    private ArrayList<SHDevice> devices = new ArrayList<>();

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
