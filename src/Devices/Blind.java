package Devices;

public class Blind extends SHDevice {
    private boolean open = true;

    public Blind() {
        open = true;
    }

    public boolean isOpened() {
        return this.open;
    }

    public void open() {
        this.open = true;
    }

    public void close() {
        this.open = false;
    }
}
