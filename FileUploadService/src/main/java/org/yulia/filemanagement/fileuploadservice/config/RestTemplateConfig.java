package org.yulia.filemanagement.fileuploadservice.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Configuration
public class RestTemplateConfig {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplateConfig.class);

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(@Value("${metadata.response.timeout.ms}") int timeout) {
        RequestConfig config = RequestConfig.custom()
                .setResponseTimeout(timeout, TimeUnit.MILLISECONDS)
                .build();

        CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(client);

        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public void handleError(@NotNull ClientHttpResponse response) throws IOException {
                HttpStatus statusCode = (HttpStatus) response.getStatusCode();
                if (statusCode.is4xxClientError() || statusCode.is5xxServerError()) {
                    logger.error("Received error response with status code: {}", statusCode);
                } else {
                    super.handleError(response);
                }
            }

            @Override
            public boolean hasError(@NotNull ClientHttpResponse response) throws IOException {
                return response.getStatusCode().isError();
            }
        });

        return restTemplate;
    }
}
