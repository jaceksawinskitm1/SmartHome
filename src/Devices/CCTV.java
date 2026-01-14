package Devices;

public class CCTV extends SHDevice {
    private boolean isOn = false;          // Stan kamery (włączona/wyłączona)
    private boolean movementDetected = false; // Detekcja ruchu (czy wykryto ruch)

    // Konstruktor klasy CCTV
    public CCTV() {
        // Rejestracja kodów sieciowych
        registerNetworkCode("GET_STATUS", "BOOL", () -> String.valueOf(isOn));  // Zwraca stan kamery (włączona/wyłączona)
        registerNetworkCode("SET_STATUS", "BOOL", (String[] params) -> this.setStatus(Boolean.parseBoolean(params[0])));  // Ustawia stan (włączona/wyłączona)

        registerNetworkCode("GET_MOVEMENT", "BOOL", () -> String.valueOf(movementDetected));  // Zwraca, czy wykryto ruch
        registerNetworkCode("SET_MOVEMENT", "BOOL", (String[] params) -> this.setMovement(Boolean.parseBoolean(params[0])));  // Ustawia stan detekcji ruchu
    }

    // Getter stanu kamery (włączona/wyłączona)
    public boolean isOn() {
        return isOn;
    }

    // Setter stanu kamery (włącz/wyłącz)
    public void setStatus(boolean isOn) {
        this.isOn = isOn;
    }

    // Getter stanu detekcji ruchu
    public boolean isMovementDetected() {
        return movementDetected;
    }

    // Setter stanu detekcji ruchu
    public void setMovement(boolean movementDetected) {
        this.movementDetected = movementDetected;
    }
}
