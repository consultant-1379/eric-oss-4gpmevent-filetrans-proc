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

package com.ericsson.oss.adc.controller.topiclistener;

import com.ericsson.oss.adc.config.KafkaConfiguration;
import com.ericsson.oss.adc.models.InputMessage;
import com.ericsson.oss.adc.models.connected.systems.ConnectionProperties;
import com.ericsson.oss.adc.models.connected.systems.Subsystem;
import com.ericsson.oss.adc.models.connected.systems.SubsystemType;
import com.ericsson.oss.adc.models.connected.systems.SubsystemUsers;
import com.ericsson.oss.adc.service.file.processor.FileProcessorService;
import com.ericsson.oss.adc.service.sftp.SFTPService;
import com.ericsson.oss.adc.service.kafka.KafkaConsumerService;
import com.ericsson.oss.adc.util.ConsumerRecordSkipException;
import com.ericsson.oss.adc.util.ConsumerRecordRollbackException;
import com.ericsson.oss.adc.util.FileTransferMetricsUtil;
import com.ericsson.oss.adc.util.StartupUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.catchexception.apis.BDDCatchException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.googlecode.catchexception.CatchException.caughtException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {InputTopicListenerImpl.class})
public class InputTopicListenerImplTest {

    @MockBean
    private SFTPService sftpServiceMock;

    @MockBean
    private FileProcessorService fileProcessorServiceMock;

    @SpyBean
    private InputTopicListenerImpl inputTopicListener;

    @Value("${spring.kafka.topics.input.name}")
    private String topicName;

    private static final String CONNECTION_EXCEPTION_SFTP_CONNECTION_NEVER_ESTABLISHED = "SftpConnectionNeverEstablishedNoFilesDownloadedOrProcessed";
    private static final String CONNECTION_EXCEPTION_FILE_DOWNLOAD_RETRIES_EXCEEDED_EXCEPTION = "EventFileDownloadRetriesExceededException";

    @MockBean
    private StartupUtil startupUtil;

    @MockBean
    private KafkaConsumerService kafkaConsumerService;

    @MockBean
    ObjectMapper objectMapper;

    @MockBean
    KafkaConfiguration kafkaConfiguration;

    @Mock
    Map<String, String> headerMap = new HashMap<>();

    @Mock
    InputMessage inputMessage;

    @MockBean
    FileTransferMetricsUtil metrics;

    private static final String NODE_NAME = "SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=ERBS-SUBNW-1,MeContext=ieatnetsimv6034-10_LTE35ERBS00104";
    private static final String NODE_TYPE = "RadioNode";
    private static final String DATA_TYPE = "PM_CELLTRACE";

    private static final String TEST_RESOURCE_DIR = "src/test/resources/test-event-files/";
    private static final String FILE_NAME = "A20181121.0645+0000-0700+0000_SubNetwork=4GMeContext=NE12345_CellTrace_DUL1_1.bin";
    private static final String FILE_LOCATION = TEST_RESOURCE_DIR + FILE_NAME;
    private static final File EVENT_FILE = Paths.get(FILE_LOCATION).toFile();
    private static final String SUB_SYSTEM_TYPE = "enm2";

    private static final InputMessage INPUT_MESSAGE = new InputMessage(NODE_NAME, NODE_TYPE, FILE_LOCATION, DATA_TYPE, EVENT_FILE, SUB_SYSTEM_TYPE);

    private final SubsystemUsers subsystemUsers1 = new SubsystemUsers(4L, 3L);
    private final SubsystemUsers subsystemUsers2 = new SubsystemUsers(15L, 3L);
    private final SubsystemUsers subsystemUsers3 = new SubsystemUsers(2L, 3L);
    private final SubsystemType subsystemType = new SubsystemType(null, "PhysicalDevice");
    private final String enm = "enm1";
    private final ConnectionProperties connectionProperties = new ConnectionProperties(
            5L,
            2L,
            "localhost",
            "tenant1",
            "user",
            "password",
            "localhost, localhost, localhost",
            "5678",
            null,
            new ArrayList<>((Arrays.asList(subsystemUsers1, subsystemUsers2, subsystemUsers3))));
    private final Subsystem subsystem = new Subsystem(
            2L,
            5L,
            "enm2",
            "https://test.subsystem-2/",
            null,
            new ArrayList<ConnectionProperties>(Collections.singletonList(connectionProperties)),
            "EricssonTEST",
            subsystemType,
            "eric-eo-ecm-adapter");
    private final Map<String, Subsystem> subSystemMapHappy = Collections.singletonMap(enm, subsystem);

    @Test
    @DisplayName("Should map the json correctly to the InputMessage model and call setUpSFTPConnectionAndDownloadFile and processEventFile methods once")
    public void test_input_topic_listener_parses_data_correctly() throws Exception {

        String enmName = "enm2";

        when(sftpServiceMock.downloadSftpFile(any(), any())).thenReturn(INPUT_MESSAGE);
        doNothing().when(fileProcessorServiceMock).processEventFile(any(), any());

        assertDoesNotThrow(() -> inputTopicListener.processInputMessage(INPUT_MESSAGE, SUB_SYSTEM_TYPE));

        verify(sftpServiceMock, times(1)).downloadSftpFile(any(), any());
        verify(fileProcessorServiceMock, times(1)).processEventFile(any(), any());
    }

    @Test
    @DisplayName("Should throw a ConnectException from failing to establish SFTP connection")
    public void test_input_topic_listener_catches_jsonProcessingException_when_deserializing_message() throws Exception {
        doThrow(ConsumerRecordRollbackException.class).when(sftpServiceMock).downloadSftpFile(any(), any());
        assertThrows(ConsumerRecordRollbackException.class, () -> inputTopicListener.processInputMessage(INPUT_MESSAGE, SUB_SYSTEM_TYPE));

        verify(sftpServiceMock, times(1)).downloadSftpFile(any(), any());
        verifyNoInteractions(fileProcessorServiceMock);
    }

    @Test
    @DisplayName("Should throw a ConnectException when a SFTP connection is never established")
    public void test_input_topic_listener_throws_connectException_when_sftp_connection_fails() throws Exception {
        doThrow(ConsumerRecordRollbackException.class).when(sftpServiceMock).downloadSftpFile(any(), any());
        assertThrows(ConsumerRecordRollbackException.class, () -> inputTopicListener.processInputMessage(INPUT_MESSAGE, SUB_SYSTEM_TYPE));

        doThrow(new ConsumerRecordRollbackException("sftp connection error", new IOException())).when(sftpServiceMock).downloadSftpFile(any(), any());
        assertThrows(ConsumerRecordRollbackException.class, () -> inputTopicListener.processInputMessage(INPUT_MESSAGE, SUB_SYSTEM_TYPE));
        verify(sftpServiceMock, times(2)).downloadSftpFile(any(), any()); // twice, listener retries because connection not established
        verifyNoInteractions(fileProcessorServiceMock);
    }

    @Test
    @DisplayName("Verify event file is skipped after failing to download for any file in a batch")
    public void test_input_topic_listener_skips_event_file_when_download_fails() throws Exception {
        when(sftpServiceMock.downloadSftpFile(any(), any())).thenReturn(null);

        BDDCatchException.when(() -> inputTopicListener.processInputMessage(INPUT_MESSAGE, SUB_SYSTEM_TYPE));

        verify(sftpServiceMock, times(1)).downloadSftpFile(any(), any());
        verifyNoInteractions(fileProcessorServiceMock);
        assertThat(caughtException()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Batch fails when the number of event file retry attempts is exceeded for any file in the batch causing a connect exception")
    public void test_input_topic_listener_fails_batch_download_when_numberOfEventFileDownloadRetries_exceeded() throws Exception {
        when(sftpServiceMock.downloadSftpFile(any(), any())).thenThrow(new ConsumerRecordRollbackException("sftp connection error", new IOException()));
        assertThrows(ConsumerRecordRollbackException.class, () -> inputTopicListener.processInputMessage(INPUT_MESSAGE, SUB_SYSTEM_TYPE));

        verify(sftpServiceMock, times(1)).downloadSftpFile(any(), any());
        verifyNoInteractions(fileProcessorServiceMock);
    }

    @Test
    @DisplayName("Should throw an IOException from failing to process the event file after download")
    public void test_input_topic_listener_throws_ioException_when_processEventFile_fails() throws Exception {


        when(sftpServiceMock.downloadSftpFile(any(), any())).thenReturn(INPUT_MESSAGE);
        doThrow(ConsumerRecordSkipException.class).when(fileProcessorServiceMock).processEventFile(any(), any());

        assertThrows(ConsumerRecordSkipException.class, () -> inputTopicListener.processInputMessage(INPUT_MESSAGE, SUB_SYSTEM_TYPE));
        verify(sftpServiceMock, times(1)).downloadSftpFile(any(), any());
        verify(fileProcessorServiceMock, times(1)).processEventFile(any(), any());
    }

    @Test
    @DisplayName("Listen successMessage")
    void testListenSuccessMessage() throws InterruptedException {
        List<String> mockTopicList = Arrays.asList("file-notification-service--4g-event--enm1", "file-notification-service--4g-event--enm2");
        when(startupUtil.getInputTopicList()).thenReturn(mockTopicList);
        when(startupUtil.isApplicationReady()).thenReturn(true);
        when(sftpServiceMock.getSubsystemDetails()).thenReturn(subSystemMapHappy);
        when(sftpServiceMock.checkSubsystemsConnection(anyString())).thenReturn(true);

        //calling multiple times should not have any effect
        inputTopicListener.startOrStopMessageContainers();
        inputTopicListener.startOrStopMessageContainers();
        inputTopicListener.startOrStopMessageContainers();
        verify(kafkaConsumerService, times(1)).register(anySet(), any());
        verify(kafkaConsumerService, times(1)).start();
    }


    @Test
    @DisplayName("check and restart container")
    void testcheckAndRestartContainer() throws InterruptedException {
        Set<String> topicList = Set.of("enm1", "enm2");
        when(sftpServiceMock.checkSubsystemsConnection(anyString())).thenReturn(true);

        //calling multiple times should not have any effect
        inputTopicListener.checkAndRestartContainer(topicList, Set.of());
        inputTopicListener.checkAndRestartContainer(topicList, Set.of());
        verify(kafkaConsumerService, times(1)).register(anySet(), any());
        verify(kafkaConsumerService, times(1)).start();
        verify(kafkaConsumerService, times(1)).stop();
        Mockito.clearInvocations(kafkaConsumerService);

        inputTopicListener.checkAndRestartContainer(Set.of(), Set.of());
        inputTopicListener.checkAndRestartContainer(Set.of(), Set.of());
        verify(kafkaConsumerService, times(1)).stop();
        verify(kafkaConsumerService, times(0)).register(anySet(), any());
        verify(kafkaConsumerService, times(0)).start();
        Mockito.clearInvocations(kafkaConsumerService);

        inputTopicListener.checkAndRestartContainer(Set.of(), topicList);
        inputTopicListener.checkAndRestartContainer(Set.of(), topicList);
        verify(kafkaConsumerService, times(0)).stop();
        verify(kafkaConsumerService, times(0)).register(anySet(), any());
        verify(kafkaConsumerService, times(0)).start();
        Mockito.clearInvocations(kafkaConsumerService);


        inputTopicListener.checkAndRestartContainer(Set.of("enm1"), topicList);
        inputTopicListener.checkAndRestartContainer(Set.of("enm2"), topicList);
        verify(kafkaConsumerService, times(2)).stop();
        verify(kafkaConsumerService, times(2)).register(anySet(), any());
        verify(kafkaConsumerService, times(2)).start();
        Mockito.clearInvocations(kafkaConsumerService);

        Set<String> topicList2 = Set.of("enm1", "enm2", "enm3");
        inputTopicListener.checkAndRestartContainer(topicList, Set.of());
        inputTopicListener.checkAndRestartContainer(topicList, Set.of());
        inputTopicListener.checkAndRestartContainer(topicList2, Set.of());
        inputTopicListener.checkAndRestartContainer(topicList2, Set.of());
        inputTopicListener.checkAndRestartContainer(topicList, Set.of());
        inputTopicListener.checkAndRestartContainer(topicList, Set.of());
        inputTopicListener.checkAndRestartContainer(topicList2, Set.of());
        inputTopicListener.checkAndRestartContainer(topicList2, Set.of());
        verify(kafkaConsumerService, times(4)).register(anySet(), any());
        verify(kafkaConsumerService, times(4)).start();
        verify(kafkaConsumerService, times(4)).stop();
        Mockito.clearInvocations(kafkaConsumerService);

    }


    @Test
    @DisplayName("Listen testListenFailureMessage")
    void testListenFailureMessage() throws InterruptedException {
        List<String> mockTopicList = Arrays.asList("file-notification-service--4g-event--enm1", "file-notification-service--4g-event--enm2");
        when(startupUtil.getInputTopicList()).thenReturn(mockTopicList);
        when(startupUtil.isApplicationReady()).thenReturn(true);
        when(sftpServiceMock.getSubsystemDetails()).thenReturn(subSystemMapHappy);
        when(sftpServiceMock.checkSubsystemsConnection(anyString())).thenThrow(new RuntimeException("connection failed"));

        inputTopicListener.startOrStopMessageContainers();
        verify(kafkaConsumerService, times(0)).register(anySet(), any());
        verify(kafkaConsumerService, times(0)).start();
    }

    @Test
    @DisplayName("Listen testListenConnectionFailMessage")
    void testListenConnectionFailMessage() throws InterruptedException {
        List<String> mockTopicList = Arrays.asList("file-notification-service--4g-event--enm1", "file-notification-service--4g-event--enm2");
        when(startupUtil.getInputTopicList()).thenReturn(mockTopicList);
        when(startupUtil.isApplicationReady()).thenReturn(true);
        when(sftpServiceMock.getSubsystemDetails()).thenReturn(subSystemMapHappy);
        when(sftpServiceMock.checkSubsystemsConnection(anyString())).thenReturn(false);

        inputTopicListener.startOrStopMessageContainers();
        verify(kafkaConsumerService, times(0)).register(anySet(), any());
        verify(kafkaConsumerService, times(0)).start();
    }

    @Test
    @DisplayName("Listen testListenInterruptFailMessage")
    void testListenInterruptFailMessage() throws InterruptedException {
        List<String> mockTopicList = Arrays.asList("file-notification-service--4g-event--enm1", "file-notification-service--4g-event--enm2");
        when(startupUtil.getInputTopicList()).thenReturn(mockTopicList);
        when(startupUtil.isApplicationReady()).thenReturn(true);
        doThrow(new InterruptedException("thread interrupted")).when(startupUtil).sleepInSeconds(anyInt());
        when(sftpServiceMock.getSubsystemDetails()).thenReturn(subSystemMapHappy);
        when(sftpServiceMock.checkSubsystemsConnection(anyString())).thenReturn(false);

        assertThrows(InterruptedException.class, () -> inputTopicListener.startOrStopMessageContainers());
        verify(kafkaConsumerService, times(0)).register(anySet(), any());
        verify(kafkaConsumerService, times(0)).start();
    }

    @Test
    @DisplayName("Listen testListenRuntimeFailMessage")
    void testListenRuntimeFailMessage() {
        List<String> mockTopicList = Arrays.asList("file-notification-service--4g-event--enm1", "file-notification-service--4g-event--enm2");
        when(startupUtil.getInputTopicList()).thenReturn(mockTopicList);
        when(startupUtil.isApplicationReady()).thenReturn(true);
        when(sftpServiceMock.getSubsystemDetails()).thenReturn(subSystemMapHappy);
        when(sftpServiceMock.checkSubsystemsConnection(anyString())).thenReturn(false);

        assertDoesNotThrow(() -> inputTopicListener.startOrStopMessageContainers());
        verify(kafkaConsumerService, times(0)).register(anySet(), any());
        verify(kafkaConsumerService, times(0)).start();
    }

    @Test
    @DisplayName("test ProcessRecords")
    void testProcessRecords()  {

        String topic = "file-notification-service--4g-event-enm2";
        Map<TopicPartition, List<ConsumerRecord<String, InputMessage>>> records = new LinkedHashMap<>();
        records.put(new TopicPartition(topic, 0), new ArrayList<ConsumerRecord<String, InputMessage>>());
        InputMessage inputMessage=new InputMessage();

        inputMessage.setDataType("PM_CEL");
        inputMessage.setNodeName("RadioNode");
        inputMessage.setFileLocation("/sftp/ericsson/pmic1/CELLTRACE/LTE01_GENERATED_BY_RAN_FROM_OPEN0070/A20231020.1330+0000-1345+0000_LTE01_GENERATED_BY_RAN_FROM_OPEN0070_CellTrace_DUL1_1.bin.gz");

        ConsumerRecord<String, InputMessage> record1 = new ConsumerRecord<>(topic, 1, 0, "1", inputMessage);
        ConsumerRecord<String, InputMessage> record2 = new ConsumerRecord<>(topic, 1, 1, "2", inputMessage);
        Header header = new Header() {
            @Override

            public String key() {
                return "test";
            }

            @Override
            public byte[] value() {
                return new byte[0];
            }
        };
        record1.headers().add(header);
        record2.headers().add(header);
        records.put(new TopicPartition(topic, 1), Arrays.asList(record1, record2));
        records.put(new TopicPartition(topic, 2), new ArrayList<ConsumerRecord<String, InputMessage>>());
        ConsumerRecords<String, InputMessage> consumerRecords = new ConsumerRecords<>(records);
        List<ConsumerRecord<String, InputMessage>> list = new ArrayList<>();
        list.add(record1);
        list.add(record2);
        InputMessage in = InputMessage.builder()
                .subSystemType("ENM2")
                .fileLocation("/sftp/ericsson/pmic1/CELLTRACE/LTE01_GENERATED_BY_RAN_FROM_OPEN0070/A20231020.1330+0000-1345+0000_LTE01_GENERATED_BY_RAN_FROM_OPEN0070_CellTrace_DUL1_1.bin.gz")
                .build();
        headerMap.put("key", "value");
        InputMessage dummyInputMessage=new InputMessage();
        dummyInputMessage.setNodeName(headerMap.get("key"));
        Mockito.when(record1.value()).thenReturn(in);
        Mockito.lenient().when(inputTopicListener.constructInputMessage(headerMap,dummyInputMessage)).thenReturn(in);
        assertDoesNotThrow(() -> inputTopicListener.processRecord(List.of(record1)));
        assertDoesNotThrow(() -> inputTopicListener.processRecord(List.of(record2)));
        Assert.assertNotNull(list);
    }


    @Test
    @DisplayName("test ProcessRecords rollback fail")
    void testProcessRecordsRollbackFail() throws ConsumerRecordSkipException {

        String topic = "file-notification-service--4g-event-enm2";
        InputMessage inputMessage=new InputMessage();

        inputMessage.setDataType("PM_CEL");
        inputMessage.setNodeName("RadioNode");
        inputMessage.setFileLocation("__dummy_file.bin.gz");
        ConsumerRecord<String, InputMessage> consumerRecord = new ConsumerRecord<>(topic, 1, 0, "1", inputMessage);
        Header header = new Header() {
            @Override

            public String key() {
                return "test";
            }
            @Override
            public byte[] value() {
                return new byte[0];
            }
        };
        consumerRecord.headers().add(header);
        InputMessage in = InputMessage.builder()
                .subSystemType("ENM2")
                .fileLocation("__dummy_CellTrace_DUL1_1.bin.gz")
                .build();
        headerMap.put("key", "value");
        InputMessage dummyInputMessage=new InputMessage();
        dummyInputMessage.setNodeName(headerMap.get("key"));
        Mockito.when(consumerRecord.value()).thenReturn(in);
        Mockito.lenient().when(inputTopicListener.constructInputMessage(headerMap,dummyInputMessage)).thenReturn(in);
        when(sftpServiceMock.downloadSftpFile(any(), any())).thenThrow(new ConsumerRecordRollbackException("sftp error", new RuntimeException("connection failed")));
        assertThrows(ConsumerRecordRollbackException.class, () -> inputTopicListener.processRecord(List.of(consumerRecord)));
    }


    @Test
    @DisplayName("test ProcessRecords skip success")
    void testProcessRecordsSkipFail() throws ConsumerRecordSkipException {

        String topic = "file-notification-service--4g-event-enm2";
        InputMessage inputMessage=new InputMessage();

        inputMessage.setDataType("PM_CEL");
        inputMessage.setNodeName("RadioNode");
        inputMessage.setFileLocation("__dummy_file.bin.gz");
        ConsumerRecord<String, InputMessage> consumerRecord = new ConsumerRecord<>(topic, 1, 0, "1", inputMessage);
        Header header = new Header() {
            @Override

            public String key() {
                return "test";
            }
            @Override
            public byte[] value() {
                return new byte[0];
            }
        };
        consumerRecord.headers().add(header);
        InputMessage in = InputMessage.builder()
                .subSystemType("ENM2")
                .fileLocation("__dummy_CellTrace_DUL1_1.bin.gz")
                .build();
        headerMap.put("key", "value");
        InputMessage dummyInputMessage=new InputMessage();
        dummyInputMessage.setNodeName(headerMap.get("key"));
        Mockito.when(consumerRecord.value()).thenReturn(in);
        Mockito.lenient().when(inputTopicListener.constructInputMessage(headerMap,dummyInputMessage)).thenReturn(in);
        when(sftpServiceMock.downloadSftpFile(any(), any())).thenThrow(new ConsumerRecordSkipException("sftp error", new RuntimeException("connection failed")));
        assertDoesNotThrow(() -> inputTopicListener.processRecord(List.of(consumerRecord)));
    }

    /**
     * Test constructInput
     */
    @Test
    @DisplayName("construct InputMessage")
    void constructInput() {

        InputMessage inputMessage = InputMessage.builder()
                .nodeName(headerMap.get("key"))
                .build();
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("key", "value");
        inputMessage = inputTopicListener.constructInputMessage(headerMap, inputMessage);
        Assert.assertNotNull(inputMessage);

    }

}