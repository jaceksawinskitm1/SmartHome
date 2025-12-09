package Devices;

public class Heater extends SHDevice {
    private double power = 0;

    public Heater() {

        // Network
        addNetworkCode("GET_POW", (Object[] params) -> power);
        addNetworkCode("SET_POW", (Object[] params) -> {
            this.power = (double)params[0]; return null;
        });
    }

    public double getPower() {
        return power;
    }

    public void setPower(double power) {
        this.power = power;
    }
}
