package seokhoon.trade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AutoStockApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoStockApplication.class, args);
    }

}
