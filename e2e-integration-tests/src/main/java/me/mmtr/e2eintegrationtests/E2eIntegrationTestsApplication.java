package me.mmtr.e2eintegrationtests;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "me.mmtr")
public class E2eIntegrationTestsApplication {

    static void main(String[] args) {
        SpringApplication.run(E2eIntegrationTestsApplication.class, args);
    }

}
