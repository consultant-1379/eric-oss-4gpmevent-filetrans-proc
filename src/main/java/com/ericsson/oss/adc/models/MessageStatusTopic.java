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

import com.ericsson.oss.adc.enums.MessageEncoding;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageStatusTopic {
    private String name;
    private Long messageBusId;
    private String specificationReference = "";
    private MessageEncoding encoding = MessageEncoding.JSON;

    public MessageStatusTopic(final String name, final Long messageBusId) {
        this.name = name;
        this.messageBusId = messageBusId;
    }
}
