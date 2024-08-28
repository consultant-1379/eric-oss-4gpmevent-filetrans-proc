/*******************************************************************************
 * COPYRIGHT Ericsson 2022
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

import com.ericsson.oss.adc.models.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Data
@Builder
public class MessageSchemaPutRequest {
    private Integer id;
    private DataSpace dataSpace;
    private DataServiceForMessageSchemaPut dataService;
    private DataServiceInstance dataServiceInstance;
    private DataCategory dataCategory;
    private DataProviderTypeForMessageSchemaPUT dataProviderType;
    private MessageStatusTopic messageStatusTopic;
    private MessageDataTopic messageDataTopic;
    private DataType dataType;
    private SupportedPredicateParameter supportedPredicateParameter;
    private MessageSchema messageSchema;
}
