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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageBus {
    private static final long serialVersionUID = 1;
    private Long id;
    private String name;
    private String clusterName;
    private String nameSpace;
    private ArrayList<String> accessEndpoints;
    private ArrayList<Long> notificationTopicIds;
    private ArrayList<Long> messageStatusTopicIds;
    private ArrayList<Long> messageDataTopicIds;
}
