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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupportedPredicateParameter {
    private String parameterName = "nodeName";
    private boolean isPassedToConsumedService = true;

    public boolean getIsPassedToConsumedService() {
        return isPassedToConsumedService;
    }

    public void setIsPassedToConsumedService(boolean isPassedToConsumedService) {
        this.isPassedToConsumedService = isPassedToConsumedService;
    }
}
