package io.github.ponker.center.ekuiper.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ponker.center.ekuiper.entity.dto.SqlConfigDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author : Zhen
 */
@Configuration
public class EkuiperConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.create();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public SqlConfigDto sqlConfigDto() {
        return new SqlConfigDto();
    }


}
