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

import com.ericsson.oss.adc.config.EventFileDownloadConfiguration;
import com.ericsson.oss.adc.config.KafkaHeadersConfig;
import com.ericsson.oss.adc.models.EventFileProcessMetrics;
import com.ericsson.oss.adc.models.EventHeader;
import com.ericsson.oss.adc.service.input.topic.InputTopicService;
import com.ericsson.oss.adc.service.output.topic.OutputTopicService;
import com.ericsson.oss.mediation.parsers.celltrace.CelltraceFooterRecordHandler;
import com.ericsson.oss.mediation.parsers.celltrace.CelltraceHeaderRecordHandler;
import com.ericsson.oss.mediation.parsers.exception.LoadingFailedException;
import com.ericsson.oss.mediation.parsersapi.base.util.FfvFivKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.*;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;



@SpringBootTest(classes = {CelltraceFileParser.class,OutputTopicService.class,KafkaHeadersConfig.class, EventFileDownloadConfiguration.class})
class CelltraceFilePraserTest {

    @MockBean
    private CelltraceParserUtil celltraceParserUtil;

    @MockBean
    private OutputTopicService outputTopicServiceMock;

    @Autowired
    CelltraceFileParser celltraceFileParser;

    @Autowired
    private KafkaHeadersConfig kafkaHeadersConfig;

    @MockBean
    private InputTopicService inputTopicService;

    @Captor
    private ArgumentCaptor<byte[]> combinedDataCaptor;

    @Captor
    private ArgumentCaptor<EventHeader> eventHeaderCaptor;

    
    @Test
    @DisplayName("Should be false if file is invalid")
    void testGetFileInputStreamForNonGzipFile() throws IOException {
        File nonGzipFile = createTestFile("test.txt");
        InputStream inputStream = CelltraceParserUtil.getFileInputStream(nonGzipFile, new FileInputStream(nonGzipFile));
        assertTrue(inputStream instanceof BufferedInputStream);
        assertFalse(inputStream instanceof GZIPInputStream);
        inputStream.close();
        assertTrue(nonGzipFile.delete());

    }

    @Test
    @DisplayName("Should be true if file is valid")
    void testGetFileInputStreamForGzipFile() throws IOException {

        File gzipFile = createGzipTestFile("test.gz");
        InputStream inputStream = CelltraceParserUtil.getFileInputStream(gzipFile, new FileInputStream(gzipFile));
        assertFalse(inputStream instanceof BufferedInputStream);
        assertTrue(inputStream instanceof GZIPInputStream);
        inputStream.close();
        assertTrue(gzipFile.delete());
    }

    @Test
    @DisplayName("Should be true if footer record type is valid")
    void testFooterRecordTypeMatches() {
        CelltraceFooterRecordHandler footerClass = new CelltraceFooterRecordHandler();
        int recordType = 5;
        isValuesEqual(recordType, footerClass.getRecordType());
        Assertions.assertEquals(recordType, footerClass.getRecordType());

        assertTrue(isValuesEqual(recordType, footerClass.getRecordType()));
    }

    @Test
    @DisplayName("Should be false if footer record type is invalid")
    void testFooterRecordTypeDoesNotMatch() {
        CelltraceFooterRecordHandler footerClass = new CelltraceFooterRecordHandler();
        int recordType = 10;
        assertFalse(isValuesEqual(recordType, footerClass.getRecordType()));
    }


    public boolean isValuesEqual(int value1, int value2) {
        return value1 == value2;
    }

    private File createTestFile(String fileName) throws IOException {
        File file = new File(fileName);
        try (Writer writer = new FileWriter(file)) {
            writer.write("This is a test file.");
        }
        return file;
    }

    private File createGzipTestFile(String fileName) throws IOException {
        File file = new File(fileName);
        try (FileOutputStream fos = new FileOutputStream(file);
             GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fos);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzipOutputStream))) {
            writer.write("This is a GZIP test file.");
        }
        return file;
    }

    @Test
    @DisplayName("Throws IOException when file is invalid")
    void testRecordTypeEqualsFooterHandlerRecordType() {
        String testResourceDir = "src/test/resources/test-event-files/";
        String fileName = "A20181121.0645+0000-0700+0000_SubNetwork=4GMeContext=NE12345_CellTrace_DUL1_1.bin";
        String fileLocation = testResourceDir + fileName;
        Assertions.assertThrows((IOException.class),() -> celltraceFileParser.processCelltrace(fileLocation, "testNode"));
    }

    @Test
    @DisplayName("Checking byte array data is valid")
    void testGetByteArray() throws IOException {
        byte[] sampleData = {1, 2, 3, 4, 5};
        int dataLength = sampleData.length;
        ByteArrayInputStream inputStream = new ByteArrayInputStream(sampleData);
        byte[] result = CelltraceParserUtil.getByteArray(inputStream, dataLength);
        assertArrayEquals(sampleData, result);
    }

    @Test
    @DisplayName("Checking valid headers in event header")
    public void testPrepareEventHeaderFileVersion() {

        CelltraceHeaderRecordHandler headerHandler = mock(CelltraceHeaderRecordHandler.class);
        when(headerHandler.getUtcCalendar()).thenReturn(Calendar.getInstance());
        FfvFivKey ffVFivKey = new FfvFivKey("1", "2","3");
        when(headerHandler.getFileVersionKey()).thenReturn(ffVFivKey);
        EventHeader result = celltraceFileParser.prepareEventHeader("testNode");
        assertNotNull(result.getDataVersion());
        assertEquals(2, kafkaHeadersConfig.kafkaEventHeaderVersion);
    }

    @Test
    @DisplayName("Send valid data to Kafka")
    void testSendToKafkaWithValidData() {
        // Set up test data
        byte[] recordHeader = new byte[]{0, -76, 0, 4, 0, 0, 8, 6, 45, 6, 1, -8, 4, 0, 0, 0, 8, -76, -92, 11, 4, 16, -13, 18, -44, 20, 97, 0, 5, -11, 16, -59, 69, 40, 5,};
        byte[] recordData = new byte[]{-48, -96, -12, 0, 24, 29, 1, 0, -125, 32, 27, -86, 0, 64, 17, 0, 34, 16, 2, 37, -5, -60, -104, 64, -82, 38, 127, -31, -110, -73, -17, -101, -86, 0, -46, 0, 3, 0, 32, 40, 9, -63, 126, 117, 87, -96, -76, 0, -54, 0, -64, 104, 5, 68, -47, 16, 15, 93, -114, 43, 49, 9, -93, 32, 30, -69, 28, 87, -94, 39, -24, 1, -69, -92, -114, -92, -2, 84, 62, 0, 12, 0, 12, 6, 7, -123, 13, 4, -127, -80, 66, 0, 92, 80, 110, -48, 48, 13, 64, 0, -100, 40, 66, 63, 9, -127, 24, 36, 4, -84, 38, -124, 112, -110, 18, -64, -111, 19, 83, 0, 113, 24, 36, -62, 10, -87, 2, -26, 64, 90, 8, -116, -128, 0, 0, 0, 76, 35, 0, 0, 0, 0, 108, 0, 4};
        byte[] combinedRecord = new byte[recordHeader.length + recordData.length];
        Map<Integer, Integer> droppedEventMap = new HashMap<>();
        droppedEventMap.put(56, 10);
        droppedEventMap.put(26, 15);
        EventHeader eventHeader = new EventHeader();
        eventHeader.setEventId(10);
        eventHeader.setDataVersion("27_R94A");
        eventHeader.setNodeName("4GNode");
        celltraceFileParser.sendToKafka(recordData, recordHeader, eventHeader, droppedEventMap);
        outputTopicServiceMock.sendKafkaMessage(combinedRecord, eventHeader);
        verify(outputTopicServiceMock, times(2)).sendKafkaMessage(any(byte[].class), eq(eventHeader));

    }

    @Test
    @DisplayName("Dropping PI events before Sending to kafka")
    void testSendToKafkaWithDroppedData() throws IOException, LoadingFailedException {
        String testResourceDir = "src/test/resources/test-gz-files/";
        String fileName = "A20181121.0645+0000-0700+0000_SubNetwork=4GMeContext=NE12345_CellTrace_DUL1_1.bin.gz";
        String fileLocation = testResourceDir + fileName;
        EventFileProcessMetrics eventFileProcessMetrics = celltraceFileParser.processCelltrace(fileLocation, "testNode");
        long droppedEventCount = eventFileProcessMetrics.getDroppedEventsMap().values().stream().mapToInt(i -> i).sum();
        long libraryParsedEventsCount = TestCellTraceParser.processFile(new File(fileLocation));
        Assertions.assertEquals(libraryParsedEventsCount, droppedEventCount + eventFileProcessMetrics.getSentEventsCount());
        Assertions.assertNotEquals( 0, libraryParsedEventsCount);
    }

    @Test
    @DisplayName("Checking sent events with library parsed events")
    void testProcessCelltrace() throws IOException, LoadingFailedException {
        String testResourceDir = "src/test/resources/test-gz-files/";
        String fileName = "A20181121.0645+0000-0700+0000_SubNetwork=4GMeContext=NE12345_CellTrace_DUL1_1.bin.gz";
        String fileLocation = testResourceDir + fileName;
        EventFileProcessMetrics eventFileProcessMetrics = celltraceFileParser.processCelltrace(fileLocation, "testNode");
        long libraryParsedEvents = TestCellTraceParser.processFile(new File(fileLocation));
        long droppedEventCount = eventFileProcessMetrics.getDroppedEventsMap().values().stream().mapToInt(i -> i).sum();
        libraryParsedEvents = libraryParsedEvents - droppedEventCount;
        Assertions.assertEquals(eventFileProcessMetrics.getSentEventsCount(), libraryParsedEvents);
        Assertions.assertNotEquals( 0, droppedEventCount);
    }

    @Test
    @DisplayName("Checking if header are valid or not")
    public void testHeaderValidation() throws LoadingFailedException, IOException {
        String nodeName = "nodeName";
        String dataVersion = "27_R94A";
        long eventId = 3084;
        String testResourceDir = "src/test/resources/test-gz-files/";
        String fileName = "A20200227.0530-0545_LTE01_GENERATED_BY_RAN_FROM_OPEN_ALM_CellTrace_DUL1_1.bin.gz";
        String fileLocation = testResourceDir + fileName;
        celltraceFileParser.processCelltrace(fileLocation, nodeName);
        verify(outputTopicServiceMock, atLeastOnce()).sendKafkaMessage(combinedDataCaptor.capture(), eventHeaderCaptor.capture());
        EventHeader capturedEventHeader = eventHeaderCaptor.getValue();

        assertEquals(eventId, capturedEventHeader.getEventId());
        assertEquals(nodeName, capturedEventHeader.getNodeName());
        assertEquals(2, kafkaHeadersConfig.kafkaEventHeaderVersion);
        assertEquals(dataVersion, capturedEventHeader.getDataVersion());
    }
    }
