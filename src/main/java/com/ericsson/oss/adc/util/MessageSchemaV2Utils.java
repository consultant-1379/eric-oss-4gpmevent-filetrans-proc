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

import com.ericsson.oss.adc.models.DataProviderType;
import com.ericsson.oss.adc.models.DataSpace;
import com.ericsson.oss.adc.models.data.catalog.v2.MessageDataTopicV2;
import com.ericsson.oss.adc.models.data.catalog.v2.MessageSchemaV2;

import java.util.Optional;

/**
 * Implementation of utility class to verify the present of objects and sub-objects received from Data Management and Movement (DM&M) Data Catalog service.
 * This generally begins by traversing from the root object and verifying a sub-object and or verifying the presence of a sub-object and its values.
 */
public class MessageSchemaV2Utils {

    private MessageSchemaV2Utils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Checks if there is a {@link DataSpace} with a name by traversing the objects to ensure the object tree exists from {@link MessageSchemaV2} root
     *
     * @param messageSchema the {@link MessageSchemaV2} root object to traverse from.
     * @return true if the {@link DataSpace} name is present, false otherwise.
     */
    public static boolean canGetDataSpaceName(MessageSchemaV2 messageSchema) {
        return Optional.of(messageSchema)
                .map(MessageSchemaV2::getMessageDataTopic)
                .map(MessageDataTopicV2::getDataProviderType)
                .map(DataProviderType::getDataSpace)
                .map(DataSpace::getName)
                .isPresent();
    }
    /**
     * Checks if there is a {@link MessageDataTopicV2} with a name (topic name) by traversing the objects to ensure the object tree exists from {@link MessageSchemaV2} root
     *
     * @param messageSchema the {@link MessageSchemaV2} root object to traverse from.
     * @return true if the {@link MessageDataTopicV2} name is present, false otherwise.
     */
    public static boolean canGetTopicName(MessageSchemaV2 messageSchema) {
        return Optional.of(messageSchema)
                .map(MessageSchemaV2::getMessageDataTopic)
                .map(MessageDataTopicV2::getName)
                .isPresent();
    }
}


