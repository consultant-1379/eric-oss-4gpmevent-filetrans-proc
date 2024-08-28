/*******************************************************************************
 * COPYRIGHT Ericsson 2022
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

package com.ericsson.oss.adc.service.input.topic;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of metrics on the Input Topic
 */
@Service
public class InputTopicService {

    private final MeterRegistry meterRegistry;
    private final Timer timer;

    private static final String PROCESSED_FILES_TIME_TOTAL = "eric-oss-4gpmevent-filetrans-proc:processed.files.time.total";
    private static final Logger LOGGER = LoggerFactory.getLogger(InputTopicService.class);
    private static final Duration[] durations = { Duration.ofMillis(100), Duration.ofMillis(1000), Duration.ofMillis(10000), Duration.ofMillis(60000),
            Duration.ofMillis(300000), Duration.ofMillis(540000), Duration.ofMillis(600000), Duration.ofMillis(900000)};


    public InputTopicService(MeterRegistry meterRegistry){
        this.meterRegistry = meterRegistry;
        timer = Timer.builder(PROCESSED_FILES_TIME_TOTAL).serviceLevelObjectives(durations).publishPercentileHistogram().register(this.meterRegistry);

    }
    /**
     * Records time to process a set number of records
     * @param timeToProcessBatch
     * @param recordNumber
     */
    public void recordTimer(final long timeToProcessBatch, final int recordNumber){
        timer.record(timeToProcessBatch, TimeUnit.MILLISECONDS);
        LOGGER.debug("timer: Record count {} took {} ms to process", recordNumber, timeToProcessBatch);
    }

}
