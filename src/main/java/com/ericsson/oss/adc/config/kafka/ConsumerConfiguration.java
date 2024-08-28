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

import com.ericsson.oss.adc.models.InputMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ConsumerConfiguration {

    public static final String CONSUMER_CONFIG = "consumerConfig";

    @Value("${spring.kafka.consumer.group-id}")
    private String groupIdConsumerConfig;

    @Value("${spring.kafka.consumer.auto.offset.reset}")
    private String autoOffsetReset;

    //Input Topic
    @Value("${spring.kafka.topics.input.session.timeout.ms}")
    private int sessionTimeoutMs;

    @Value("${spring.kafka.topics.input.partition.assignment.strategy}")
    private String partitionAssignmentStrategy;

    @Value("${spring.kafka.topics.input.max.poll.records}")
    private int maxPollRecords;

    @Value("${spring.kafka.topics.input.max.poll.interval.ms}")
    private int maxPollIntervalMs;

    // Consumer robustness values start

    @Value("${spring.kafka.consumer.retry.backoff.ms}")
    private int consumerRetryBackoffMs;

    @Value("${spring.kafka.consumer.reconnect.backoff.ms}")
    private int consumerReconnectBackoffMs;

    @Value("${spring.kafka.consumer.reconnect.backoff.max.ms}")
    private int consumerReconnectBackoffMaxMs;

    @Value("${spring.kafka.consumer.request.timeout.ms}")
    private int consumerRequestTimeoutMs;

    // Consumer robustness values end

    @Bean
    public Map<String, Object> consumerConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupIdConsumerConfig);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        config.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, partitionAssignmentStrategy);
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollIntervalMs);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, InputMessage.class);
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false); // Don't type check the deserialization so hard

        addConsumerRobustnessValues(config);

        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        return config;
    }

    /**
     * Robustness/Resiliency values for consumer read from configmap/values file.
     *
     * @param config Kafka Config {@link Map}
     */
    private void addConsumerRobustnessValues(final Map<String, Object> config) {
        config.put(ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, consumerRetryBackoffMs);
        config.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, consumerReconnectBackoffMs);
        config.put(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, consumerReconnectBackoffMaxMs);
        config.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, consumerRequestTimeoutMs);
    }
}
