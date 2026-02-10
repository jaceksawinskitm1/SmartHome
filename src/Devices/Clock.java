package Devices;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class Clock extends SHDevice {
  private LocalTime time = LocalTime.of(12, 0, 0);

  public Clock() {
    new Thread(this::loop).start();

    // Network
    registerNetworkCode("GET_TIME", "TIME", () -> getTime().format(DateTimeFormatter.ISO_LOCAL_TIME));
  }

  public void _setTime(String timeString) {
    this.time = LocalTime.parse(timeString, DateTimeFormatter.ISO_LOCAL_TIME);
  }

  public void _setTime(LocalTime time) {
    this.time = time;
  }

  public LocalTime getTime() {
    return this.time;
  }

  private void loop() {
    try {
      TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    this.time = this.time.plusSeconds(1);
    loop();
  }
}
