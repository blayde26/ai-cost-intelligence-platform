package com.acip.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "acip.demo-data")
public record DemoDataProperties(boolean enabled, int usageEventCount) {
}
