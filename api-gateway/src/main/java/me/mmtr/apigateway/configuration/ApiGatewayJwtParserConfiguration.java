package me.mmtr.apigateway.configuration;

import me.mmtr.common.jwt.JwtParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiGatewayJwtParserConfiguration {
    @Bean
    public JwtParser jwtParser(
            @Value("${jwt.secret}") String jwtSecret
    ) {
        return new JwtParser(jwtSecret);
    }
}
