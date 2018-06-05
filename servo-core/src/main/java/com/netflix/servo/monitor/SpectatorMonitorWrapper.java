package com.netflix.servo.monitor;

import com.netflix.servo.tag.TagList;

class SpectatorMonitorWrapper<T extends Number>
    extends NumericMonitorWrapper<T> implements SpectatorMonitor {

  SpectatorMonitorWrapper(TagList tags, NumericMonitor<T> monitor) {
    super(tags, monitor);
    if (monitor instanceof SpectatorMonitor) {
      ((SpectatorMonitor) monitor).initializeSpectator(tags);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initializeSpectator(TagList tags) {
  }
}
