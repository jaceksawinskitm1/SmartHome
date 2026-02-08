package Devices;

public class GarageDoor extends SHDevice {
    private double position = 1.0;  // Pozycja bramy, 0.0 = otwarta, 1.0 = zamknięta
    private DoorState state = DoorState.CLOSED;  // Początkowy stan bramy (zamknięta)

    // Enum stanu bramy
    public enum DoorState {
        OPEN,             // Brama otwarta (0)
        CLOSED,           // Brama zamknięta (100)
        PARTIALLY_OPENED, // Brama częściowo otwarta (pozycja 1-99)
    }

    // Konstruktor klasy GarageDoor
    public GarageDoor() {
        // Rejestracja kodów sieciowych
        registerNetworkCode("GET_POSITION", "RANGE", () -> String.valueOf(getPosition()));  // Zwraca pozycję bramy
        registerNetworkCode("SET_POSITION", "RANGE", (String[] params) -> this.setPosition(Double.parseDouble(params[0])));  // Ustawia pozycję bramy

        registerNetworkCode("GET_STATE", "STRING", () -> state.name());  // Zwraca stan bramy (OPEN, CLOSED, PARTIALLY_OPENED)
    }

    // Getter pozycji bramy
    public double getPosition() {
        return position;
    }

    // Setter pozycji bramy (0-100)
    public void setPosition(double position) {
        if (position < 0) {
            this.position = 0;  // Brama nie może być otwarta poniżej 0
        } else if (position > 1.0) {
            this.position = 1.0;  // Brama nie może być zamknięta powyżej 100
        } else {
            this.position = position;  // Ustawia pozycję bramy
        }

        // Ustawienie stanu na podstawie pozycji
        if (position == 0) {
            this.state = DoorState.OPEN;  // Brama otwarta
        } else if (position == 1.0) {
            this.state = DoorState.CLOSED;  // Brama zamknięta
        } else {
            this.state = DoorState.PARTIALLY_OPENED;  // Brama częściowo otwarta
        }
    }

    // Getter stanu bramy
    public DoorState getState() {
        return state;
    }

    // Setter stanu bramy
    public void setState(DoorState state) {
        this.state = state;
    }
}
