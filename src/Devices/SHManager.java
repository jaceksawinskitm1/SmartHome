package Devices;

import Network.IP;
import Network.NetworkDevice;
import Network.NetworkManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class SHManager extends NetworkDevice {

    public static class DeviceLogic {
        private final Comparator[] comparators;
        private String successRequest;
        private final SHManager manager;

        public DeviceLogic(SHDevice device, String code, String[] params, SHManager manager, Comparator... comparators) {
            this.comparators = comparators;

            this.successRequest = code.toUpperCase() + " " + manager.getIP().getAddressString() + " " + device.getIP().getAddressString() + " [";
            for (String p : params)
                this.successRequest += p + ",";
            this.successRequest = this.successRequest.substring(0, this.successRequest.length() - 1) + "]";

            this.manager = manager;
        }

        public DeviceLogic(String successRequest, SHManager manager, Comparator... comparators) {
            this.comparators = comparators;
            this.successRequest = successRequest;
            this.manager = manager;
        }

        public void Try() {
            for (Comparator comp : comparators) {
                if (!comp.test()) {
                    return;
                }
            }

            manager.networkManager.createRequest(successRequest).send();
        }
    }

    public static class Comparator {
        public enum Condition {EQUAL, GREATER_THAN, LESS_THAN, GREATER_EQUAL, LESS_EQUAL, NOT_EQUAL};

        private final boolean isAConst;
        private final String paramA;

        private final Condition condition;

        private final boolean isBConst;
        private final String paramB;

        private final SHManager manager;

        // Variable <-> Constant comparison
        public Comparator(SHDevice deviceA, String codeA, Condition condition, String constB, SHManager manager) {
            this.isAConst = false;
            this.isBConst = true;
            this.paramA = codeA.toUpperCase() + " " + manager.getIP().getAddressString() + " " + deviceA.getIP().getAddressString() + " []";
            this.paramB = constB;

            this.condition = condition;
            this.manager = manager;
        }

        // Variable <-> Variable comparison
        public Comparator(SHDevice deviceA, String codeA, Condition condition, SHDevice deviceB, String codeB, SHManager manager) {
            this.isAConst = false;
            this.isBConst = false;
            this.paramA = codeA.toUpperCase() + " " + manager.getIP().getAddressString() + " " + deviceA.getIP().getAddressString() + " []";
            this.paramB = codeB.toUpperCase() + " " + manager.getIP().getAddressString() + " " + deviceB.getIP().getAddressString() + " []";

            this.condition = condition;
            this.manager = manager;
        }

        // Const <-> Variable comparison
        public Comparator(String constA, Condition condition, SHDevice deviceB, String codeB, SHManager manager) {
            this.isAConst = true;
            this.isBConst = false;
            this.paramA = constA;
            this.paramB = codeB.toUpperCase() + " " + manager.getIP().getAddressString() + " " + deviceB.getIP().getAddressString() + " []";

            this.condition = condition;
            this.manager = manager;
        }

        public boolean test() {
            String valueA, valueB;

            if (isAConst) {
                valueA = paramA;
            } else {
                NetworkManager.Request req = this.manager.networkManager.createRequest(paramA);
                req.send();
                valueA = req.getResult();
            }

            if (isBConst) {
                valueB = paramB;
            } else {
                NetworkManager.Request req = this.manager.networkManager.createRequest(paramB);
                req.send();
                valueB = req.getResult();
            }

            switch (this.condition) {
                case EQUAL -> {return Objects.equals(valueA, valueB);}
                case NOT_EQUAL -> {return !Objects.equals(valueA, valueB);}
            }

            // Cast to double for further comparisons
            double dA, dB;

            try {
                dA = Double.parseDouble(valueA);
                dB = Double.parseDouble(valueB);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Comparator: invalid data, can't cast " + valueA + " and " + valueB + " to double.");
            }


            switch (this.condition) {
                case GREATER_THAN -> {return dA > dB;}
                case GREATER_EQUAL -> {return dA >= dB;}
                case LESS_THAN -> {return dA < dB;}
                case LESS_EQUAL -> {return dA <= dB;}
                default -> {return false;}
            }
        }
    }


    // SHManager

    private final NetworkManager networkManager;

    private final HashMap<String, SHDevice> devices = new HashMap<>();

    private final ArrayList<DeviceLogic> logics = new ArrayList<>();

    public NetworkManager getNetworkManager() {
        return networkManager;
    }

    public void registerDevice(String id, SHDevice dev) {
        if (devices.containsValue(dev))
            return;

        dev.leaseIP(this.getNetworkManager());
        devices.put(id, dev);
    }

    public void deregisterDevice(String id) {
        devices.remove(id);
    }


    // Variable <-> Constant comparison
    public Comparator createComparator(SHDevice deviceA, String codeA, Comparator.Condition condition, String constB) {
        return new Comparator(deviceA, codeA, condition, constB, this);
    }

    // Variable <-> Variable comparison
    public Comparator createComparator(SHDevice deviceA, String codeA, Comparator.Condition condition, SHDevice deviceB, String codeB) {
        return new Comparator(deviceA, codeA, condition, deviceB, codeB, this);
    }

    // Const <-> Variable comparison
    public Comparator createComparator(String constA, Comparator.Condition condition, SHDevice deviceB, String codeB) {
        return new Comparator(constA, condition, deviceB, codeB, this);
    }


    public void registerLogic(SHDevice device, String code, String[] params, Comparator... comparators) {
        DeviceLogic logic = new DeviceLogic(device, code, params, this, comparators);

        logics.add(logic);
    }

    public void registerLogic(String successRequest, Comparator... comparators) {
        DeviceLogic logic = new DeviceLogic(successRequest, this, comparators);

        logics.add(logic);
    }

    public void deregisterLogic(DeviceLogic logic) {
        logics.remove(logic);
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
            for (SHDevice dev : devices.values()) {
                res += dev.getIP().getAddressString() + ",";
            }
            res = res.substring(0, res.length() - 1) + "]";

            return res;
        });
    }

    private void loop() {
        // Runs every second

        // Depracated, use logics instead
        for (SHDevice dev : devices.values()) {
            dev.refresh();
            dev.events();
        }

        // Try every logic connection
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
