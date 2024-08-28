/*******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.adc.config.kafka;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class AdminConfiguration {

    public static final String ADMIN_CONFIG = "adminConfig";

    @Value("${spring.kafka.admin.retry}")
    private int kafkaRetries;

    @Value("${spring.kafka.admin.retry.backoff.ms}")
    private int retryBackoffMs;

    @Value("${spring.kafka.admin.reconnect.backoff.ms}")
    private int reconnectBackoffMs;

    @Value("${spring.kafka.admin.reconnect.backoff.max.ms}")
    private int reconnectBackoffMaxMs;

    @Value("${spring.kafka.admin.request.timeout.ms}")
    private int requestTimeoutMs;

    // kafka admin robustness values end

    @Bean
    public Map<String, Object> adminConfig() {
        Map<String, Object> config = new HashMap<>();
        config.putAll(addAdminRobustnessValues());
        return config;
    }

    /**
     * Robustness/Resiliency values for {@link org.apache.kafka.clients.admin.AdminClient} read from configmap/values file.
     *
     * Note: It is also possible to get these values from the {@link KafkaAdmin} object.
     *
     * @return The list of properties read from the application.yaml/values file.
     */
    private Map<String, Object> addAdminRobustnessValues() {
        Map<String, Object> configuration = new HashMap<>(5);
        configuration.put(AdminClientConfig.RETRIES_CONFIG, kafkaRetries);
        configuration.put(AdminClientConfig.RETRY_BACKOFF_MS_CONFIG, retryBackoffMs);
        configuration.put(AdminClientConfig.RECONNECT_BACKOFF_MS_CONFIG, reconnectBackoffMs);
        configuration.put(AdminClientConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, reconnectBackoffMaxMs);
        configuration.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMs);
        return configuration;
    }
}
