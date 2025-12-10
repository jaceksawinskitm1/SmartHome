package Devices;

public class Light extends SHDevice {
    private boolean isOn = false;
    private double brightness = 0.0;
    private String hexColor = "#FFFFFF";

    public Light() {
        this.isOn = false;

        registerNetworkCode("GET_STATE", () -> isOn ? "ON" : "OFF");
        registerNetworkCode("SET_STATE", (String[] params) -> {
            this.setState(params[0].equals("ON"));
        });
        registerNetworkCode("GET_BRIGHTNESS", () -> String.valueOf(this.getBrightness()));
        registerNetworkCode("SET_BRIGHTNESS", (String[] params) -> {
            this.setBrightness(Double.parseDouble(params[0]));
        });
        registerNetworkCode("GET_COLOR", () -> hexColor);
        registerNetworkCode("SET_COLOR", (String[] params) -> {
            this.setColor(params[0]);
        });
    }

    public void setState(boolean state) {
        this.isOn = state;
    }

    public void setColor(String hexColor) {
        this.hexColor = hexColor;
    }

    public void setBrightness(double brightness) {
        this.brightness = brightness;
    }

    public double getBrightness() {
        return brightness;
    }
}