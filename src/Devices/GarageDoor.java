package Devices;

public class GarageDoor extends SHDevice {
    private int position = 100;  // Pozycja bramy, 0 = otwarta, 100 = zamknięta
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
        registerNetworkCode("GET_POSITION", () -> String.valueOf(position));  // Zwraca pozycję bramy
        registerNetworkCode("SET_POSITION", (String[] params) -> this.setPosition(Integer.parseInt(params[0])));  // Ustawia pozycję bramy

        registerNetworkCode("GET_STATE", () -> state.name());  // Zwraca stan bramy (OPEN, CLOSED, PARTIALLY_OPENED)
        registerNetworkCode("SET_STATE", (String[] params) -> this.setState(DoorState.valueOf(params[0])));  // Ustawia stan bramy
    }

    // Getter pozycji bramy
    public int getPosition() {
        return position;
    }

    // Setter pozycji bramy (0-100)
    public void setPosition(int position) {
        if (position < 0) {
            this.position = 0;  // Brama nie może być otwarta poniżej 0
        } else if (position > 100) {
            this.position = 100;  // Brama nie może być zamknięta powyżej 100
        } else {
            this.position = position;  // Ustawia pozycję bramy
        }

        // Ustawienie stanu na podstawie pozycji
        if (position == 0) {
            state = DoorState.OPEN;  // Brama otwarta
        } else if (position == 100) {
            state = DoorState.CLOSED;  // Brama zamknięta
        } else {
            state = DoorState.PARTIALLY_OPENED;  // Brama częściowo otwarta
        }
    }

    // Getter stanu bramy
    public DoorState getState() {
        return state;
    }

    // Setter stanu bramy
    public void setState(DoorState state) {
        this.state = state;

        // Ustawienie pozycji na podstawie stanu
        switch (state) {
            case OPEN:
                setPosition(0);  // Ustawia bramę na otwartą
                break;
            case CLOSED:
                setPosition(100);  // Ustawia bramę na zamkniętą
                break;
            case PARTIALLY_OPENED:
                // Można ustawić dowolną pozycję pomiędzy 1 a 99, np. na 50
                setPosition(50);
                break;
        }
    }
}
