package Devices;

public class Light extends SHDevice {
    private boolean isOn = false;
    private double brightness = 1.0;
    private String hexColor = "#FFFFFF";

    public Light() {
        this.isOn = false;

        registerNetworkCode("GET_STATE", "BOOL", () -> String.valueOf(getState()));
        registerNetworkCode("SET_STATE", "BOOL", (String[] params) -> {
            this.setState(params[0].equals("true"));
        });
        registerNetworkCode("GET_BRIGHTNESS", "RANGE", () -> String.valueOf(this.getBrightness()));
        registerNetworkCode("SET_BRIGHTNESS", "RANGE", (String[] params) -> {
            this.setBrightness(Double.parseDouble(params[0]));
        });
        registerNetworkCode("GET_COLOR", "COLOR", () -> getColor());
        registerNetworkCode("SET_COLOR", "COLOR", (String[] params) -> {
            this.setColor(params[0]);
        });
    }

    public void setState(boolean state) {
        this.isOn = state;
    }
    public boolean getState() {
        return this.isOn;
    }

    public void setColor(String hexColor) {
        this.hexColor = hexColor;
    }
    public String getColor() {
        return this.hexColor;
    }

    public void setBrightness(double brightness) {
        this.brightness = brightness;
    }

    public double getBrightness() {
        return brightness;
    }
}
