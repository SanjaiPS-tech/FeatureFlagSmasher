package com.featureflaglite.featureflagsmasher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class FeartureFlagSmasherApplication {

	public static void main(String[] args) {
		SpringApplication.run(FeartureFlagSmasherApplication.class, args);
	}

}
