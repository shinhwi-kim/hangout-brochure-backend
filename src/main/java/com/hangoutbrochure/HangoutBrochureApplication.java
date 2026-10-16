package com.hangoutbrochure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Arrays;

@SpringBootApplication
public class HangoutBrochureApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(HangoutBrochureApplication.class, args);
		boolean oneShotCommand = Arrays.stream(args).anyMatch(arg -> arg.startsWith("--app.command="));
		if (oneShotCommand) {
			SpringApplication.exit(context);
		}
	}

}
