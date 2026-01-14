package Devices;

public class CoffeeMachine extends SHDevice {
    public enum Status {IDLE, READY, GRINDING};
    private Status status = Status.IDLE; // IDLE, GRINDING, BREWING, READY
    private double progress = 0.0;  // 0% - 100% postępu parzenia
    private int scheduleTimer = -1; // Odliczanie do startu (-1 = brak harmonogramu)

    public CoffeeMachine() {
        this.status = Status.IDLE;

        registerNetworkCode("GET_STATUS", () -> status.toString());
        registerNetworkCode("GET_PROGRESS", () -> String.valueOf((int)progress));


        registerNetworkCode("COFFEE", (String[] args) -> {
            startProcess();
        });

        // Ustawienie harmonogramu (za ile cykli ma zrobić kawę)
        // np. SET_TIMER 100 -> za 100 cykli włączy się ekspres
        registerNetworkCode("SET_TIMER", (String[] args) -> {
            if (args.length > 0) {
                try {
                    int ticks = Integer.parseInt(args[0]);
                    setTimer(ticks);
                    return "TIMER_SET_" + ticks;
                } catch (NumberFormatException e) {
                    return "ERROR";
                }
            }
            return "ERROR";
        });
    }

    public void startProcess() {
        this.status = Status.GRINDING;
        this.progress = 0;
    }

    public void setTimer(int ticks) {
        this.scheduleTimer = ticks;
    }


    public String getStatus() {
        return status + " (" + (int)progress + "%)";
    }
}