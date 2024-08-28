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

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataType {
    private Integer id;
    private String mediumType = "stream";
    private String schemaName = "4g-pm-event_raw_schema";
    private String schemaVersion = "1";
    private boolean isExternal = false;
    private String consumedDataProvider = "";
    private String consumedDataSpace = "";
    private String consumedDataCategory = "";
    private String consumedSchemaName = "";
    private String consumedSchemaVersion = "";

    public boolean getIsExternal() {
        return isExternal;
    }

    public void setIsExternal(boolean isExternal) {
        this.isExternal = isExternal;
    }
}
