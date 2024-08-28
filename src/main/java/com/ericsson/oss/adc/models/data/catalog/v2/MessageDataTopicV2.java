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

package com.ericsson.oss.adc.models.data.catalog.v2;

import com.ericsson.oss.adc.models.DataProviderType;
import com.ericsson.oss.adc.models.MessageBus;
import com.ericsson.oss.adc.models.MessageStatusTopic;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageDataTopicV2 {
    private static final long serialVersionUID = 1;
    private String name;
    private DataProviderType dataProviderType;
    private MessageBus messageBus;
    private MessageStatusTopic messageStatusTopic;

}
