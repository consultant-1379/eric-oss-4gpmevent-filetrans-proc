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

package com.ericsson.oss.adc.availability;

import com.ericsson.oss.adc.config.CircuitBreakerConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {DependentServiceAvailabilityKafka.class, KafkaAdmin.class, CircuitBreakerConfig.class})
public class DependentServiceAvailabilityKafkaUnavailableTest {

    @MockBean
    private KafkaAdmin kafkaAdmin;

    @Autowired
    private DependentServiceAvailabilityKafka dependentServiceAvailability;

    @Test
    @DisplayName("When a null bootstrap server is supplied, max retries should be reached and exhausted")
    void test_null_bootstrap_kafka_not_reachable_max_retries_reached() {

        Map<String, Object> properties = new HashMap<>(1);
        properties.put("bootstrap.servers", null);
        when(kafkaAdmin.getConfigurationProperties()).thenReturn(properties);
        dependentServiceAvailability.useCiruitBreaker=false;
        final boolean result = dependentServiceAvailability.checkService();

        assertFalse(result);
    }

    @Test
    @DisplayName("When an empty string for the bootstrap server is supplied, max retries should be reached and exhausted")
    void test_empty_bootstrap_kafka_not_reachable_max_retries_reached() {

        Map<String, Object> properties = new HashMap<>(1);
        properties.put("bootstrap.servers", "");
        when(kafkaAdmin.getConfigurationProperties()).thenReturn(properties);
        dependentServiceAvailability.useCiruitBreaker=false;
        final boolean result = dependentServiceAvailability.checkService();

        assertFalse(result);
    }


}
