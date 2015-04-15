package com.amazonaws.transform;

import com.fasterxml.jackson.core.JsonParser;

import iep.io.reactivex.netty.protocol.http.client.HttpResponseHeaders;

import com.amazonaws.http.HttpResponse;

import com.amazonaws.transform.JsonUnmarshallerContextImpl;

public class JsonRxNettyUnmarshallerContextImpl extends JsonUnmarshallerContextImpl {
  final HttpResponseHeaders httpHeaders;

  public JsonRxNettyUnmarshallerContextImpl(JsonParser jsonParser, HttpResponseHeaders httpHeaders) {
    super(jsonParser);
    this.httpHeaders = httpHeaders;
  }

  @Override
  public String getHeader(String header) {
    if (httpHeaders == null) return null;
    return httpHeaders.get(header);
  }

  @Override
  public HttpResponse getHttpResponse() {
    throw new UnsupportedOperationException("apache client not supported");
  }
}
