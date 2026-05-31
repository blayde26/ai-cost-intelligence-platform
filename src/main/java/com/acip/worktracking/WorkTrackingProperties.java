package com.acip.worktracking;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "acip.work-tracking")
public record WorkTrackingProperties(String provider) {
}
