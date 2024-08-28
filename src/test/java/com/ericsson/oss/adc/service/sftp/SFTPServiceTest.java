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

package com.ericsson.oss.adc.service.sftp;

import com.ericsson.oss.adc.availability.UnsatisfiedExternalDependencyException;
import com.ericsson.oss.adc.config.EventFileDownloadConfiguration;
import com.ericsson.oss.adc.models.InputMessage;
import com.ericsson.oss.adc.models.connected.systems.ConnectionProperties;
import com.ericsson.oss.adc.models.connected.systems.Subsystem;
import com.ericsson.oss.adc.models.connected.systems.SubsystemType;
import com.ericsson.oss.adc.models.connected.systems.SubsystemUsers;
import com.ericsson.oss.adc.service.connected.systems.ConnectedSystemsService;
import com.ericsson.oss.adc.service.sftp.spring.EnmSftpConfig;
import com.ericsson.oss.adc.service.sftp.spring.EnmSftpService;
import com.ericsson.oss.adc.service.sftp.spring.EnmSftpSessionFactoryLocator;
import com.ericsson.oss.adc.util.ConsumerRecordRollbackException;
import com.ericsson.oss.adc.util.FileTransferMetricsUtil;
import com.ericsson.oss.adc.util.ConsumerRecordSkipException;
import com.ericsson.oss.adc.util.RetryUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.retry.support.RetryTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static com.github.stefanbirkner.fakesftpserver.lambda.FakeSftpServer.withSftpServer;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {EnmSftpService.class, EnmSftpConfig.class, EnmSftpSessionFactoryLocator.class, SFTPService.class, EventFileDownloadConfiguration.class, SFTPServiceTest.class, ConnectedSystemsService.class, RetryUtil.class})
@AutoConfigureWebClient(registerRestTemplate = true)
@EnableAutoConfiguration
public class SFTPServiceTest {

    private static final int PORT = 5678;
    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private static final String ENM_FILE_PATH = "/ericsson/pmic1/CELLTRACE/SubNetwork=ONRM_ROOT_MO_R,SubNetwork=4G,MeContext=NE00000650,ManagedElement=NE00000650/";
    private static final String FILE_NAME = "A20181121.0645+0000-0700+0000_SubNetwork=4GMeContext=NE12345_CellTrace_DUL1_1.bin.gz";

    private final String NODE_NAME = "SubNetwork=ONRM_ROOT_MO_R,SubNetwork=4G,MeContext=NE00000650,ManagedElement=NE00000650";
    private final String NODE_TYPE = "RadioNode";
    private final String DATA_TYPE = "PM_CELLTRACE";
    private final SubsystemUsers subsystemUsers1 = new SubsystemUsers(4L, 3L);
    private final SubsystemUsers subsystemUsers2 = new SubsystemUsers(15L, 3L);
    private final SubsystemUsers subsystemUsers3 = new SubsystemUsers(2L, 3L);
    private final SubsystemType subsystemType = new SubsystemType(null, "PhysicalDevice");
    private final String enm = "enm";
    @Value("${dmm.data-catalog.availability.retry-interval}")
    private int retryInterval;

    @Value("${dmm.data-catalog.availability.retry-attempts}")
    private int retryAttempts;

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
    private final Map subSystemMapEmpty = Collections.EMPTY_MAP;

    @Value("${temp-directory}")
    private String tempDirectory;

    @SpyBean
    private RetryUtil retryUtil;
    @MockBean
    private ConnectedSystemsService connectedSystemsService;

    @Autowired
    private EventFileDownloadConfiguration eventFileDownloadConfiguration;

    @SpyBean
    private SFTPService sftpService;

    @SpyBean
    private SftpRemoteFileTemplate sftpRemoteFileTemplate;

    @MockBean
    private FileTransferMetricsUtil metricsUtil;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @Order(1)
    @DisplayName("Verify successful SFTP connection")
    public void test_setupSFTPConnectionVerifySuccessfulServerConnection() throws Exception {
        withSftpServer(server -> {
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            when(connectedSystemsService.getSubsystemDetails()).thenReturn(subSystemMapHappy);
            server.deleteAllFilesAndDirectories();
            RetryTemplate retryTemplate = retryUtil.retryTemplate(retryAttempts, retryInterval);
            Mockito.when(retryUtil.retryTemplate(retryAttempts, retryInterval)).thenReturn(retryTemplate);
            assertTrue(sftpService.checkSubsystemsConnections());
        });
    }


    @Test
    @Order(2)
    @DisplayName("Verify unsuccessful SFTP connection")
    public void test_setupSFTPConnectionVerifyUnsuccessfulServerConnection() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD + 1);
            when(connectedSystemsService.getSubsystemDetails()).thenReturn(subSystemMapHappy);
            server.deleteAllFilesAndDirectories();
            RetryTemplate retryTemplate = retryUtil.retryTemplate(retryAttempts, retryInterval);
            Mockito.when(retryUtil.retryTemplate(retryAttempts, retryInterval)).thenReturn(retryTemplate);
            assertDoesNotThrow(() -> sftpService.checkSubsystemsConnections());
        });
    }

    @Test
    @Order(3)
    @DisplayName("Verify unsuccessful SFTP connection based on empty connected systems response")
    public void test_setupSFTPConnectionAndEmptyConnectedSystemsResponseVerifyNoConnectionToSftpServer() throws Exception {
        withSftpServer(server -> {
            when(connectedSystemsService.getSubsystemDetails()).thenReturn(null);
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            assertFalse(sftpService.checkSubsystemsConnections());
        });
    }

    @Test
    @Order(4)
    @DisplayName("Should throw ConnectException when empty connected systems response is returned")
    public void test_setUpSFTPConnectionAndDownloadFile_connectException() throws Exception {
        withSftpServer(server -> {
            when(connectedSystemsService.getSubsystemDetails()).thenReturn(subSystemMapEmpty);

            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);

            final InputMessage inputMessagesToAttemptDownloading = new InputMessage();
            inputMessagesToAttemptDownloading.setNodeName(NODE_NAME);
            inputMessagesToAttemptDownloading.setNodeType(NODE_TYPE);
            final String remoteFilePath = ENM_FILE_PATH + FILE_NAME;
            inputMessagesToAttemptDownloading.setFileLocation(remoteFilePath);
            inputMessagesToAttemptDownloading.setDataType(DATA_TYPE);

            server.putFile(remoteFilePath, "Content of file", UTF_8);
            assertThrows(ConsumerRecordRollbackException.class, () -> sftpService.downloadSftpFile(inputMessagesToAttemptDownloading, "enm2"));
        });
    }


    @Test
    @Order(5)
    @DisplayName("Should fetch SubsystemDetails only once, establish SFTP connection and successfully download a single batch, single file")
    public void test_getSubsystemDetailsSetUpSingleSFTPConnectionAndDownloadFiles() throws Exception {
        withSftpServer(server -> {
            when(connectedSystemsService.getSubsystemDetails()).thenReturn(subSystemMapHappy);

            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);

            final InputMessage inputMessagesToAttemptDownloading = new InputMessage();
            inputMessagesToAttemptDownloading.setNodeName(NODE_NAME);
            inputMessagesToAttemptDownloading.setNodeType(NODE_TYPE);
            final String remoteFilePath = ENM_FILE_PATH + FILE_NAME;
            inputMessagesToAttemptDownloading.setFileLocation(remoteFilePath);
            inputMessagesToAttemptDownloading.setDataType(DATA_TYPE);

            server.putFile(remoteFilePath, "Content of file", UTF_8);

            // Called twice to assert that 1 REST call is made at first setUpConnectionToSFTPServerUsingConnectedSystemsResponse attempt where
            // getSubsystemDetails is called once and then stored. Prevents unneeded REST calls for each setUpSFTPConnectionAndDownloadFile call
            assertFalse(sftpService.downloadSftpFile(inputMessagesToAttemptDownloading, enm) == null);
            assertFalse(sftpService.downloadSftpFile(inputMessagesToAttemptDownloading, enm) == null);

            //it should not be invoked as this is already initialized with the previous test
            verify(connectedSystemsService, times(0)).getSubsystemDetails();
            verify(sftpService, times(2)).downloadSftpFile(inputMessagesToAttemptDownloading, enm);

            // cleanup
            Path expectedDownloadedFilePath = new File(tempDirectory + "/" + FILE_NAME).toPath();
            Files.delete(expectedDownloadedFilePath);
            assertThat(expectedDownloadedFilePath).doesNotExist();
        });
    }

    @Test
    @Order(6)
    @DisplayName("Verify successful single batch, single file download verifying server connection")
    public void test_SetupSFTPConnectionVerifyServerConnectionAndSingleFileDownloadedSuccessfully() throws Exception {
        withSftpServer(server -> {
            when(connectedSystemsService.getSubsystemDetails()).thenReturn(subSystemMapHappy);

            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);

            final InputMessage inputMessage = new InputMessage();
            inputMessage.setNodeName(NODE_NAME);
            inputMessage.setNodeType(NODE_TYPE);
            final String remoteFilePath = ENM_FILE_PATH + FILE_NAME;
            inputMessage.setFileLocation(remoteFilePath);
            inputMessage.setDataType(DATA_TYPE);
            server.putFile(remoteFilePath, "Content of file", UTF_8);

            final InputMessage downloadedInputMessage = sftpService.downloadSftpFile(inputMessage, enm);
            assertNotNull(downloadedInputMessage.getDownloadedFile());

            //it should not be invoked as this is already initialized with the previous test
            verify(connectedSystemsService, times(0)).getSubsystemDetails();

            // cleanup
            Path expectedDownloadedFilePath = new File(tempDirectory + "/" + FILE_NAME).toPath();
            Files.delete(expectedDownloadedFilePath);
            assertThat(expectedDownloadedFilePath).doesNotExist();
        });
    }

    @Test
    @Order(7)
    @DisplayName("Verify successful reconnect post connection to server is lost")
    public void test_download_files() throws Exception {
        when(connectedSystemsService.getSubsystemDetails()).thenReturn(subSystemMapHappy);
        final InputMessage inputMessage = new InputMessage();
        inputMessage.setNodeName(NODE_NAME);
        inputMessage.setNodeType(NODE_TYPE);

        final String remoteFilePath = ENM_FILE_PATH + FILE_NAME;
        inputMessage.setFileLocation(remoteFilePath);
        inputMessage.setDataType(DATA_TYPE);

        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            server.putFile(remoteFilePath, "Content of file", UTF_8);

            final InputMessage downloadedInputMessage = sftpService.downloadSftpFile(inputMessage, enm);
            assertNotNull(downloadedInputMessage.getDownloadedFile());

            //it should not be invoked as this is already initialized with the previous test
            verify(connectedSystemsService, times(0)).getSubsystemDetails();

            // cleanup
            Path expectedDownloadedFilePath = new File(tempDirectory + "/" + FILE_NAME).toPath();
            Files.delete(expectedDownloadedFilePath);
            assertThat(expectedDownloadedFilePath).doesNotExist();
        });

        Mockito.clearInvocations(sftpRemoteFileTemplate);
        assertThrows(ConsumerRecordRollbackException.class, () -> sftpService.downloadSftpFile(inputMessage, enm));
        verify(sftpRemoteFileTemplate, times(3)).get(anyString(), any());
        //verify - refresh enm connection has been invoked post failure
        verify(connectedSystemsService, times(1)).getSubsystemDetails();

        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            server.putFile(remoteFilePath, "Content of file", UTF_8);

            final InputMessage downloadedInputMessage = sftpService.downloadSftpFile(inputMessage, enm);
            assertNotNull(downloadedInputMessage.getDownloadedFile());

            //it should not be invoked as this is already initialized with the previous test
            verify(connectedSystemsService, times(1)).getSubsystemDetails();

            // cleanup
            Path expectedDownloadedFilePath = new File(tempDirectory + "/" + FILE_NAME).toPath();
            Files.delete(expectedDownloadedFilePath);
            assertThat(expectedDownloadedFilePath).doesNotExist();
        });
    }

    @Test
    @Order(8)
    @DisplayName("Verify checkSubsystemDetails failed")
    public void test_checkSubsystemDetails_failed() throws Exception {
        when(connectedSystemsService.getSubsystemDetails()).thenReturn(null);
        assertThrows(UnsatisfiedExternalDependencyException.class, () -> sftpService.checkSubsystemDetails());

    }

    @Test
    @Order(9)
    @DisplayName("Verify checkSubsystemDetails succeeded")
    public void test_checkSubsystemDetails_succeeded() throws Exception {
        when(connectedSystemsService.getSubsystemDetails()).thenReturn(subSystemMapHappy);
        assertTrue(sftpService.checkSubsystemDetails());

    }

    @Test
    @Order(10)
    @DisplayName("Verify concurrent requests succeeded")
    public void test_concurrent_requests_succeeded() throws Exception {
        when(connectedSystemsService.getSubsystemDetails()).thenReturn(subSystemMapHappy);

        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            List<CompletableFuture<Integer>> allFutures = new ArrayList<>();
            final int MAX_ITERATIONS = 100;

            IntStream.range(1, MAX_ITERATIONS).forEach(i -> {
                try {
                    final String remoteFilePath = ENM_FILE_PATH + i + "_" + FILE_NAME;
                    server.putFile(remoteFilePath, "Content of file", UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            IntStream.range(1, MAX_ITERATIONS).forEach(i -> {
                CompletableFuture cf = CompletableFuture.supplyAsync(() -> {
                    final InputMessage inputMessage = new InputMessage();
                    inputMessage.setNodeName(NODE_NAME);
                    inputMessage.setNodeType(NODE_TYPE);
                    inputMessage.setDataType(DATA_TYPE);

                    final String remoteFilePath = ENM_FILE_PATH + i + "_" + FILE_NAME;
                    inputMessage.setFileLocation(remoteFilePath);
                    try {
                        return sftpService.downloadSftpFile(inputMessage, enm);
                    } catch (ConsumerRecordRollbackException | ConsumerRecordSkipException e) {
                        throw new RuntimeException(e);
                    }
                });
                allFutures.add(cf);
            });

            CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).join();
            AtomicInteger downloadFilesCount = new AtomicInteger(0);

            IntStream.range(1, MAX_ITERATIONS).forEach(i -> {
                try {
                    //check file exist and delete it
                    Path expectedDownloadedFilePath = new File(tempDirectory + "/" + i + "_" + FILE_NAME).toPath();
                    Files.delete(expectedDownloadedFilePath);
                    assertThat(expectedDownloadedFilePath).doesNotExist();
                    downloadFilesCount.incrementAndGet();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            assertEquals(MAX_ITERATIONS - 1, downloadFilesCount.get(), "concurrent download failed something went wrong");
        });
    }

    @Test
    @Order(11)
    @DisplayName("Verify unsuccessful single batch, single file download with null attached when not found on server")
    public void test_setupSFTPConnectionVerifyServerConnectionAndSingleFileDownloadedFail() throws Exception {
        when(connectedSystemsService.getSubsystemDetails()).thenReturn(subSystemMapHappy);
        final InputMessage inputMessage = new InputMessage();
        inputMessage.setNodeName(NODE_NAME);
        inputMessage.setNodeType(NODE_TYPE);

        final String remoteFilePath = ENM_FILE_PATH + FILE_NAME;
        inputMessage.setFileLocation(remoteFilePath);
        inputMessage.setDataType(DATA_TYPE);

        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            server.putFile(remoteFilePath + 1, "Content of file", UTF_8);
            Mockito.clearInvocations(sftpRemoteFileTemplate);
            assertThrows(ConsumerRecordSkipException.class, () -> sftpService.downloadSftpFile(inputMessage, enm));
            //verify - file not found is retried only once
            verify(sftpRemoteFileTemplate, times(1)).get(anyString(), any());
        });
    }

    @Test
    @Order(13)
    @DisplayName("Verify successful SFTP connection for enm")
    public void test_setupSFTPConnectionVerifySuccessfulServerConnectionForEnm() throws Exception {
        withSftpServer(server -> {
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            when(connectedSystemsService.getSubsystemDetails()).thenReturn(subSystemMapHappy);
            server.deleteAllFilesAndDirectories();
            RetryTemplate retryTemplate = retryUtil.retryTemplate(retryAttempts, retryInterval);
            Mockito.when(retryUtil.retryTemplate(retryAttempts, retryInterval)).thenReturn(retryTemplate);
            assertTrue(sftpService.checkSubsystemsConnection(enm));
        });
    }
}
