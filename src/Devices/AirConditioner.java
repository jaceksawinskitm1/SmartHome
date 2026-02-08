package Devices;

import java.util.Objects;

public class AirConditioner extends SHDevice {
  private boolean isOn = false;
  private double power = 0.0;

  public AirConditioner() {
    this.isOn = false;

    registerNetworkCode("GET_STATE", "BOOL", () -> String.valueOf(isOn));
    registerNetworkCode("SET_STATE", "BOOL", (String[] params) -> {
      this.isOn = params[0].equals("true");
    });
    registerNetworkCode("GET_POWER", "RANGE", () -> String.valueOf(getCoolingPower()));
    registerNetworkCode("SET_POWER", "RANGE", (String[] arg) -> {
      double val = Double.parseDouble(arg[0]);
      setCoolingPower(val);
    });
  }

  public void setCoolingPower(double power) {
    this.power = power;
  }

  public double getCoolingPower() {
    return this.power;
  }
}
