package Devices;

public class Heater extends SHDevice {
  private boolean isOn = false;
  private double power = 0;

  public Heater() {

    // Network
    registerNetworkCode("GET_STATE", "BOOL", () -> String.valueOf(isOn));

    registerNetworkCode("SET_STATE", "BOOL", (String[] params) -> {
      this.isOn = params[0].equals("true");
    });

    registerNetworkCode("GET_POWER", "RANGE", () -> String.valueOf(power));
    registerNetworkCode("SET_POWER", "RANGE", (String[] params) -> this.setPower(Double.parseDouble(params[0])));
  }

  public double getPower() {
    return power;
  }

  public void setPower(double power) {
    this.power = power;
  }
}
