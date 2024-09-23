package com.paulpladziewicz.fremontmi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.mongo.config.annotation.web.http.EnableMongoHttpSession;

@Configuration
@EnableMongoHttpSession(maxInactiveIntervalInSeconds = 3600) // Sets session timeout to 1 hour
public class SessionConfig {
}
