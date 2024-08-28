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
package com.ericsson.oss.adc.models;

import lombok.Getter;
import lombok.Setter;

import static java.nio.charset.StandardCharsets.UTF_8;


public class EventHeader {

    @Getter
    private String dataVersion;
    @Getter
    private String nodeName;
    @Getter
    private long timestamp;

    @Getter
    @Setter
    private int eventId;

    @Getter
    private byte[] dataVersionBytes;
    @Getter

    private byte[] nodeNameBytes;


    public void setDataVersion(String dataVersion) {
        this.dataVersion = dataVersion;
        this.dataVersionBytes = dataVersion.getBytes(UTF_8);
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
        this.nodeNameBytes = nodeName.getBytes(UTF_8);

    }

}
