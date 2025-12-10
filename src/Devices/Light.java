package Devices;

public class Light extends SHDevice {
    private boolean isOn = false;
    private double currentBrightness = 0.0;
    private double targetBrightness = 1.0;
    private String hexColor = "#FFFFFF";

    public Light() {
        this.isOn = false;

        registerNetworkCode("GET_STATE", () -> isOn ? "ON" : "OFF");
        registerNetworkCode("GET_BRIGHTNESS", () -> String.valueOf(currentBrightness));
        registerNetworkCode("GET_COLOR", () -> hexColor);

    }

    public void setState(boolean state) {
        this.isOn = state;
    }

    public void setColor(String hexColor) {
        this.hexColor = hexColor;
    }

    public void setBrightness(double brightness) {
        if (brightness < 0.0) brightness = 0.0;
        if (brightness > 1.0) brightness = 1.0;
        this.targetBrightness = brightness;
    }

    public double getBrightness() {
        return currentBrightness;
    }
}