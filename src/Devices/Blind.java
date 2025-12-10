package Devices;

public class Blind extends SHDevice {
    private boolean open = false;  // Status żaluzji, otwarte/zamknięte

    // Konstruktor klasy Blind
    public Blind() {
        // Rejestracja kodów sieciowych
        registerNetworkCode("GET_STATUS", () -> open ? "ON" : "OFF");  // Zwraca stan otwarcia/otwarte (ON lub OFF)
        registerNetworkCode("SET_STATUS", (String[] params) -> this.setStatus(params[0].equals("ON")));  // Ustawia stan (otwarte/ zamknięte)
    }

    // Getter statusu żaluzji
    public boolean isOpen() {
        return open;
    }

    // Setter statusu żaluzji (otwarte/zamknięte)
    public void setStatus(boolean open) {
        this.open = open;
    }
}
