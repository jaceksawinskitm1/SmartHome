package Devices;

public class LightDetector extends SHDevice {
    private boolean night = true;  // Początkowy stan (noc)

    // Konstruktor klasy LightDetector
    public LightDetector() {
        // Rejestracja kodów sieciowych
        registerNetworkCode("GET_NIGHT", "BOOL", () -> String.valueOf(night));  // Zwraca stan (DAY lub NIGHT)
        registerNetworkCode("SET_NIGHT", "BOOL", (String[] params) -> this.setState(Boolean.valueOf(params[0])));  // Ustawia stan
    }

    // Getter stanu wykrywacza
    public boolean getState() {
        return night;
    }

    // Setter stanu wykrywacza
    public void setState(boolean night) {
        this.night = night;
    }
}
