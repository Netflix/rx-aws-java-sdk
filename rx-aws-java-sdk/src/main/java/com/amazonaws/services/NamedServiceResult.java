package com.amazonaws.services;

public class NamedServiceResult<R> extends ServiceResult<R> {
  public final String name;

  public NamedServiceResult(long startTime, String name, R result) {
    super(startTime, result);
    this.name = name;
  }
}
