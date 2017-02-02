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
package com.amazonaws.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonServiceException.ErrorType;
import com.amazonaws.transform.JsonErrorUnmarshallerV2;
//import com.amazonaws.util.json.JSONObject;
import com.amazonaws.util.RxSchedulers;
import com.amazonaws.internal.http.JsonErrorCodeParser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import rx.Observable;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import iep.io.reactivex.netty.protocol.http.client.HttpClientResponse;

public class JsonRxNettyErrorResponseHandlerV2 implements RxNettyResponseHandler<AmazonServiceException> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
                .configure(JsonParser.Feature.ALLOW_COMMENTS, true);

    /**
     * Services using AWS JSON 1.1 protocol with HTTP binding send the error
     * type information in the response headers, instead of the content.
     */

    private final JsonErrorCodeParser errorCodeParser = JsonErrorCodeParser.DEFAULT_ERROR_CODE_PARSER;
    /**
     * The list of error response unmarshallers to try to apply to error
     * responses.
     */
    private List<? extends JsonErrorUnmarshallerV2> unmarshallerList;
    private String serviceName;

    public JsonRxNettyErrorResponseHandlerV2(String serviceName, List<? extends JsonErrorUnmarshallerV2> exceptionUnmarshallers) {
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
          finally {
            ReferenceCountUtil.safeRelease(bb);
          }
        }
      )
      .observeOn(RxSchedulers.computation())
      .map(out -> {
        JsonContent jsonContent;
        try {
          String s = new String(out.toByteArray());
          if (s.length() == 0 || s.trim().length() == 0) s = "{}";
          jsonContent = new JsonContent(s);
        } catch (Exception e) {
          throw new AmazonClientException("Unable to parse error response", e);
        }

        Map<String, String> responseHeaders = new HashMap<String,String>();
        for (String k : response.getHeaders().names()) {
          // TODO: comma seperated?
          responseHeaders.put(k, response.getHeaders().get(k));
        }

        String errorCode = errorCodeParser.parseErrorCode(responseHeaders, jsonContent.jsonNode);
        AmazonServiceException ase = runErrorUnmarshallers(errorCode, jsonContent);

        ase.setErrorCode(errorCode);
        ase.setServiceName(serviceName);
        ase.setStatusCode(response.getStatus().code());
        ase.setRawResponseContent(jsonContent.rawJsonContent);

        if (response.getStatus().code() < 500) {
          ase.setErrorType(ErrorType.Client);
        } else {
          ase.setErrorType(ErrorType.Service);
        }

        ase.setRequestId(response.getHeaders().get("X-Amzn-RequestId"));

        return ase;
      });
    }

    private AmazonServiceException runErrorUnmarshallers(String errorCode, JsonContent jsonContent) {
        /*
         * We need to select which exception unmarshaller is the correct one to
         * use from all the possible exceptions this operation can throw.
         * Currently we rely on JsonErrorUnmarshaller.match(...) method which
         * checks for the error type parsed either from response headers or the
         * content.
         */

      try {
        for (JsonErrorUnmarshallerV2 unmarshaller : unmarshallerList) {
            if (unmarshaller.matchErrorCode(errorCode)) {
                AmazonServiceException ase = unmarshaller.unmarshall(jsonContent.jsonNode);
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

    private static class JsonContent {
        private static final ObjectMapper MAPPER = new ObjectMapper()
                .configure(JsonParser.Feature.ALLOW_COMMENTS, true);

        public final String rawJsonContent;
        public final JsonNode jsonNode;

        private JsonContent(String rawJsonContent) {
            this.rawJsonContent = rawJsonContent;
            this.jsonNode = parseJsonContent();
        }

        private JsonNode parseJsonContent() {
            try {
                return MAPPER.readTree(rawJsonContent);
            } catch (Exception e) {
                //LOG.error("Unable to parse HTTP response content", e);
                return null;
            }
        }

        public boolean isJsonValid() {
            return jsonNode != null;
        }

    }
}
