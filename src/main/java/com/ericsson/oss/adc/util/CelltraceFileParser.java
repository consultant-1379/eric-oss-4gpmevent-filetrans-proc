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

import com.ericsson.oss.adc.models.EventFileProcessMetrics;
import com.ericsson.oss.adc.models.EventHeader;
import com.ericsson.oss.adc.service.input.topic.InputTopicService;
import com.ericsson.oss.adc.service.output.topic.OutputTopicService;
import com.ericsson.oss.mediation.parsers.celltrace.CelltraceHeaderRecordHandler;
import com.ericsson.oss.mediation.parsers.exception.LoadingFailedException;
import com.ericsson.oss.mediation.parsers.handler.RecordDescriptor;
import com.ericsson.oss.mediation.parsers.handler.RecordHandler;
import com.ericsson.oss.mediation.parsersapi.base.util.DataConverters;
import com.ericsson.oss.mediation.parsersapi.base.util.FfvFivKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class CelltraceFileParser {

    private static final int RECORD_HEADER_LENGTH = 4;

    private CelltraceHeaderRecordHandler headerHandler;

    private static final short RECORD_TYPE_HEADER = 0;
    private static final short RECORD_TYPE_SCANNER = 3;
    private static final short RECORD_TYPE_EVENT = 4;
    private static final short RECORD_TYPE_FOOTER = 5;


    @Autowired
    private InputTopicService inputTopicService;

    @Autowired
    private OutputTopicService outputTopicService;

    private final Lock lock = new ReentrantLock();

    @Value("${droppedPIEventList}")
    private List<Integer> droppedPIEventList;

    private static final Logger LOG = LoggerFactory.getLogger(CelltraceFileParser.class);

    public CelltraceFileParser() {
        RecordDescriptor celltraceRecordDescriptor = CelltraceParserUtil.getCelltraceRecordDescriptor();
        Map<Short, RecordHandler> handlerMap = celltraceRecordDescriptor.getHandlerMap();
        for (Map.Entry<Short, RecordHandler> entry : handlerMap.entrySet()) {
            if (entry.getValue() instanceof CelltraceHeaderRecordHandler) {
                headerHandler = (CelltraceHeaderRecordHandler) entry.getValue();
                break;
            }
        }
    }

    /**
     * @param fileName
     * @throws IOException
     */
    public EventFileProcessMetrics processCelltrace(String fileName, String nodeName) throws IOException, LoadingFailedException {
        File file = new File(fileName);
        long startTime = System.currentTimeMillis();
        int recordNumber = 0;
        Map<Integer, Integer> droppedEventsMap = new HashMap<>();
        EventHeader eventHeader = null;
        EventFileProcessMetrics eventFileProcessMetrics = new EventFileProcessMetrics();
        try (final InputStream neFileInputStream = CelltraceParserUtil.getFileInputStream(file, new FileInputStream(file))) {
            while (true) {
                final byte[] recordHeader = CelltraceParserUtil.getByteArray(neFileInputStream, RECORD_HEADER_LENGTH);
                final int recordLength = (int) DataConverters.getByteArrayInteger(recordHeader, 0, 2, DataConverters.UNSIGNED_FLAG);
                final short recordType = (short) DataConverters.getByteArrayInteger(recordHeader, 2, 2, DataConverters.UNSIGNED_FLAG);
                final byte[] recordData = CelltraceParserUtil.getByteArray(neFileInputStream, recordLength - RECORD_HEADER_LENGTH);

                if (recordType == RECORD_TYPE_EVENT) {
                    if (sendToKafka(recordData, recordHeader, eventHeader, droppedEventsMap)) {
                        recordNumber++;
                    }
                } else if (recordType == RECORD_TYPE_SCANNER) {
                    //
                } else if (recordType == RECORD_TYPE_HEADER) {
                    lock.lock();
                    try {
                        headerHandler.setEventFileName(file.getAbsolutePath());
                        headerHandler.process(recordData);
                        eventHeader = prepareEventHeader(nodeName);
                    } finally {
                        lock.unlock();
                    }
                } else if (recordType == RECORD_TYPE_FOOTER) {
                    break;
                } else {
                    LOG.info("Invalid record type received in the file");
                    throw new IOException("Invalid record type received in the file");
                }
            }
            long processedTime = System.currentTimeMillis() - startTime;
            inputTopicService.recordTimer(processedTime, recordNumber);
        }
        eventFileProcessMetrics.setSentEventsCount(recordNumber);
        eventFileProcessMetrics.setDroppedEventsMap(droppedEventsMap);
        return eventFileProcessMetrics;
    }

    public EventHeader prepareEventHeader(String nodeName) {
        FfvFivKey ffVFivKey = headerHandler.getFileVersionKey();
        String dataVersion = ffVFivKey.getIteration() + "_" + ffVFivKey.getFileInformationVersion();
        EventHeader eventHeader = new EventHeader();
        eventHeader.setDataVersion(dataVersion);
        eventHeader.setNodeName(nodeName);
        return eventHeader;
    }

    public boolean sendToKafka(byte[] recordData, byte[] recordHeader, EventHeader eventHeader, Map<Integer, Integer> droppedEventsMap) {
        final int eventId = (int) DataConverters.getByteArrayInteger(recordData, 0, 3, DataConverters.UNSIGNED_FLAG);
        if (droppedPIEventList.contains(eventId)) {
            droppedEventsMap.merge(eventId, 1, Integer::sum);
            return false;
        } else {
            byte[] combined = new byte[recordHeader.length + recordData.length];
            System.arraycopy(recordHeader, 0, combined, 0, recordHeader.length);
            System.arraycopy(recordData, 0, combined, recordHeader.length, recordData.length);
            eventHeader.setEventId(eventId);
            outputTopicService.sendKafkaMessage(combined, eventHeader);
            return true;
        }
    }
}

