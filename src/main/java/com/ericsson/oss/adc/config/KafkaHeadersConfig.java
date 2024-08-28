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
package com.ericsson.oss.adc.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * The supported KafkaHeader sent per message.
 */
@Configuration
public class KafkaHeadersConfig {

    @Value("${kafka.header-event.version}")
    public int kafkaEventHeaderVersion;
    /**
     * Represents the ID of the event, a {@link Long} value
     */
    public static final String EVENT_ID = "eventId";
    public static final String NODE_NAME = "nodeName";
    public static final String TIME_STAMP = "ts";
    public static final String DATA_VERSION = "dataVersion";
    public static final String VERSION = "version";


}
