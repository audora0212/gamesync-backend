package com.example.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class SchedulerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SchedulerApplication.class, args);
	}

    @PostConstruct
    public void initTimezone() {
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
            System.out.println("[TimeZone] Default set to Asia/Seoul");
        } catch (Exception ignored) {}
    }

}
