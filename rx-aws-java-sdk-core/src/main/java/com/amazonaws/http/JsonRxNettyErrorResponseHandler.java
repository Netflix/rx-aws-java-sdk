/*
 * Copyright 2011-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonServiceException.ErrorType;
import com.amazonaws.transform.JsonErrorUnmarshaller;
import com.amazonaws.util.json.JSONObject;

import rx.Observable;
import io.netty.buffer.ByteBuf;
import iep.io.reactivex.netty.protocol.http.client.HttpClientResponse;

public class JsonRxNettyErrorResponseHandler implements RxNettyResponseHandler<AmazonServiceException> {

    /**
     * Services using AWS JSON 1.1 protocol with HTTP binding send the error
     * type information in the response headers, instead of the content.
     */
    private static final String X_AMZN_ERROR_TYPE = "x-amzn-ErrorType";

    /**
     * The list of error response unmarshallers to try to apply to error
     * responses.
     */
    private List<? extends JsonErrorUnmarshaller> unmarshallerList;
    private String serviceName;

    public JsonRxNettyErrorResponseHandler(String serviceName, List<? extends JsonErrorUnmarshaller> exceptionUnmarshallers) {
        this.serviceName = serviceName;
        this.unmarshallerList = exceptionUnmarshallers;
    }

    public AmazonServiceException handle(HttpResponse response) throws Exception {
      throw new UnsupportedOperationException("amazon client not supported");
    }

    public Observable<AmazonServiceException> handle(HttpClientResponse<ByteBuf> response) throws Exception {
      return response.getContent().reduce(
        new ByteArrayOutputStream(),
        (out, bb) -> {
          try {
            bb.readBytes(out, bb.readableBytes());
            return out;
          }
          catch (java.io.IOException e) {
            throw new RuntimeException(e);
          }
        }
      )
      .map(out -> {
        JSONObject jsonErrorMessage;
        try {
          String s = new String(out.toByteArray());
          if (s.length() == 0 || s.trim().length() == 0) s = "{}";
          jsonErrorMessage = new JSONObject(s);
        } catch (Exception e) {
          throw new AmazonClientException("Unable to parse error response", e);
        }

        String errorTypeFromHeader = parseErrorTypeFromHeader(response);

        AmazonServiceException ase = runErrorUnmarshallers(response, jsonErrorMessage, errorTypeFromHeader);
        if (ase == null) return null;

        ase.setServiceName(serviceName);
        ase.setStatusCode(response.getStatus().code());
        if (response.getStatus().code() < 500) {
          ase.setErrorType(ErrorType.Client);
        } else {
          ase.setErrorType(ErrorType.Service);
        }

        ase.setRequestId(response.getHeaders().get("X-Amzn-RequestId"));

        return ase;
      });
    }

    private AmazonServiceException runErrorUnmarshallers(HttpClientResponse<ByteBuf> errorResponse, JSONObject json, String errorTypeFromHeader) {
        /*
         * We need to select which exception unmarshaller is the correct one to
         * use from all the possible exceptions this operation can throw.
         * Currently we rely on JsonErrorUnmarshaller.match(...) method which
         * checks for the error type parsed either from response headers or the
         * content.
         */
      try {
        for (JsonErrorUnmarshaller unmarshaller : unmarshallerList) {
            if (unmarshaller.match(errorTypeFromHeader, json)) {
                AmazonServiceException ase = unmarshaller.unmarshall(json);
                ase.setStatusCode(errorResponse.getStatus().code());
                return ase;
            }
        }

        return null;
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public boolean needsConnectionLeftOpen() {
        return false;
    }

    /**
     * Attempt to parse the error type from the response headers.
     * Returns null if such information is not available in the header.
     */
    private String parseErrorTypeFromHeader(HttpClientResponse<ByteBuf> response) {
        String headerValue = response.getHeaders().get(X_AMZN_ERROR_TYPE);
        if (headerValue != null) {
            int separator = headerValue.indexOf(':');
            if (separator != -1) {
                headerValue  = headerValue.substring(0, separator);
            }
        }
        return headerValue;
    }
}
