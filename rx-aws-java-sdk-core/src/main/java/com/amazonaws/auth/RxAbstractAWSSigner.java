package com.amazonaws.auth;

import static com.amazonaws.util.StringUtils.UTF8;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ReadLimitInfo;
import com.amazonaws.SDKGlobalTime;
import com.amazonaws.SignableRequest;
import com.amazonaws.internal.SdkDigestInputStream;
import com.amazonaws.util.Base64;
import com.amazonaws.util.BinaryUtils;
import com.amazonaws.util.RxSdkHttpUtils;

public abstract class RxAbstractAWSSigner implements Signer {
    public static final String EMPTY_STRING_SHA256_HEX;

    static {
        EMPTY_STRING_SHA256_HEX = BinaryUtils.toHex(doHash(""));
    }

    protected String signAndBase64Encode(String data, String key,
            SigningAlgorithm algorithm) throws AmazonClientException {
        return signAndBase64Encode(data.getBytes(UTF8), key, algorithm);
    }

    protected String signAndBase64Encode(byte[] data, String key,
            SigningAlgorithm algorithm) throws AmazonClientException {
        try {
            byte[] signature = sign(data, key.getBytes(UTF8), algorithm);
            return Base64.encodeAsString(signature);
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Unable to calculate a request signature: "
                            + e.getMessage(), e);
        }
    }

    public byte[] sign(String stringData, byte[] key,
            SigningAlgorithm algorithm) throws AmazonClientException {
        try {
            byte[] data = stringData.getBytes(UTF8);
            return sign(data, key, algorithm);
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Unable to calculate a request signature: "
                            + e.getMessage(), e);
        }
    }

    public byte[] signWithMac(String stringData, Mac mac) {
        try {
            return mac.doFinal(stringData.getBytes(UTF8));
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Unable to calculate a request signature: "
                            + e.getMessage(), e);
        }
    }

    protected byte[] sign(byte[] data, byte[] key,
            SigningAlgorithm algorithm) throws AmazonClientException {
        try {
            Mac mac = Mac.getInstance(algorithm.toString());
            mac.init(new SecretKeySpec(key, algorithm.toString()));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Unable to calculate a request signature: "
                            + e.getMessage(), e);
        }
    }

    public byte[] hash(String text) throws AmazonClientException {
        return RxAbstractAWSSigner.doHash(text);
    }

    private static byte[] doHash(String text) throws AmazonClientException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(text.getBytes(UTF8));
            return md.digest();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Unable to compute hash while signing request: "
                            + e.getMessage(), e);
        }
    }

    protected byte[] hash(InputStream input) throws AmazonClientException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            @SuppressWarnings("resource")
            DigestInputStream digestInputStream = new SdkDigestInputStream(
                    input, md);
            byte[] buffer = new byte[1024];
            while (digestInputStream.read(buffer) > -1)
                ;
            return digestInputStream.getMessageDigest().digest();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Unable to compute hash while signing request: "
                            + e.getMessage(), e);
        }
    }

    public byte[] hash(byte[] data) throws AmazonClientException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(data);
            return md.digest();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Unable to compute hash while signing request: "
                            + e.getMessage(), e);
        }
    }
    protected String getCanonicalizedQueryString(Map<String,List<String>> parameters) {

        final SortedMap<String, List<String>> sorted = new TreeMap<String, List<String>>();

        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            final String encodedParamName = RxSdkHttpUtils.urlEncode(
                    entry.getKey(), false);
            final List<String> paramValues = entry.getValue();
            final List<String> encodedValues = new ArrayList<String>(
                    paramValues.size());
            for (String value : paramValues) {
                encodedValues.add(RxSdkHttpUtils.urlEncode(value, false));
            }
            Collections.sort(encodedValues);
            sorted.put(encodedParamName, encodedValues);

        }

        final StringBuilder result = new StringBuilder();
        for(Map.Entry<String, List<String>> entry : sorted.entrySet()) {
            for(String value : entry.getValue()) {
                if (result.length() > 0) {
                    result.append("&");
                }
                result.append(entry.getKey())
                      .append("=")
                      .append(value);
            }
        }

        return result.toString();
    }

    protected String getCanonicalizedQueryString(SignableRequest<?> request) {
        if (RxSdkHttpUtils.usePayloadForQueryParameters(request))
            return "";
        return this.getCanonicalizedQueryString(request.getParameters());
    }

    protected byte[] getBinaryRequestPayload(SignableRequest<?> request) {
        if (RxSdkHttpUtils.usePayloadForQueryParameters(request)) {
            String encodedParameters = RxSdkHttpUtils.encodeParameters(request);
            if (encodedParameters == null)
                return new byte[0];

            return encodedParameters.getBytes(UTF8);
        }

        return getBinaryRequestPayloadWithoutQueryParams(request);
    }

    protected String getRequestPayload(SignableRequest<?> request) {
        return newString(getBinaryRequestPayload(request));
    }

    protected String getRequestPayloadWithoutQueryParams(SignableRequest<?> request) {
        return newString(getBinaryRequestPayloadWithoutQueryParams(request));
    }

    protected byte[] getBinaryRequestPayloadWithoutQueryParams(SignableRequest<?> request) {
        InputStream content = getBinaryRequestPayloadStreamWithoutQueryParams(request);

        try {
            ReadLimitInfo info = request.getReadLimitInfo();
            content.mark(info == null ? -1 : info.getReadLimit());
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024 * 5];
            while (true) {
                int bytesRead = content.read(buffer);
                if (bytesRead == -1) break;

                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            byteArrayOutputStream.close();
            content.reset();

            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new AmazonClientException("Unable to read request payload to sign request: " + e.getMessage(), e);
        }
    }

    protected InputStream getBinaryRequestPayloadStream(SignableRequest<?> request) {
        if (RxSdkHttpUtils.usePayloadForQueryParameters(request)) {
            String encodedParameters = RxSdkHttpUtils.encodeParameters(request);
            if (encodedParameters == null)
                return new ByteArrayInputStream(new byte[0]);

            return new ByteArrayInputStream(
                    encodedParameters.getBytes(UTF8));
        }

        return getBinaryRequestPayloadStreamWithoutQueryParams(request);
    }

    protected InputStream getBinaryRequestPayloadStreamWithoutQueryParams(SignableRequest<?> request) {
        try {
            InputStream is = request.getContentUnwrapped();
            if (is == null)
                return new ByteArrayInputStream(new byte[0]);
            if (!is.markSupported())
                throw new AmazonClientException("Unable to read request payload to sign request.");
            return is;
        } catch (AmazonClientException e) {
            throw e;
        } catch (Exception e) {
            throw new AmazonClientException("Unable to read request payload to sign request: " + e.getMessage(), e);
        }
    }

    protected String getCanonicalizedResourcePath(String resourcePath) {
        return getCanonicalizedResourcePath(resourcePath, true);
    }

    protected String getCanonicalizedResourcePath(String resourcePath, boolean urlEncode) {
        if (resourcePath == null || resourcePath.isEmpty()) {
            return "/";
        } else {
            String value = urlEncode ? RxSdkHttpUtils.urlEncode(resourcePath, true) : resourcePath;
            if (value.startsWith("/")) {
                return value;
            } else {
                return "/".concat(value);
            }
        }
    }

    protected String getCanonicalizedEndpoint(URI endpoint) {
        String endpointForStringToSign = endpoint.getHost().toLowerCase();
        if (RxSdkHttpUtils.isUsingNonDefaultPort(endpoint)) {
            endpointForStringToSign += ":" + endpoint.getPort();
        }

        return endpointForStringToSign;
    }

    protected AWSCredentials sanitizeCredentials(AWSCredentials credentials) {
        String accessKeyId = null;
        String secretKey   = null;
        String token = null;
        synchronized (credentials) {
            accessKeyId = credentials.getAWSAccessKeyId();
            secretKey   = credentials.getAWSSecretKey();
            if ( credentials instanceof AWSSessionCredentials ) {
                token = ((AWSSessionCredentials) credentials).getSessionToken();
            }
        }
        if (secretKey != null) secretKey = secretKey.trim();
        if (accessKeyId != null) accessKeyId = accessKeyId.trim();
        if (token != null) token = token.trim();

        if (credentials instanceof AWSSessionCredentials) {
            return new BasicSessionCredentials(accessKeyId, secretKey, token);
        }

        return new BasicAWSCredentials(accessKeyId, secretKey);
    }

    protected String newString(byte[] bytes) {
        return new String(bytes, UTF8);
    }

    protected Date getSignatureDate(int offsetInSeconds) {
        return new Date(System.currentTimeMillis() - offsetInSeconds*1000);
    }

    @Deprecated
    protected int getTimeOffset(SignableRequest<?> request) {
        final int globleOffset = SDKGlobalTime.getGlobalTimeOffset();
        return globleOffset == 0 ? request.getTimeOffset() : globleOffset;
    }

    protected abstract void addSessionCredentials(SignableRequest<?> request,
            AWSSessionCredentials credentials);
}
