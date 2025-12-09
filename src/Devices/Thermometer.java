package Devices;

public class Thermometer extends SHDevice {
    private double temperature = 0;

    public Thermometer() {
        temperature = 20;

        // Network
        registerNetworkCode("GET_TEMPERATURE", () -> String.valueOf(temperature));
    }

    public void _changeTemp(double temperature) {
        this.temperature = temperature;
    }

    public void _enableTempSimulation(Thermometer termometr) {
        // Only used for simulating
        this.addEvent(() -> {
            double f = 0.1;
            double a = this.getTemperature();
            double b = termometr.getTemperature();
            this._changeTemp((a * (1.0 - f)) + (b * f));
        });
    }

    public double getTemperature() {
        return temperature;
    }
}
