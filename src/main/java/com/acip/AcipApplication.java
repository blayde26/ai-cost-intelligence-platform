package com.acip;

import com.acip.proxy.OpenAiProperties;
import com.acip.jira.JiraProperties;
import com.acip.sourcecontrol.SourceControlProperties;
import com.acip.worktracking.WorkTrackingProperties;
import com.acip.demo.DemoDataProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({OpenAiProperties.class, JiraProperties.class, WorkTrackingProperties.class, DemoDataProperties.class, SourceControlProperties.class})
public class AcipApplication {

    public static void main(String[] args) {
        SpringApplication.run(AcipApplication.class, args);
    }
}
