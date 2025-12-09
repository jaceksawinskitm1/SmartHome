package Devices;

/*
    Każde urządzenie ma konstruktor w którym dodaje się wszystkie kody sieciowe (to są poprostu jakieś teksty które mają rozróżniać różne polecenia, np. GET_POWER, SET_POWER)
    Można je definiować w wielu kombinacjach:
    1. Przyjmuje IP[] i String[], zwraca String
    2. Przyjmuje String[], zwraca String
    3. Nie przyjmuje nic, zwraca String
    4. Przyjmuje IP[] i String[], nie zwraca nic
    5. Przyjmuje String[], nie zwraca nic
    6. Nie przyjmuje nic, nie zwraca nic
    W zależności od potrzeby.
 */

public class Heater extends SHDevice {
    private double power = 0;

    public Heater() {

        // Network
        registerNetworkCode("GET_POWER", () -> String.valueOf(power));
        registerNetworkCode("SET_POWER", (String[] params) -> this.setPower(Double.parseDouble(params[0])));
    }

    public double getPower() {
        return power;
    }

    public void setPower(double power) {
        this.power = power;
    }
}
