package Devices;

public class Heater extends SHDevice {
    private double power = 0;

    public Heater() {

        // Network
        registerNetworkCode("GET_POW", (String[] params) -> String.valueOf(power));
        registerNetworkCode("SET_POW", (String[] params) -> {
            this.power = Double.valueOf(params[0]); return null;
        });
    }

    public double getPower() {
        return power;
    }

    public void setPower(double power) {
        this.power = power;
    }
}
