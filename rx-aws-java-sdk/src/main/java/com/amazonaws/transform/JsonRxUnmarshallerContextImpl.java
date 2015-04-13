package com.amazonaws.transform;

import com.fasterxml.jackson.core.JsonParser;

import iep.io.reactivex.netty.protocol.http.client.HttpResponseHeaders;

import com.amazonaws.http.HttpResponse;

import com.amazonaws.transform.JsonUnmarshallerContextImpl;

public class JsonRxUnmarshallerContextImpl extends JsonUnmarshallerContextImpl {
  final HttpResponseHeaders httpHeaders;

  public JsonRxUnmarshallerContextImpl(JsonParser jsonParser, HttpResponseHeaders httpHeaders) {
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
