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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "dmm.data-catalog")
@NoArgsConstructor
@Getter
@Setter
public class DataCatalogProperties {

    private String messageBusName;
    private String messageBusNameSpace;
    private String dataSpace;
    private String dataProviderType;
    private String dataProviderTypeVersion;
    private String dataProviderTypeId;
    private String dataCategory;
    private String dataCollectorName;
    private MessageEncoding encoding;
    private String specificationReference;
    private String dataServiceName;
}