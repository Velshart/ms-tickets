package me.mmtr.userservice;

import org.springframework.boot.SpringApplication;

public class TestUserServiceApplication {

    static void main(String[] args) {
        SpringApplication.from(UserServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
