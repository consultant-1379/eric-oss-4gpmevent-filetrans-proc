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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {FileProcessorService.class, SimpleMeterRegistry.class})
class FileProcessorServiceTest {

    @Value("${temp-directory}")
    private String tempDirectory;

    @Autowired
    private FileProcessorService fileProcessorService;

    @MockBean
    private CelltraceFileParser celltraceFileParserMock;

    @Autowired
    CelltraceFileParser celltraceFileParser;

    @MockBean
    private FileTransferMetricsUtil metricsUtil;

    @MockBean
    private EventFileProcessMetrics eventFileProcessMetrics;

    @Test
    @DisplayName("Should successfully process the event file and and not throw an Exception")
    void test_processEventFile() throws Exception {
        String testResourceDir = "src/test/resources/test-gz-files/";
        String fileName = "A20181121.0645+0000-0700+0000_SubNetwork=4GMeContext=NE12345_CellTrace_DUL1_1.bin.gz";
        String fileLocation = testResourceDir + fileName;
        String nodeName = "4GTestNode";
        String copyFileName = "copy-A20181121.0645+0000-0700+0000_SubNetwork=4GMeContext=NE12345_CellTrace_DUL1_1.bin.gz";
        Path originalPath = Paths.get(fileLocation);
        Path copyPath = Paths.get(tempDirectory + copyFileName);
        Files.copy(originalPath, copyPath, StandardCopyOption.REPLACE_EXISTING);
        File copyEventFile = copyPath.toFile();
        when(celltraceFileParserMock.processCelltrace(any(),any())).thenReturn(eventFileProcessMetrics);
        fileProcessorService.processEventFile(copyEventFile, nodeName);
        verify(celltraceFileParserMock, times(1)).processCelltrace(any(), eq(nodeName));
        assertFalse(copyEventFile.exists());
    }

    @Test
    @DisplayName("Should throw FileNotFoundException when asked to process non-existent event file")
    void test_processEventFile_FileNotFoundException() {
        String testResourceDir = "src/test/resources/test-event-files/";
        String fileName = "fake-file.bin.gz";
        String fileLocation = testResourceDir + fileName;
        String nodeName = "4GTestNode";
        Path eventFilePath = Paths.get(fileLocation);
        File eventFile = eventFilePath.toFile();
        assertThrows(ConsumerRecordRollbackException.class, () -> fileProcessorService.processEventFile(eventFile, nodeName));
        assertFalse(Files.exists(eventFilePath), "Event file should not exist.");
        assertEquals(0, eventFile.length(), "Event file should be empty.");
    }

    @Test
    @DisplayName("Should throw IOException when asked to process non-existent event file")
    void test_processEventFile_ioException() {
        String testResourceDir = "src/test/resources/test-gz-files/";
        String fileName = "corrupt1.gz";
        String fileLocation = testResourceDir + fileName;
        String nodeName = "4GTestNode";
        Path eventFilePath = Paths.get(fileLocation);
        File eventFile = eventFilePath.toFile();
        assertThrows(ConsumerRecordRollbackException.class, () -> fileProcessorService.processEventFile(eventFile, nodeName));

    }

    @Test
    @DisplayName("Should successfully perform the clean up")
    void test_cleanUp() throws IOException {

        String testResourceDir = "src/test/resources/test-gz-files/";
        String compressedFileName = "A20181121.0645+0000-0700+0000_SubNetwork=4GMeContext=NE12345_CellTrace_DUL1_1.bin.gz";
        String copyCompressedFileName = "copy-A20181121.0645+0000-0700+0000_SubNetwork=4GMeContext=NE12345_CellTrace_DUL1_1.bin.gz";
        Path originalCompressedPath = Paths.get(testResourceDir + compressedFileName);
        Path copyCompressedPath = Paths.get(testResourceDir + copyCompressedFileName);
        Files.copy(originalCompressedPath, copyCompressedPath, StandardCopyOption.REPLACE_EXISTING);
        fileProcessorService.cleanUp(copyCompressedPath.toString());
        assertThat(copyCompressedPath).doesNotExist();

    }

    @Test
    @DisplayName("Should catch FileNotFound exception if file does not exists")
    void test_cleanUp_neitherFileExists() {
        String testRestDir = "/src/test/resources/test-gz-files/";
        String nonExistingCompressed = "nothere.gpb.gz";
        assertDoesNotThrow(() -> fileProcessorService.cleanUp(testRestDir + nonExistingCompressed));
    }

    @Test
    @DisplayName("Should catch exception when path is null")
    void test_cleanUp_bothNull() {
        String pathNull = null;
        assertDoesNotThrow(() -> fileProcessorService.cleanUp(pathNull));

    }
    @Test
    @DisplayName("Checking IOException for processCelltrace method")
    void test_IOException() throws LoadingFailedException,IOException {
        String testResourceDir = "src/test/resources/test-gz-files/";
        String fileName = "A20181121.0645+0000-0700+0000_SubNetwork=4GMeContext=NE12345_CellTrace_DUL1_1.bin";
        String fileLocation = testResourceDir + fileName;
        String nodeName = "4GTestNode";
        assertThrows(ConsumerRecordRollbackException.class, () -> fileProcessorService.processEventFile(new File(fileLocation),nodeName));
    }

    @Test
    @DisplayName("Checking LoadingFailedException for processCelltrace method")
    void test_LoadingFailedException() throws LoadingFailedException,IOException {
        String testResourceDir = "src/test/resources/test-gz-files/";
        String compressedFileName = "A20181121.0645+0000-0700+0000_SubNetwork=4GMeContext=NE12345_CellTrace_DUL1_1.bin.gz";
        String copyCompressedFileName = "copy-A20181121.0645+0000-0700+0000_SubNetwork=4GMeContext=NE12345_CellTrace_DUL1_1.bin.gz";
        String nodeName = "4GTestNode";
        Path originalCompressedPath = Paths.get(testResourceDir + compressedFileName);
        Path copyCompressedPath = Paths.get(testResourceDir + copyCompressedFileName);
        Files.copy(originalCompressedPath, copyCompressedPath, StandardCopyOption.REPLACE_EXISTING);
        Mockito.doThrow(new LoadingFailedException("Simulated LoadingFailedException"))
                .when(celltraceFileParser).processCelltrace(Mockito.anyString(), Mockito.anyString());
        Assert.assertThrows(ConsumerRecordSkipException.class, () -> fileProcessorService.processEventFile(copyCompressedPath.toFile(), nodeName));

    }


    @Test
    @DisplayName("Checking RuntimeExcption for processCelltrace method")
    void test_RuntimeException() throws IOException, LoadingFailedException {
        String testResourceDir = "src/test/resources/test-gz-files/";
        String compressedFileName = "A20181121.0645+0000-0700+0000_SubNetwork=4GMeContext=NE12345_CellTrace_DUL1_1.bin.gz";
        String copyCompressedFileName = "copy-A20181121.0645+0000-0700+0000_SubNetwork=4GMeContext=NE12345_CellTrace_DUL1_1.bin.gz";
        String nodeName = "4GTestNode";
        Path originalCompressedPath = Paths.get(testResourceDir + compressedFileName);
        Path copyCompressedPath = Paths.get(testResourceDir + copyCompressedFileName);
        Files.copy(originalCompressedPath, copyCompressedPath, StandardCopyOption.REPLACE_EXISTING);
        Mockito.doThrow(new RuntimeException("Simulated Parsing Failed Exception"))
                .when(celltraceFileParser).processCelltrace(Mockito.anyString(), Mockito.anyString());
        Assert.assertThrows(ConsumerRecordSkipException.class, () -> fileProcessorService.processEventFile(copyCompressedPath.toFile(), nodeName));

    }
}
