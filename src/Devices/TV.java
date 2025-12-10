package Devices;

public class TV extends SHDevice {
    private boolean isOn = false;  // Stan telewizora (włączony/wyłączony)
    private int volume = 50;       // Poziom głośności (domyślnie 50)
    private int channel = 1;       // Numer kanału (domyślnie 1)

    // Konstruktor klasy TV
    public TV() {
        // Rejestracja kodów sieciowych
        registerNetworkCode("GET_STATUS", () -> isOn ? "ON" : "OFF");  // Zwraca stan telewizora (ON lub OFF)
        registerNetworkCode("SET_STATUS", (String[] params) -> this.setStatus(params[0].equals("ON")));  // Ustawia stan (włączony/wyłączony)

        registerNetworkCode("GET_VOLUME", () -> String.valueOf(volume));  // Zwraca poziom głośności
        registerNetworkCode("SET_VOLUME", (String[] params) -> this.setVolume(Integer.parseInt(params[0])));  // Ustawia poziom głośności

        registerNetworkCode("GET_CHANNEL", () -> String.valueOf(channel));  // Zwraca numer kanału
        registerNetworkCode("SET_CHANNEL", (String[] params) -> this.setChannel(Integer.parseInt(params[0])));  // Ustawia numer kanału
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
    public int getVolume() {
        return volume;
    }

    // Setter poziomu głośności
    public void setVolume(int volume) {
        if (volume >= 0 && volume <= 100) {
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
