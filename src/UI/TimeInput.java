package UI;

import java.awt.Component;
import java.awt.FlowLayout;
import java.time.LocalTime;

import javax.swing.*;

public class TimeInput extends JComponent {
  JSpinner hour = new JSpinner(new SpinnerNumberModel(0, 0, 23, 1));
  JSpinner minute = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
  JSpinner second = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));

  public TimeInput() {
    setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
    add(hour);
    add(new JLabel(":"));
    add(minute);
    add(new JLabel(":"));
    add(second);
  }

  public LocalTime getTime() {
    return LocalTime.of(
        (int) hour.getValue(),
        (int) minute.getValue(),
        (int) second.getValue());
  }

  public void setTime(LocalTime time) {
    hour.setValue(time.getHour());
    minute.setValue(time.getMinute());
    second.setValue(time.getSecond());
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);

    hour.setEnabled(enabled);
    minute.setEnabled(enabled);
    second.setEnabled(enabled);

    for (Component c : getComponents()) {
      c.setEnabled(enabled);
    }
  }
};
