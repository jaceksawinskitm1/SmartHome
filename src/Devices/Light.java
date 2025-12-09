package Devices;

public class Light extends SHDevice {
    private boolean on = false;
    private double brightness = 100;
    private int[] color = new int[] {255, 255, 255};

    public boolean isOn() {
        return this.on;
    }

    public double getBrightness() {
        return this.brightness;
    }

    public int[] getColor() {
        return this.color;
    }

    public void setBrightness(double brightness) {
        this.brightness = brightness;
    }

    public void setColor(int r, int g, int b) {
        this.color = new int[] {r, g, b};
    }
}
