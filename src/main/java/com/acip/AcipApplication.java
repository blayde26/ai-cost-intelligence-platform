package com.acip;

import com.acip.proxy.OpenAiProperties;
import com.acip.jira.JiraProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({OpenAiProperties.class, JiraProperties.class})
public class AcipApplication {

    public static void main(String[] args) {
        SpringApplication.run(AcipApplication.class, args);
    }
}
