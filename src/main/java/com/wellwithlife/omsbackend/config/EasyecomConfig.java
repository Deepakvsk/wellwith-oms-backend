package com.wellwithlife.omsbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class EasyecomConfig {

    @Value("${easyecom.api.base-url:https://api.easyecom.io}")
    private String baseUrl;

    @Value("${easyecom.api.token:YOUR_EASYECOM_API_TOKEN}")
    private String apiToken;

    @Bean
    public WebClient easyecomWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Authorization", "Bearer " + apiToken)
                .build();
    }
}