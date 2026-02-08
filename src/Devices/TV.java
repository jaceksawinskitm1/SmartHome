package Devices;

public class TV extends SHDevice {
    private boolean isOn = false;  // Stan telewizora (włączony/wyłączony)
    private double volume = 0.5;       // Poziom głośności (domyślnie 50)
    private int channel = 1;       // Numer kanału (domyślnie 1)

    // Konstruktor klasy TV
    public TV() {
        // Rejestracja kodów sieciowych
        registerNetworkCode("GET_STATUS", "BOOL", () -> String.valueOf(isOn));  // Zwraca stan telewizora (ON lub OFF)
        registerNetworkCode("SET_STATUS", "BOOL", (String[] params) -> this.setStatus(params[0].equals("true")));  // Ustawia stan (włączony/wyłączony)

        registerNetworkCode("GET_VOLUME", "RANGE", () -> String.valueOf(volume));  // Zwraca poziom głośności
        registerNetworkCode("SET_VOLUME", "RANGE", (String[] params) -> this.setVolume(Double.parseDouble(params[0])));  // Ustawia poziom głośności

        registerNetworkCode("GET_CHANNEL", "INT", () -> String.valueOf(channel));  // Zwraca numer kanału
        registerNetworkCode("SET_CHANNEL", "INT", (String[] params) -> this.setChannel(Integer.parseInt(params[0])));  // Ustawia numer kanału
    }

    // Getter stanu telewizora
    public boolean isOn() {
        return isOn;
    }



    // Setter stanu telewizora na podstawie wartości "ON" lub "OFF"
    public void setStatus(boolean isOn) {
        this.isOn = isOn;
    }

    // Getter poziomu głośności
    public double getVolume() {
        return volume;
    }

    // Setter poziomu głośności
    public void setVolume(double volume) {
        if (volume >= 0 && volume <= 1) {
            this.volume = volume;  // Poziom głośności w zakresie 0-100
        }
    }

    // Getter numeru kanału
    public int getChannel() {
        return channel;
    }

    // Setter numeru kanału
    public void setChannel(int channel) {
        if (channel > 0) {
            this.channel = channel;  // Numer kanału nie może być mniejszy niż 1
        }
    }
}
