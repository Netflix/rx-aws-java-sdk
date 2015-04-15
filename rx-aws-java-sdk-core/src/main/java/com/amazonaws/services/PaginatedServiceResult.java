package com.amazonaws.services;

public class PaginatedServiceResult<R> extends ServiceResult<R> {
  public final String token;

  public PaginatedServiceResult(long startTime, String token, R result) {
    super(startTime, result);
    this.token = token;
  }
}
