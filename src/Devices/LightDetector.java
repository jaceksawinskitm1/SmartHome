package Devices;

public class LightDetector extends SHDevice {
    private LightState state = LightState.NIGHT;  // Początkowy stan (noc)

    // Enum stanu wykrywacza światła
    public enum LightState {
        DAY,   // Dzień
        NIGHT  // Noc
    }

    // Konstruktor klasy LightDetector
    public LightDetector() {
        // Rejestracja kodów sieciowych
        registerNetworkCode("GET_STATE", "STRING", () -> state.name());  // Zwraca stan (DAY lub NIGHT)
        registerNetworkCode("SET_STATE", "STRING", (String[] params) -> this.setState(LightState.valueOf(params[0])));  // Ustawia stan
    }

    // Getter stanu wykrywacza
    public LightState getState() {
        return state;
    }

    // Setter stanu wykrywacza
    public void setState(LightState state) {
        this.state = state;
    }
}
