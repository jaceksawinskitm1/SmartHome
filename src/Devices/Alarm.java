package Devices;

import java.util.concurrent.TimeUnit;

public class Alarm extends SHDevice {
  private int duration = 30;
  private int counter = 0;

  public Alarm() {
    // Network
    registerNetworkCode("GET_DURATION", "INT", () -> String.valueOf(getDuration()));
    registerNetworkCode("SET_DURATION", "INT", (String[] params) -> setDuration(Integer.parseInt(params[0])));

    registerNetworkCode("RING", "NULL", () -> {
      ring();
    });

    registerNetworkCode("GET_STATUS", "STRING", () -> {
      if (counter >= duration)
        return "DONE";
      if (counter > 0)
        return "RINGING";
      return "IDLE";
    });
  }

  public void ring() {
    if (counter > 0)
      return;
    counter = 0;
    Thread th = new Thread(this::loop);
    th.start();
  }

  public void setDuration(int duration) {
    this.duration = duration;
  }

  public int getDuration() {
    return this.duration;
  }

  private void loop() {
    if (counter > duration) {
      counter = 0;
      return;
    }
    try {
      TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    counter++;
    loop();
  }
}
