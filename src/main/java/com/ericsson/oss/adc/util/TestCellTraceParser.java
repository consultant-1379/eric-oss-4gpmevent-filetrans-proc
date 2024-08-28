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

import com.ericsson.oss.mediation.parsers.exception.ParsingFailedException;
import com.ericsson.oss.mediation.parsers.parser.FileParser;
import com.ericsson.oss.mediation.parsers.receiver.DecodedEventReceiver;
import com.ericsson.oss.mediation.parsers.receiver.NullHeaderFooterReceivable;
import com.ericsson.oss.mediation.parsersapi.base.config.bean.SchemaEnum;
import com.ericsson.oss.mediation.parsersapi.base.config.bean.SchemaProviderType;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import static com.ericsson.oss.mediation.parsersapi.base.config.bean.DecodedEventType.POJO;

public class TestCellTraceParser {

    private TestCellTraceParser(){}

    public static long processFile(File file) throws ParsingFailedException {
        return decode(file, SchemaEnum.CELLTRACE);
    }

    private static long decode(final File inputFile, final SchemaEnum schemaEnum) throws ParsingFailedException {
        AtomicLong count = new AtomicLong(0);
        final FileParser fileParser = new FileParser(schemaEnum, SchemaProviderType.FILE_BASED_ON_DEMAND, new EventReceiver(count), POJO, new NullHeaderFooterReceivable());
        fileParser.execute(Arrays.asList(inputFile));
        return count.get();
    }

    private static class EventReceiver implements DecodedEventReceiver {
        final AtomicLong count;

        public EventReceiver(AtomicLong count) {
            this.count = count;
        }

        @Override
        public void decodedEventPublisher(final Object decodedEvent) {
            count.incrementAndGet();
        }
    }
}