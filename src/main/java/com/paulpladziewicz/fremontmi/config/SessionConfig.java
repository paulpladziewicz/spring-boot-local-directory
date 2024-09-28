package com.paulpladziewicz.fremontmi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.mongo.config.annotation.web.http.EnableMongoHttpSession;

@Configuration
@EnableMongoHttpSession(maxInactiveIntervalInSeconds = 28800) // Sets session timeout to 8 hours
public class SessionConfig {
}
