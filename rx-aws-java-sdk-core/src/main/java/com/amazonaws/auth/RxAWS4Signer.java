/*
 * Copyright 2014-2016 Netflix, Inc.
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
package com.amazonaws.auth;

import static com.amazonaws.auth.internal.SignerConstants.AUTHORIZATION;
import static com.amazonaws.auth.internal.SignerConstants.AWS4_SIGNING_ALGORITHM;
import static com.amazonaws.auth.internal.SignerConstants.AWS4_TERMINATOR;
import static com.amazonaws.auth.internal.SignerConstants.HOST;
import static com.amazonaws.auth.internal.SignerConstants.LINE_SEPARATOR;
import static com.amazonaws.auth.internal.SignerConstants.PRESIGN_URL_MAX_EXPIRATION_SECONDS;
import static com.amazonaws.auth.internal.SignerConstants.X_AMZ_ALGORITHM;
import static com.amazonaws.auth.internal.SignerConstants.X_AMZ_CONTENT_SHA256;
import static com.amazonaws.auth.internal.SignerConstants.X_AMZ_CREDENTIAL;
import static com.amazonaws.auth.internal.SignerConstants.X_AMZ_DATE;
import static com.amazonaws.auth.internal.SignerConstants.X_AMZ_EXPIRES;
import static com.amazonaws.auth.internal.SignerConstants.X_AMZ_SECURITY_TOKEN;
import static com.amazonaws.auth.internal.SignerConstants.X_AMZ_SIGNATURE;
import static com.amazonaws.auth.internal.SignerConstants.X_AMZ_SIGNED_HEADER;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ReadLimitInfo;
import com.amazonaws.SignableRequest;
import com.amazonaws.auth.internal.AWS4SignerRequestParams;
import com.amazonaws.auth.internal.AWS4SignerUtils;
import com.amazonaws.auth.internal.SignerKey;
import com.amazonaws.internal.FIFOCache;
import com.amazonaws.log.InternalLogApi;
import com.amazonaws.log.InternalLogFactory;
import com.amazonaws.util.BinaryUtils;
import com.amazonaws.util.DateUtils;
import com.amazonaws.util.RxSdkHttpUtils;

public class RxAWS4Signer extends RxAbstractAWSSigner implements
        ServiceAwareSigner, RegionAwareSigner, Presigner {

    protected static final InternalLogApi log = InternalLogFactory.getLog(RxAWS4Signer.class);
    private static final int SIGNER_CACHE_MAX_SIZE = 300;
    private static final FIFOCache<SignerKey> signerCache = new FIFOCache<SignerKey>(SIGNER_CACHE_MAX_SIZE);

    protected String serviceName;

    protected String regionName;

    protected Date overriddenDate;

    protected boolean doubleUrlEncode;

    public RxAWS4Signer() {
        this(true);
    }

    public RxAWS4Signer(boolean doubleUrlEncoding) {
        this.doubleUrlEncode = doubleUrlEncoding;
    }

    @Override
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    void setOverrideDate(Date overriddenDate) {
        this.overriddenDate = overriddenDate;
    }

    public String getRegionName() {
        return regionName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Date getOverriddenDate() {
        return overriddenDate == null ? null : new Date(
                overriddenDate.getTime());
    }

    @Override
    public void sign(SignableRequest<?> request, AWSCredentials credentials) {
        if (isAnonymous(credentials)) {
            return;
        }

        AWSCredentials sanitizedCredentials = sanitizeCredentials(credentials);
        if (sanitizedCredentials instanceof AWSSessionCredentials) {
            addSessionCredentials(request,
                    (AWSSessionCredentials) sanitizedCredentials);
        }

        final AWS4SignerRequestParams signerParams = new AWS4SignerRequestParams(
                request, overriddenDate, regionName, serviceName,
                AWS4_SIGNING_ALGORITHM);

        addHostHeader(request);
        request.addHeader(X_AMZ_DATE,
                signerParams.getFormattedSigningDateTime());

        String contentSha256 = calculateContentHash(request);

        if ("required".equals(request.getHeaders().get(X_AMZ_CONTENT_SHA256))) {
            request.addHeader(X_AMZ_CONTENT_SHA256, contentSha256);
        }

        final String canonicalRequest = createCanonicalRequest(request,
                contentSha256);

        final String stringToSign = createStringToSign(canonicalRequest,
                signerParams);

        final byte[] signingKey = deriveSigningKey(sanitizedCredentials,
                signerParams);

        final byte[] signature = computeSignature(stringToSign, signingKey,
                signerParams);

        request.addHeader(
                AUTHORIZATION,
                buildAuthorizationHeader(request, signature,
                        sanitizedCredentials, signerParams));

        processRequestPayload(request, signature, signingKey,
                signerParams);
    }

    @Override
    public void presignRequest(SignableRequest<?> request, AWSCredentials credentials,
            Date userSpecifiedExpirationDate) {

        if (isAnonymous(credentials)) {
            return;
        }

        long expirationInSeconds = generateExpirationDate(userSpecifiedExpirationDate);

        addHostHeader(request);

        AWSCredentials sanitizedCredentials = sanitizeCredentials(credentials);
        if (sanitizedCredentials instanceof AWSSessionCredentials) {
            request.addParameter(X_AMZ_SECURITY_TOKEN,
                    ((AWSSessionCredentials) sanitizedCredentials)
                            .getSessionToken());
        }

        final AWS4SignerRequestParams signerRequestParams = new AWS4SignerRequestParams(
                request, overriddenDate, regionName, serviceName,
                AWS4_SIGNING_ALGORITHM);

        final String timeStamp = AWS4SignerUtils.formatTimestamp(System
                .currentTimeMillis());

        addPreSignInformationToRequest(request, sanitizedCredentials,
                signerRequestParams, timeStamp, expirationInSeconds);

        final String contentSha256 = calculateContentHashPresign(request);

        final String canonicalRequest = createCanonicalRequest(request,
                contentSha256);

        final String stringToSign = createStringToSign(canonicalRequest,
                signerRequestParams);

        final byte[] signingKey = deriveSigningKey(sanitizedCredentials,
                signerRequestParams);

        final byte[] signature = computeSignature(stringToSign, signingKey,
                signerRequestParams);

        request.addParameter(X_AMZ_SIGNATURE, BinaryUtils.toHex(signature));
    }

    protected String createCanonicalRequest(SignableRequest<?> request,
            String contentSha256) {
        final String path = RxSdkHttpUtils.appendUri(
                request.getEndpoint().getPath(), request.getResourcePath());

        final StringBuilder canonicalRequestBuilder = new StringBuilder(request
                .getHttpMethod().toString());

        canonicalRequestBuilder.append(LINE_SEPARATOR)
                .append(getCanonicalizedResourcePath(path, doubleUrlEncode))
                .append(LINE_SEPARATOR)
                .append(getCanonicalizedQueryString(request))
                .append(LINE_SEPARATOR)
                .append(getCanonicalizedHeaderString(request))
                .append(LINE_SEPARATOR)
                .append(getSignedHeadersString(request)).append(LINE_SEPARATOR)
                .append(contentSha256);

        final String canonicalRequest = canonicalRequestBuilder.toString();

        if (log.isDebugEnabled())
            log.debug("AWS4 Canonical Request: '\"" + canonicalRequest + "\"");

        return canonicalRequest;
    }

    protected String createStringToSign(String canonicalRequest,
            AWS4SignerRequestParams signerParams) {

        final StringBuilder stringToSignBuilder = new StringBuilder(
                signerParams.getSigningAlgorithm());
        stringToSignBuilder.append(LINE_SEPARATOR)
                .append(signerParams.getFormattedSigningDateTime())
                .append(LINE_SEPARATOR)
                .append(signerParams.getScope())
                .append(LINE_SEPARATOR)
                .append(BinaryUtils.toHex(hash(canonicalRequest)));

        final String stringToSign = stringToSignBuilder.toString();

        if (log.isDebugEnabled())
            log.debug("AWS4 String to Sign: '\"" + stringToSign + "\"");

        return stringToSign;
    }

    private final byte[] deriveSigningKey(AWSCredentials credentials,
            AWS4SignerRequestParams signerRequestParams) {

        final String cacheKey = computeSigningCacheKeyName(credentials,
                signerRequestParams);
        final long daysSinceEpochSigningDate = DateUtils
                .numberOfDaysSinceEpoch(signerRequestParams
                        .getSigningDateTimeMilli());

        SignerKey signerKey = signerCache.get(cacheKey);

        if (signerKey != null) {
            if (daysSinceEpochSigningDate == signerKey
                    .getNumberOfDaysSinceEpoch()) {
                return signerKey.getSigningKey();
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Generating a new signing key as the signing key not available in the cache for the date "
                    + TimeUnit.DAYS.toMillis(daysSinceEpochSigningDate));
        }
        byte[] signingKey = newSigningKey(credentials,
                signerRequestParams.getFormattedSigningDate(),
                signerRequestParams.getRegionName(),
                signerRequestParams.getServiceName());
        signerCache.add(cacheKey, new SignerKey(
                daysSinceEpochSigningDate, signingKey));
        return signingKey;
    }

    private final String computeSigningCacheKeyName(AWSCredentials credentials,
            AWS4SignerRequestParams signerRequestParams) {
        final StringBuilder hashKeyBuilder = new StringBuilder(
                credentials.getAWSSecretKey());

        return hashKeyBuilder.append("-")
                .append(signerRequestParams.getRegionName())
                .append("-")
                .append(signerRequestParams.getServiceName()).toString();
    }

    protected final byte[] computeSignature(String stringToSign,
            byte[] signingKey, AWS4SignerRequestParams signerRequestParams) {
        return sign(stringToSign.getBytes(Charset.forName("UTF-8")), signingKey,
                SigningAlgorithm.HmacSHA256);
    }

    private String buildAuthorizationHeader(SignableRequest<?> request,
            byte[] signature, AWSCredentials credentials,
            AWS4SignerRequestParams signerParams) {
        final String signingCredentials = credentials.getAWSAccessKeyId() + "/"
                + signerParams.getScope();

        final String credential = "Credential="
                + signingCredentials;
        final String signerHeaders = "SignedHeaders="
                + getSignedHeadersString(request);
        final String signatureHeader = "Signature="
                + BinaryUtils.toHex(signature);

        final StringBuilder authHeaderBuilder = new StringBuilder();

        authHeaderBuilder.append(AWS4_SIGNING_ALGORITHM)
                         .append(" ")
                         .append(credential)
                         .append(", ")
                         .append(signerHeaders)
                         .append(", ")
                         .append(signatureHeader);

        return authHeaderBuilder.toString();
    }

    private void addPreSignInformationToRequest(SignableRequest<?> request,
            AWSCredentials credentials, AWS4SignerRequestParams signerParams,
            String timeStamp, long expirationInSeconds) {

        String signingCredentials = credentials.getAWSAccessKeyId() + "/"
                + signerParams.getScope();

        request.addParameter(X_AMZ_ALGORITHM, AWS4_SIGNING_ALGORITHM);
        request.addParameter(X_AMZ_DATE, timeStamp);
        request.addParameter(X_AMZ_SIGNED_HEADER,
                getSignedHeadersString(request));
        request.addParameter(X_AMZ_EXPIRES,
                Long.toString(expirationInSeconds));
        request.addParameter(X_AMZ_CREDENTIAL, signingCredentials);
    }

    @Override
    protected void addSessionCredentials(SignableRequest<?> request,
            AWSSessionCredentials credentials) {
        request.addHeader(X_AMZ_SECURITY_TOKEN, credentials.getSessionToken());
    }

    protected String getCanonicalizedHeaderString(SignableRequest<?> request) {
        final List<String> sortedHeaders = new ArrayList<String>(request.getHeaders()
                .keySet());
        Collections.sort(sortedHeaders, String.CASE_INSENSITIVE_ORDER);

        final Map<String, String> requestHeaders = request.getHeaders();
        StringBuilder buffer = new StringBuilder();
        for (String header : sortedHeaders) {
            String key = header.toLowerCase().replaceAll("\\s+", " ");
            String value = requestHeaders.get(header);

            buffer.append(key).append(":");
            if (value != null) {
                buffer.append(value.replaceAll("\\s+", " "));
            }

            buffer.append("\n");
        }

        return buffer.toString();
    }

    protected String getSignedHeadersString(SignableRequest<?> request) {
        final List<String> sortedHeaders = new ArrayList<String>(request
                .getHeaders().keySet());
        Collections.sort(sortedHeaders, String.CASE_INSENSITIVE_ORDER);

        StringBuilder buffer = new StringBuilder();
        for (String header : sortedHeaders) {
            if (buffer.length() > 0)
                buffer.append(";");
            buffer.append(header.toLowerCase());
        }

        return buffer.toString();
    }

    protected void addHostHeader(SignableRequest<?> request) {

        final URI endpoint = request.getEndpoint();
        final StringBuilder hostHeaderBuilder = new StringBuilder(
                endpoint.getHost());
        if (RxSdkHttpUtils.isUsingNonDefaultPort(endpoint)) {
            hostHeaderBuilder.append(":").append(endpoint.getPort());
        }

        request.addHeader(HOST, hostHeaderBuilder.toString());
    }

    protected String calculateContentHash(SignableRequest<?> request) {
        InputStream payloadStream = getBinaryRequestPayloadStream(request);
        ReadLimitInfo info = request.getReadLimitInfo();
        payloadStream.mark(info == null ? -1 : info.getReadLimit());
        String contentSha256 = BinaryUtils.toHex(hash(payloadStream));
        try {
            payloadStream.reset();
        } catch (IOException e) {
            throw new AmazonClientException(
                    "Unable to reset stream after calculating AWS4 signature",
                    e);
        }
        return contentSha256;
    }

    protected void processRequestPayload(SignableRequest<?> request, byte[] signature,
            byte[] signingKey, AWS4SignerRequestParams signerRequestParams) {
        return;
    }

    protected String calculateContentHashPresign(SignableRequest<?> request) {
        return calculateContentHash(request);
    }

    private boolean isAnonymous(AWSCredentials credentials) {
        return credentials instanceof AnonymousAWSCredentials;
    }

    private long generateExpirationDate(Date expirationDate) {

        long expirationInSeconds = expirationDate != null ? ((expirationDate
                .getTime() - System.currentTimeMillis()) / 1000L)
                : PRESIGN_URL_MAX_EXPIRATION_SECONDS;

        if (expirationInSeconds > PRESIGN_URL_MAX_EXPIRATION_SECONDS) {
            throw new AmazonClientException(
                    "Requests that are pre-signed by SigV4 algorithm are valid for at most 7 days. "
                            + "The expiration date set on the current request ["
                            + AWS4SignerUtils.formatTimestamp(expirationDate
                                    .getTime()) + "] has exceeded this limit.");
        }
        return expirationInSeconds;
    }

    private byte[] newSigningKey(AWSCredentials credentials,
            String dateStamp, String regionName, String serviceName) {
        byte[] kSecret = ("AWS4" + credentials.getAWSSecretKey())
                .getBytes(Charset.forName("UTF-8"));
        byte[] kDate = sign(dateStamp, kSecret, SigningAlgorithm.HmacSHA256);
        byte[] kRegion = sign(regionName, kDate, SigningAlgorithm.HmacSHA256);
        byte[] kService = sign(serviceName, kRegion,
                SigningAlgorithm.HmacSHA256);
        return sign(AWS4_TERMINATOR, kService, SigningAlgorithm.HmacSHA256);
    }
}
