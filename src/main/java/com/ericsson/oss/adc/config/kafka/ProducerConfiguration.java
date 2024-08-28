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


import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ProducerConfiguration {

    public static final String PRODUCER_CONFIG = "producerConfig";

    private static final String TRUE = "true";

    @Value("${dmm.schema-registry.url}")
    private String schemaRegistryUrl;

    @Value("${spring.kafka.topics.output.compression-type}")
    private String compressionTypeConfig;

    @Value("${spring.kafka.topics.output.acks}")
    private String acksConfig;

    @Value("${spring.kafka.topics.output.batch-size}")
    private String batchSizeConfig;

    @Value("${spring.kafka.topics.output.buffer-memory}")
    private String bufferMemoryConfig;

    @Value("${spring.kafka.topics.output.max-request-size}")
    private String maxRequestSizeConfig;

    @Value("${spring.kafka.topics.output.linger}")
    private String lingerConfig;

    // robustness values start
    @Value("${spring.kafka.producer.retry.backoff.ms}")
    private int producerRetryBackoffMs;

    @Value("${spring.kafka.producer.reconnect.backoff.ms}")
    private int producerReconnectBackoffMs;

    @Value("${spring.kafka.producer.reconnect.backoff.max.ms}")
    private int producerReconnectBackoffMaxMs;

    @Value("${spring.kafka.producer.request.timeout.ms}")
    private int producerRequestTimeoutMs;

    // robustness values end

    @Bean
    public Map<String, Object> producerConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionTypeConfig);
        config.put(ProducerConfig.ACKS_CONFIG, acksConfig);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSizeConfig);
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemoryConfig);
        config.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, maxRequestSizeConfig);
        config.put(ProducerConfig.LINGER_MS_CONFIG, lingerConfig);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, TRUE);

        addProducerRobustnessValues(config);
        return config;
    }

    /**
     * Robustness/Resiliency values for consumer read from configmap/values file.
     *
     * @param config
     *            Kafka Config {@link Map}
     */
    private void addProducerRobustnessValues(final Map<String, Object> config) {
        config.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, producerRetryBackoffMs);
        config.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, producerReconnectBackoffMs);
        config.put(ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, producerReconnectBackoffMaxMs);
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, producerRequestTimeoutMs);
    }
}
