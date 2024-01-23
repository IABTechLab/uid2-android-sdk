package com.uid2.dev.cstg;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.*;

import java.util.Objects;

public final class HttpClient {
    public static final OkHttpClient RAW_CLIENT = new OkHttpClient();
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public enum HttpMethod {
        GET,
        POST
    }

    public static class HttpException extends Exception {
        private final HttpMethod method;
        private final String url;
        private final int code;
        private final String codeMessage;
        private final String response;

        public HttpException(HttpMethod method, String url, int code, String codeMessage, String response) {
            super(createErrorMessage(method, url, code, codeMessage, response));

            this.method = method;
            this.url = url;
            this.code = code;
            this.codeMessage = codeMessage;
            this.response = response;
        }

        public HttpMethod getMethod() {
            return method;
        }

        public String getUrl() {
            return url;
        }

        public int getCode() {
            return code;
        }

        public String getCodeMessage() {
            return codeMessage;
        }

        public String getResponse() {
            return response;
        }

        public JsonNode getResponseJson() throws Exception {
            return Mapper.OBJECT_MAPPER.readTree(response);
        }

        private static String createErrorMessage(HttpMethod method, String url, int code, String message, String response) {
//            return "Unsuccessful %s request - URL: %s - Code: %d %s - Response body: %s".formatted(
//                    method, url, code, message, response
//                        );
            return "Unsuccessful "+method.toString()+" request - URL: "+ url+"- Code: "+code+" "+message+" - Response body: "+response;
        }
    }

    private HttpClient() {
    }

    public static String get(String url, String bearerToken) throws Exception {
        Request request = buildRequest(HttpMethod.GET, url, null, bearerToken);
        return execute(request, HttpMethod.GET);
    }

    public static String get(String url) throws Exception {
        return get(url, null);
    }

    public static String post(String url, String body, String bearerToken) throws Exception {
        Request request = buildRequest(HttpMethod.POST, url, body, bearerToken);
        return execute(request, HttpMethod.POST);
    }

    public static String execute(Request request, HttpMethod method) throws Exception {
        try (Response response = RAW_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new HttpException(method, request.url().toString(), response.code(), response.message(), Objects.requireNonNull(response.body()).string());
            }
            return Objects.requireNonNull(response.body()).string();
        }
    }

    private static Request buildRequest(HttpMethod method, String url, String body, String bearerToken) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(url);

        if (bearerToken != null) {
            requestBuilder = requestBuilder.addHeader("Authorization", "Bearer " + bearerToken);
        }

        switch (method) {
            case GET:
                return requestBuilder.get().build();
            case POST:
                return requestBuilder.post(RequestBody.create(body, JSON)).build();
        };
        return null;
    }
}
