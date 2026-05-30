package com.acip.proxy;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;

public record UpstreamResponse(
        HttpStatusCode statusCode,
        String body,
        HttpHeaders headers
) {
}
