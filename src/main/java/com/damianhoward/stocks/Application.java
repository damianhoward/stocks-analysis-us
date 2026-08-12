package com.damianhoward.stocks;

import com.damianhoward.stocks.event.Event;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Configuration
@SpringBootApplication
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        String zacksDate = System.getProperty("zacksDate");
        String event = System.getProperty("event");
        String dateAsString = System.getProperty("date");

        log.info("System parameters zacksDate={}, event={}, date={}", zacksDate, event, dateAsString);

        LocalDate date;
        if (!StringUtils.isEmpty(dateAsString)) {
            date = LocalDate.parse(dateAsString);
        } else {
            date = LocalDate.now();
        }

        // Fails fast on a typo'd -Devent, before the Spring context spends time starting.
        Event startEvent = StartEventResolver.resolve(event, date);

        log.info("Starting spring");
        ApplicationContext context = SpringApplication.run(Application.class, args);
        log.info("Completed starting spring");

        log.info("Publishing event {} ", startEvent);

        context.publishEvent(startEvent);
        log.info("Completed publishing event {} ", startEvent);

        System.exit(0);
    }

}
