/*
 * Copyright 2014-2017 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
