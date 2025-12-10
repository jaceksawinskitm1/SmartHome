package Devices;

public class AirConditioner extends SHDevice {
    private boolean isOn = false;
    private double currentCoolingPower = 0.0; // 0-10 (stopień chłodzenia)
    private double targetCoolingPower = 0.0;

    public AirConditioner() {
        this.isOn = false;

        registerNetworkCode("GET_STATE", () -> isOn ? "ON" : "OFF");
        registerNetworkCode("GET_POWER", () -> String.valueOf(currentCoolingPower));

        // Ustawianie mocy chłodzenia (0 = wyłącz, 10 = max)
        registerNetworkCode("SET_COOLING", (String[] arg) -> {
            try {
                double val = Double.parseDouble(arg[0]);
                _setCooling(val);
                return "OK";
            } catch (NumberFormatException e) {
                return "ERROR";
            }
        });
    }

    public void _setCooling(double power) {
        if (power <= 0) {
            this.targetCoolingPower = 0;
            this.isOn = false;
        } else {
            // Ograniczamy max moc do 10
            if (power > 10) power = 10;
            this.targetCoolingPower = power;
            this.isOn = true;
        }
    }

    public double getCoolingPower() {
        return currentCoolingPower;
    }
}