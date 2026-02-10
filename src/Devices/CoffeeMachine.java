package Devices;

import java.util.concurrent.TimeUnit;

public class CoffeeMachine extends SHDevice {
  public enum Status {
    IDLE, READY, GRINDING
  };

  private Status status = Status.IDLE; // IDLE, GRINDING, BREWING, READY
  private double progress = 0.0; // 0% - 100% postępu parzenia

  public CoffeeMachine() {
    this.status = Status.IDLE;

    registerNetworkCode("GET_STATUS", "STRING", () -> status.toString());
    registerNetworkCode("GET_PROGRESS", "FLOAT", () -> String.valueOf(getProgress()));

    registerNetworkCode("COFFEE", "NULL", (String[] args) -> {
      startProcess();
    });
  }

  public void startProcess() {
    if (this.status != Status.IDLE)
      return;

    this.status = Status.GRINDING;
    this.progress = 0;
    
    Thread process = new Thread(this::loop);
    process.start();
  }

  public double getProgress() {
    return this.progress;
  }

  public String getStatus() {
    return status + " (" + (int) (getProgress() * 100) + "%)";
  }
  
  public void takeCoffee() {
    if (this.status == Status.READY) {
      this.status = Status.IDLE;
      this.progress = 0;
    }
  }

  private void loop() {
    try {
      TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    progress += 0.1;
    if (progress >= 1) {
      status = Status.READY;
      progress = 1;
      return;
    }
    loop();
  }
}
