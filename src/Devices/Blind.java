package Devices;

public class Blind extends SHDevice {
    private boolean open = false;  // Status żaluzji, otwarte/zamknięte

    // Konstruktor klasy Blind
    public Blind() {
        // Rejestracja kodów sieciowych
        registerNetworkCode("GET_STATUS", () -> String.valueOf(open));  // Zwraca stan otwarcia/otwarty
        registerNetworkCode("SET_STATUS", (String[] params) -> this.setStatus(Boolean.parseBoolean(params[0])));  // Ustawia stan
    }

    // Getter statusu żaluzji
    public boolean isOpen() {
        return open;
    }

    // Setter statusu żaluzji
    public void setStatus(boolean open) {
        this.open = open;
    }
}
