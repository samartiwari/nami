package com.samar.nami;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // enables the periodic word_stats rebuild (WordStatsScheduler)
public class NamiApplication {

	public static void main(String[] args) {
		SpringApplication.run(NamiApplication.class, args);
	}

}
