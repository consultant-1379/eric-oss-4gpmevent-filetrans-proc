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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {CelltraceParserUtil.class})
class CelltraceParserUtilTest {

    @Test
    @DisplayName("checking for the byte array from input stream")
    void testGetByteArray() throws IOException {
        InputStream inputStream = Mockito.mock(InputStream.class);

        int dataLength = 10;
        byte[] expectedData = new byte[dataLength];
        when(inputStream.read(any(byte[].class), anyInt(), anyInt())).thenReturn(1);

        byte[] resultData = CelltraceParserUtil.getByteArray(inputStream, dataLength);
        assertArrayEquals(expectedData, resultData);
        verify(inputStream, times(dataLength)).read(any(byte[].class), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Throws IOException when .gz event file is corrupted")
    void testGetFileInputStreamWithCorruptedGzipFile() {
        String testResourceDir = "src/test/resources/test-gz-files/";
        String fileName = "corrupt.gz";
        String fileLocation = testResourceDir + fileName;
        File tempFile = new File(fileLocation);
        Assertions.assertThrows((IOException.class),() -> CelltraceParserUtil.getFileInputStream(tempFile, new FileInputStream(tempFile)));
    }

    @Test
    @DisplayName("Throws IOException when byte array length is negative")
    public void testGetByteArrayWithNegativeBytesRead() throws IOException {
        InputStream mockInputStream = mock(InputStream.class);
        int dataLength = 10;
        byte[] expectedData = new byte[dataLength];
        when(mockInputStream.read(any(byte[].class), anyInt(), anyInt())).thenReturn(-1);
        try {
            byte[] resultData = CelltraceParserUtil.getByteArray(mockInputStream, dataLength);

            fail("IOException should be thrown for negative bytesRead");
        } catch (IOException e) {
            assertEquals("error reading record, file is corrupt", e.getMessage());
        }
        verify(mockInputStream, atLeastOnce()).read(any(byte[].class), anyInt(), anyInt());
    }
}
