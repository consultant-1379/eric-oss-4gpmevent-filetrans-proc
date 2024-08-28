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
package com.ericsson.oss.adc.models.connected.systems;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Subsystem {

    private Long id;
    private Long subsystemTypeId;
    private String name;
    private String url;
    private String healthCheckTime;
    private List<ConnectionProperties> connectionProperties;
    private String vendor;
    private SubsystemType subsystemType;
    private String adapterLink;

    public void setConnectionProperties(final List<ConnectionProperties> connectionProperties) {
        this.connectionProperties = new ArrayList<>(connectionProperties);
    }
}
