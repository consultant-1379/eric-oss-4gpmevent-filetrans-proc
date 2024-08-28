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
package com.ericsson.oss.adc.util;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static com.ericsson.oss.adc.util.FileTransferMetricsUtil.NUM_OUTPUT_KAFKA_MESSAGES_PRODUCED_SUCCESSFULLY;
import static com.ericsson.oss.adc.util.FileTransferMetricsUtil.NUM_PROCESSED_FILE_DATA_VOLUME;
import static junit.framework.TestCase.*;

@SpringBootTest(classes = {FileTransferMetricsUtil.class,SimpleMeterRegistry.class})
class FileTransferMetricsUtilTest {

    private static final String NOT_A_VALID_METRIC_NAME = "Not a valid metric name";
    private final FileTransferMetricsUtil metrics = new FileTransferMetricsUtil(new SimpleMeterRegistry());

    @Test
    @DisplayName("TEST: Verify incrementCounterByName fails to increment unknown counter name")
    void test_incrementCounterByNameFailsUnknownCounter() {
        assertFalse("Expected increment on unknown counter to fail", metrics.incrementCounterByName("dumyValue"));
    }

    @Test
    @DisplayName("TEST: Verify incrementCounterByName successfully increments known counter name")
    void test_incrementCounterByNamePassesknownCounter() {
        assertTrue("Expected increment on known counter to pass", metrics.incrementCounterByName(FileTransferMetricsUtil.NUM_OUTPUT_KAFKA_MESSAGES_PRODUCED_SUCCESSFULLY));
    }

    @Test
    @DisplayName("TEST: Verify getCounterValueByName fails to get value of unknown counter name")
    void test_getCounterValueByNameFailsUnknownCounter() {
        assertEquals("Expected get value of unknown counter to reurn zero", 0.0, metrics.getCounterValueByName("dummyVlaue"));
    }

    @Test
    @DisplayName("TEST: Verify getCounterValueByName successfully get value of known counter name")
    void test_getCounterValueByNamePassesknownCounter() {
        metrics.incrementCounterByName(NUM_OUTPUT_KAFKA_MESSAGES_PRODUCED_SUCCESSFULLY);
        final Double result = metrics.getCounterValueByName(NUM_OUTPUT_KAFKA_MESSAGES_PRODUCED_SUCCESSFULLY);
        assertTrue("Expected get value of known counter to return non zero, actual value = " + result, result > 0.0);
    }

    @Test
    @DisplayName("TEST: Verify Metrcis counter print with default value [0]")
    void test_printMetrics() {
        metrics.printAllCounterValues("Test Data");
    }

    @Test
    @DisplayName("TEST: Verify addToGaugeByName fails to increment unknown guage name")
    void test_addToGaugeByNameFailsUnknownCounter() {
        assertFalse("Expected add on unknown guage to fail", metrics.addToGaugeByName("dummyValue", 0L));
    }

    @Test
    @DisplayName("TEST: Verify addToGaugeByName successfully increments known guage name")
    void test_addToGaugeByNameyNamePassesknownCounter() {
        assertTrue("Expected add on known guage to pass", metrics.addToGaugeByName(NUM_PROCESSED_FILE_DATA_VOLUME, 0L));
    }

}
