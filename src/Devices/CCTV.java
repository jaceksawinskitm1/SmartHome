package Devices;

public class CCTV extends SHDevice {
    private boolean movementDetected = false; // Detekcja ruchu (czy wykryto ruch)

    // Konstruktor klasy CCTV
    public CCTV() {
        registerNetworkCode("GET_MOVEMENT", "BOOL", () -> String.valueOf(movementDetected));  // Zwraca, czy wykryto ruch
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
