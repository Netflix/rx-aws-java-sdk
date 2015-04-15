package com.amazonaws.services;

public class ServiceResult<R> {
  public final long startTime; // currentMillis
  public final R result;

  public ServiceResult(long startTime, R result) {
    this.startTime = startTime;
    this.result = result;
  }
}
