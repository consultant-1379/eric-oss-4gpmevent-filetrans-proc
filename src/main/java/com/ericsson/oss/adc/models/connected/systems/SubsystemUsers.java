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

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SubsystemUsers {
    private Long id;
    private Long connectionPropsId;

    public void setId(final Long id) {
        this.id = id;
    }

    public void setConnectionPropsId(final Long connectionPropsId) {
        this.connectionPropsId = connectionPropsId;
    }
}
