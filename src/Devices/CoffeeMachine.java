package Devices;

public class CoffeeMachine extends SHDevice {
    private String status = "IDLE"; // IDLE, GRINDING, BREWING, READY
    private double progress = 0.0;  // 0% - 100% postępu parzenia
    private int scheduleTimer = -1; // Odliczanie do startu (-1 = brak harmonogramu)

    public CoffeeMachine() {
        this.status = "IDLE";

        registerNetworkCode("GET_STATUS", () -> status);
        registerNetworkCode("GET_PROGRESS", () -> String.valueOf((int)progress));


        registerNetworkCode("MAKE_COFFEE", (String[] args) -> {
            if (status.equals("IDLE") || status.equals("READY")) {
                _startProcess();
                return "STARTED";
            }
            return "BUSY";
        });

        // Ustawienie harmonogramu (za ile cykli ma zrobić kawę)
        // np. SET_TIMER 100 -> za 100 cykli włączy się ekspres
        registerNetworkCode("SET_TIMER", (String[] args) -> {
            if (args.length > 0) {
                try {
                    int ticks = Integer.parseInt(args[0]);
                    _setTimer(ticks);
                    return "TIMER_SET_" + ticks;
                } catch (NumberFormatException e) {
                    return "ERROR";
                }
            }
            return "ERROR";
        });

        // Odbiór gotowej kawy (resetuje stan do IDLE)
        registerNetworkCode("TAKE_COFFEE", (String[] args) -> {
            if (status.equals("READY")) {
                status = "IDLE";
                progress = 0;
                return "YUMMY";
            }
            return "NO_COFFEE";
        });
    }

    public void _startProcess() {
        this.status = "GRINDING";
        this.progress = 0;
    }

    public void _setTimer(int ticks) {
        this.scheduleTimer = ticks;
    }


    public String getStatus() {
        return status + " (" + (int)progress + "%)";
    }
}