package Devices;

public class AudioDevice extends SHDevice {
    private double currentVolume = 0.0; // Aktualna głośność (0-100)
    private double targetVolume = 0.0;  // Docelowa głośność

    public AudioDevice() {
        this.currentVolume = 0.0;
        this.targetVolume = 0.0;

        // Pobierz aktualną głośność
        registerNetworkCode("GET_VOLUME", () -> String.valueOf((int)currentVolume));

        registerNetworkCode("SET_VOLUME", (String[] args) -> {
            if (args.length > 0) {
                try {
                    double vol = Double.parseDouble(args[0]);
                    _setVolume(vol);
                    return "OK";
                } catch (NumberFormatException e) {
                    return "ERROR";
                }
            }
            return "ERROR_NO_ARGS";
        });

        registerNetworkCode("MUTE", (String[] args) -> {
            _setVolume(0);
            return "MUTED";
        });
    }

    public void _setVolume(double vol) {
        if (vol < 0) vol = 0;
        if (vol > 100) vol = 100;
        this.targetVolume = vol;
    }

    public double getVolume() {
        return currentVolume;
    }
}