package com.amazonaws.http;

import rx.Observable;
import io.netty.buffer.ByteBuf;
import iep.io.reactivex.netty.protocol.http.client.HttpClientResponse;

import com.amazonaws.AmazonWebServiceResponse;

interface RxNettyResponseHandler<T> extends HttpResponseHandler<T> {
  public Observable<T> handle(HttpClientResponse<ByteBuf> response) throws Exception;
}
