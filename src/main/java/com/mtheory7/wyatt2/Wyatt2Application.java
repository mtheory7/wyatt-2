package com.mtheory7.wyatt2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Wyatt2Application {
    private static final Logger logger = LoggerFactory.getLogger(Wyatt2Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Wyatt2Application.class, args);
        logger.debug("Started Wyatt2Application");
    }
}
