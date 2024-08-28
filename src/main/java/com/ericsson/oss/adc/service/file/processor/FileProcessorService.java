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

package com.ericsson.oss.adc.service.file.processor;

import com.ericsson.oss.adc.models.EventFileProcessMetrics;
import com.ericsson.oss.adc.util.CelltraceFileParser;
import com.ericsson.oss.adc.util.ConsumerRecordSkipException;
import com.ericsson.oss.adc.util.ConsumerRecordRollbackException;
import com.ericsson.oss.adc.util.FileTransferMetricsUtil;
import com.ericsson.oss.mediation.parsers.exception.LoadingFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.ericsson.oss.adc.util.FileTransferMetricsUtil.NUM_EVENT_FILES_PROCESSED;
import static com.ericsson.oss.adc.util.FileTransferMetricsUtil.NUM_PROCESSED_FILE_DATA_VOLUME;
import static com.ericsson.oss.adc.util.FileTransferMetricsUtil.NUM_FILE_PARSED;
import static com.ericsson.oss.adc.util.FileTransferMetricsUtil.FILE_FAILED_TO_PARESED;
import static com.ericsson.oss.adc.util.FileTransferMetricsUtil.NUM_OUTPUT_KAFKA_MESSAGES_PRODUCED_SUCCESSFULLY;
import static com.ericsson.oss.adc.util.FileTransferMetricsUtil.NUM_OUTPUT_KAFKA_MESSAGES_DROPPED;

/**
 * Implementation for processing the 4G event files. This involves reading an event file from ephemeral storage,
 * breaking the events up into records and writing the records to Kafka.
 */
@Component
public class FileProcessorService {

    private static final Logger LOG = LoggerFactory.getLogger(FileProcessorService.class);



    @Autowired
    private CelltraceFileParser celltraceFileParser;

    @Autowired
    private FileTransferMetricsUtil metricsUtil;
    /**
     * Process the event file downloaded from ENM.
     * Transactional -If processing of file fails, exception will be thrown and transaction will be aborted.
     *
     * @param downloadedFile event file downloaded from ENM
     * @param nodeName       string name of ENM Node from which the event file came
     */

    public void processEventFile(File downloadedFile, String nodeName) throws ConsumerRecordSkipException, ConsumerRecordRollbackException {
        String downloadedFileLocation = downloadedFile.getPath();

        if (!downloadedFile.exists()) {
            throw new ConsumerRecordRollbackException(String.format("File not found: %s", downloadedFile.getPath()),
                    new FileNotFoundException(downloadedFile.getPath()));
        }

        try {
            EventFileProcessMetrics eventFileProcessMetrics = celltraceFileParser.processCelltrace(downloadedFileLocation, nodeName);
            metricsUtil.incrementCounterByName(NUM_EVENT_FILES_PROCESSED);
            metricsUtil.addToGaugeByName(NUM_PROCESSED_FILE_DATA_VOLUME,downloadedFile.length());
            metricsUtil.incrementCounterByName(NUM_FILE_PARSED);
            metricsUtil.incrementCounterByName(NUM_OUTPUT_KAFKA_MESSAGES_PRODUCED_SUCCESSFULLY, eventFileProcessMetrics.getSentEventsCount());
            metricsUtil.incrementCounterByName(NUM_OUTPUT_KAFKA_MESSAGES_DROPPED, eventFileProcessMetrics.getDroppedEventsMap().values().stream().mapToInt(Integer::intValue).sum());

            LOG.debug("Successfully processed file: '{}', number of events sent from this file {}", downloadedFileLocation, eventFileProcessMetrics);
        } catch (LoadingFailedException e) {
            LOG.error("Loading Failed Exception: {}", e.getMessage());
            metricsUtil.incrementCounterByName(FILE_FAILED_TO_PARESED);
            throw new ConsumerRecordSkipException("LoadingFailedException while parsing the file " + downloadedFileLocation, e);
        } catch (Exception e) {
            metricsUtil.incrementCounterByName(FILE_FAILED_TO_PARESED);
            throw new ConsumerRecordSkipException("Error processing file: " + downloadedFileLocation, e);
        } finally {
            if (downloadedFile.exists()) {
                cleanUp(downloadedFileLocation);
                LOG.debug("downloadedFileLocation is Cleaned: [{}] ", downloadedFileLocation);
            }
        }
    }


    /**
     * Perform clean up after processing the 4G event file - removing temporary files from ephemeral storage
     *
     * @param fileToDelete the path to the file to be deleted
     */
    protected void cleanUp(final String fileToDelete) {
        if (fileToDelete != null) {
            try {
                LOG.debug("Deleting {}", fileToDelete);
                Path filePath = new File(fileToDelete).toPath();
                Files.delete(filePath);
            } catch (Exception e) {
                LOG.error("Error Attempting to delete file: {}", e.getMessage());
            }
        } else {
            LOG.error("File path provided for deletion is null");
        }
    }

}
