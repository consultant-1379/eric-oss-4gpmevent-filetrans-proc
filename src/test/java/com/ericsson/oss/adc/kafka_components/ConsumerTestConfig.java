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

package com.ericsson.oss.adc.kafka_components;


import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ConsumerTestConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServerConfig;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupIdConsumerConfig;

    @Value("${spring.kafka.topics.input.partition.assignment.strategy}")
    private String partitionAssignmentStrategy;

    @Value("${spring.kafka.topics.input.session.timeout.ms}")
    private int sessionTimeoutMs;

    @Value("${dmm.schema-registry.url}")
    private String schemaRegistryUrl;



    @Bean
    public ConsumerFactory<String, Object> consumerOutputTestFactory() {

        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServerConfig);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupIdConsumerConfig);
        config.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, partitionAssignmentStrategy);
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put("TEST", StringSerializer.class);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> consumerKafkaListenerOutputTestContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerOutputTestFactory());
        return factory;
    }
}
