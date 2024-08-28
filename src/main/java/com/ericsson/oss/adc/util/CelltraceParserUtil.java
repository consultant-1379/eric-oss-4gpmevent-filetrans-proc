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

import com.ericsson.oss.mediation.parsers.handler.RecordDescriptor;
import com.ericsson.oss.mediation.parsers.handler.RecordSource;
import com.ericsson.oss.mediation.parsers.handler.SupportedEventsController;
import com.ericsson.oss.mediation.parsers.parser.ConfigHandler;
import com.ericsson.oss.mediation.parsers.parser.config.ConfigurationProvider;
import com.ericsson.oss.mediation.parsers.receiver.DecodedEventReceiver;
import com.ericsson.oss.mediation.parsers.receiver.HeaderFooterReceivable;
import com.ericsson.oss.mediation.parsers.receiver.NullHeaderFooterReceivable;
import com.ericsson.oss.mediation.parsers.recordbuilder.loading.LoadCelltrace;
import com.ericsson.oss.mediation.parsersapi.base.config.bean.DecodedEventType;
import com.ericsson.oss.mediation.parsersapi.base.config.bean.SchemaEnum;
import com.ericsson.oss.mediation.parsersapi.base.config.bean.SchemaProviderType;
import com.ericsson.oss.mediation.parsersapi.base.schema.handler.SchemaProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.zip.GZIPInputStream;

public class CelltraceParserUtil {
    private static final SchemaProvider schemaProvider = ConfigHandler.getSchemaProvider(SchemaProviderType.FILE_BASED, SchemaEnum.CELLTRACE);
    private static final String GZIP_FILE_EXTENSION = ".gz";

    private static final Logger LOG = LoggerFactory.getLogger(CelltraceParserUtil.class);

    private static final int BUFFER_SIZE = 65536;

    private CelltraceParserUtil() {
    }

    /**
     *
     * @param neFile
     * @param fileInputStream
     * @return
     * @throws IOException
     */
    public static InputStream getFileInputStream(File neFile, FileInputStream fileInputStream) throws IOException {
        if (neFile.getName().endsWith(GZIP_FILE_EXTENSION)) {
            LOG.debug("Returning inout gzip stream");
            return new GZIPInputStream(fileInputStream, BUFFER_SIZE);
        } else {
            LOG.debug("Returning inout stream");
            return new BufferedInputStream(fileInputStream, BUFFER_SIZE);
        }
    }

    /**
     *
     * @param neFileInputStream
     * @param dataLength
     * @return
     * @throws IOException
     */
    public static byte[] getByteArray(final InputStream neFileInputStream, final int dataLength) throws IOException {
        final byte[] data = new byte[dataLength];
        int dataPos;
        for (dataPos = 0; dataPos < dataLength; ) {
            final int bytesRead = neFileInputStream.read(data, dataPos, dataLength - dataPos);

            if (bytesRead <= 0) {
                throw new IOException("error reading record, file is corrupt");
            }
            dataPos += bytesRead;
        }
        return data;
    }

    /**
     *
     * @return
     */
    public static RecordDescriptor getCelltraceRecordDescriptor() {
        ConfigurationProvider configurationProvider = new ConfigurationProvider() {
            @Override
            public SchemaProviderType getSchemaProviderType() {
                return SchemaProviderType.FILE_BASED;
            }

            @Override
            public DecodedEventReceiver getDecodedEventReceiver() {
                return decodedEvent -> { };
            }

            @Override
            public DecodedEventType getDecodedEventType() {
                return DecodedEventType.POJO;
            }

            @Override
            public HeaderFooterReceivable getHeaderFooterReceivable() {
                return new NullHeaderFooterReceivable();
            }
        };

        LoadCelltrace loadCelltrace = new LoadCelltrace();
        return loadCelltrace.getRecordDescriptor(schemaProvider, RecordSource.FILE, "dummy-subnetwork", "en-US", new SupportedEventsController(), configurationProvider);
    }

}
