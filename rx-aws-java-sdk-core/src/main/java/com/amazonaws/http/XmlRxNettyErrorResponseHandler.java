/*
 * Copyright 2010-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.transform.Unmarshaller;
import com.amazonaws.util.IOUtils;
import com.amazonaws.util.XpathUtils;
import com.amazonaws.util.RxSchedulers;

import rx.Observable;
import rx.functions.*;
import io.netty.buffer.ByteBuf;
import iep.io.reactivex.netty.protocol.http.client.HttpClientResponse;

/**
 * Implementation of HttpResponseHandler that handles only error responses from
 * Amazon Web Services. A list of unmarshallers is passed into the constructor,
 * and while handling a response, each unmarshaller is tried, in order, until
 * one is found that can successfully unmarshall the error response.  If no
 * unmarshaller is found that can unmarshall the error response, a generic
 * AmazonServiceException is created and populated with the AWS error response
 * information (error message, AWS error code, AWS request ID, etc).
 */
public class XmlRxNettyErrorResponseHandler
        implements RxNettyResponseHandler<AmazonServiceException> {
    private static final Log log = LogFactory.getLog(XmlRxNettyErrorResponseHandler.class);

    /**
     * The list of error response unmarshallers to try to apply to error
     * responses.
     */
    private List<Unmarshaller<AmazonServiceException, Node>> unmarshallerList;

    /**
     * Constructs a new XmlRxNettyErrorResponseHandler that will handle error
     * responses from Amazon services using the specified list of unmarshallers.
     * Each unmarshaller will be tried, in order, until one is found that can
     * unmarshall the error response.
     *
     * @param unmarshallerList
     *            The list of unmarshallers to try using when handling an error
     *            response.
     */
    public XmlRxNettyErrorResponseHandler(
            List<Unmarshaller<AmazonServiceException, Node>> unmarshallerList) {
        this.unmarshallerList = unmarshallerList;
    }

    @Override
    public AmazonServiceException handle(HttpResponse response) throws Exception {
        throw new UnsupportedOperationException("appache response not supported");
    }

    @Override
    public Observable<AmazonServiceException> handle(HttpClientResponse<ByteBuf> response) throws Exception {
        // Try to read the error response
      return response.getContent().reduce(
        new ByteArrayOutputStream(),
        new Func2<ByteArrayOutputStream,ByteBuf,ByteArrayOutputStream>() {
          @Override
          public ByteArrayOutputStream call(ByteArrayOutputStream out, ByteBuf bb) {
            try {
              bb.readBytes(out, bb.readableBytes());
              return out;
            }
            catch (java.io.IOException e) {
              throw newAmazonServiceException("Unable to unmarshall error response", response, e);
            }
          }
        }
      )
      .observeOn(RxSchedulers.computation())
      .map(new Func1<ByteArrayOutputStream,Document>() {
        @Override
        public Document call(ByteArrayOutputStream out) {
          String content = new String(out.toByteArray());
          try {
            return XpathUtils.documentFrom(content);
          }
          catch (Exception e) {
            throw newAmazonServiceException(
              String.format("Unable to unmarshall error response (%s)", content), response, e
            );
          }
        }
      })
      .map(new Func1<Document,AmazonServiceException>() {
        @Override
        public AmazonServiceException call(Document document) {
          for (Unmarshaller<AmazonServiceException, Node> unmarshaller : unmarshallerList) {
              try {
                AmazonServiceException ase = unmarshaller.unmarshall(document);
                if (ase != null) {
                    ase.setStatusCode(response.getStatus().code());
                    return ase;
                }
              }
              catch (Exception e) {}
          }
          throw new AmazonClientException("Unable to unmarshall error response from service");
        }
      })
      .onErrorResumeNext(new Func1<Throwable,Observable<AmazonServiceException>>() {
        @Override
        public Observable<AmazonServiceException> call(Throwable t) {
          if (t instanceof AmazonServiceException) return Observable.just((AmazonServiceException)t);
          else return Observable.error(t);
        }
      });
    }

    /**
     * Used to create an {@link newAmazonServiceException} when we failed to
     * read the error response or parsed the error response as XML.
     */
    private AmazonServiceException newAmazonServiceException(
      String errmsg,
      HttpClientResponse<ByteBuf> httpResponse,
      Exception readFailure
    ) {
        AmazonServiceException exception = new AmazonServiceException(errmsg, readFailure);
            final int statusCode = httpResponse.getStatus().code();
            exception.setErrorCode(statusCode + " " + httpResponse.getStatus().reasonPhrase());
            exception.setErrorType(AmazonServiceException.ErrorType.Unknown);
            exception.setStatusCode(statusCode);
            return exception;
    }
    /**
     * Since this response handler completely consumes all the data from the
     * underlying HTTP connection during the handle method, we don't need to
     * keep the HTTP connection open.
     *
     * @see com.amazonaws.http.HttpResponseHandler#needsConnectionLeftOpen()
     */
    public boolean needsConnectionLeftOpen() {
        return false;
    }

}
