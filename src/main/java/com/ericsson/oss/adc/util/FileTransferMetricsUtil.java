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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


@Component
@Slf4j
public class FileTransferMetricsUtil {

    private static final String ERIC_OSS_4G_FILETRANS = "eric-oss-4gpmevent-filetrans-proc:";

    public static final String NUM_OUTPUT_KAFKA_MESSAGES_PRODUCED_SUCCESSFULLY = "total.event.produced.successfully";
    public static final String NUM_OUTPUT_KAFKA_MESSAGES_DROPPED = "total.event.dropped";
    public static final String NUM_EVENT_FILES_PROCESSED ="event.files.processed";
    public static final String NUM_PROCESSED_FILE_DATA_VOLUME ="processed.file.data.volume";
    public static final String NUM_FILE_PARSED ="files.parsed";
    public static final String FILE_FAILED_TO_PARESED ="files.failed.to.parse";

    public static final String NUM_SUCCESSFULL_FILES_TRANSFERRED ="num.successful.file.transfer";
    public static final String NUM_FAILED_FILES_TRANSFERRED ="num.failed.file.transfer";
    public static final String TRANSFERRED_FILE_DATA_VOLUME ="transferred.file.data.volume";
    public static final String NUM_FILES_DELETED_DUETO_CONNECTION_EXCEPTION ="num.files.deleted.sftp.connection.exception";


    private final ConcurrentHashMap<String, Counter> counterMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> gaugeMap = new ConcurrentHashMap<>();
    final MeterRegistry meterRegistry;

    public FileTransferMetricsUtil(final MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        counterMap.put(NUM_OUTPUT_KAFKA_MESSAGES_PRODUCED_SUCCESSFULLY, meterRegistry.counter(ERIC_OSS_4G_FILETRANS + NUM_OUTPUT_KAFKA_MESSAGES_PRODUCED_SUCCESSFULLY));
        counterMap.put(NUM_OUTPUT_KAFKA_MESSAGES_DROPPED, meterRegistry.counter(ERIC_OSS_4G_FILETRANS + NUM_OUTPUT_KAFKA_MESSAGES_DROPPED));

        counterMap.put(NUM_EVENT_FILES_PROCESSED, meterRegistry.counter(ERIC_OSS_4G_FILETRANS + NUM_EVENT_FILES_PROCESSED));
        counterMap.put(NUM_FILE_PARSED, meterRegistry.counter(ERIC_OSS_4G_FILETRANS + NUM_FILE_PARSED));
        counterMap.put(FILE_FAILED_TO_PARESED, meterRegistry.counter(ERIC_OSS_4G_FILETRANS + FILE_FAILED_TO_PARESED));


        counterMap.put(NUM_SUCCESSFULL_FILES_TRANSFERRED, meterRegistry.counter(ERIC_OSS_4G_FILETRANS + NUM_SUCCESSFULL_FILES_TRANSFERRED));
        counterMap.put(NUM_FAILED_FILES_TRANSFERRED, meterRegistry.counter(ERIC_OSS_4G_FILETRANS + NUM_FAILED_FILES_TRANSFERRED));
        counterMap.put(NUM_FILES_DELETED_DUETO_CONNECTION_EXCEPTION, meterRegistry.counter(ERIC_OSS_4G_FILETRANS + NUM_FILES_DELETED_DUETO_CONNECTION_EXCEPTION));


        gaugeMap.put(NUM_PROCESSED_FILE_DATA_VOLUME, meterRegistry.gauge(ERIC_OSS_4G_FILETRANS + NUM_PROCESSED_FILE_DATA_VOLUME,new AtomicLong(0)));


        gaugeMap.put(TRANSFERRED_FILE_DATA_VOLUME, meterRegistry.gauge(ERIC_OSS_4G_FILETRANS + TRANSFERRED_FILE_DATA_VOLUME,new AtomicLong(0)));

    }

    public boolean incrementCounterByName(final String counterName) {
        if (counterMap.containsKey(counterName)) {
            counterMap.get(counterName).increment();
            return true;
        } else{
            log.error("Counter {} doesn't exist", counterName);
            return false;
        }
    }

    public boolean incrementCounterByName(final String counterName, final long value) {
        if (counterMap.containsKey(counterName)) {
            counterMap.get(counterName).increment(value);
            return true;
        } else{
            log.error("Counter {} doesn't exist", counterName);
            return false;
        }
    }

    public void printAllCounterValues(final String stage) {
        log.info("--------------------------------------------- METRIC VALUES : {} -------------------------------------------------", stage);
        log.info("Kafka OUTPUT Messages produced (total): {}", getCounterValueByName(NUM_OUTPUT_KAFKA_MESSAGES_PRODUCED_SUCCESSFULLY));
        log.info("Kafka OUTPUT Messages dropped (total): {}", getCounterValueByName(NUM_OUTPUT_KAFKA_MESSAGES_DROPPED));
        log.info("Event File Processed (total): {}", getCounterValueByName(NUM_EVENT_FILES_PROCESSED));
        log.info("Processed File Data Volume (total): {}", getCounterValueByName(NUM_PROCESSED_FILE_DATA_VOLUME));
        log.info("File Parsed (total): {}", getCounterValueByName(NUM_FILE_PARSED));
        log.info("File Failed to Parsed (total): {}", getCounterValueByName(FILE_FAILED_TO_PARESED));
        log.info("Successful File Transfered (total): {}", getCounterValueByName(NUM_SUCCESSFULL_FILES_TRANSFERRED));
        log.info("File Failed to Transfer (total): {}", getCounterValueByName(NUM_FAILED_FILES_TRANSFERRED));
        log.info("Transfered File Data Volume (total): {}", getCounterValueByName(TRANSFERRED_FILE_DATA_VOLUME));
        log.info("File Deleted Due to Connection Exception (total): {}", getCounterValueByName(NUM_FILES_DELETED_DUETO_CONNECTION_EXCEPTION));
        log.info("------------------------------------------------------------------------------------------------------------------");
    }

    public double getCounterValueByName(final String counterName) {
        if (counterMap.containsKey(counterName)) {
            return counterMap.get(counterName).count();
        } else {
            log.error("Counter {} doesn't exist", counterName);
            return 0;
        }
    }

    public boolean addToGaugeByName(final String gaugeName, final long value) {
        if (gaugeMap.containsKey(gaugeName)) {
            gaugeMap.get(gaugeName).addAndGet(value);
            return true;
        }
        else{
            log.error("Counter {} doesn't exist", gaugeName);
            return false;
        }
    }
}
