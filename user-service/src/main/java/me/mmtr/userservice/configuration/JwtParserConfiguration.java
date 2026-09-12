package me.mmtr.userservice.configuration;

import me.mmtr.common.jwt.JwtParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtParserConfiguration {
    @Bean
    public JwtParser jwtParser(
            @Value("${jwt.secret}") String jwtSecret
    ) {
        return new JwtParser(jwtSecret);
    }
}
