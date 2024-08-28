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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataServiceInstance {
	private String dataServiceInstanceName;
	private String consumedDataSpace = "4G";
	private String consumedDataCategory = "PM_EVENTS";
	private String consumedDataProvider;
	private String controlEndPoint = "";
	private String consumedSchemaName = "FLS";
	private String consumedSchemaVersion = "1";

	public DataServiceInstance(final String dataServiceInstanceName, final String consumedDataProvider) {
		this.dataServiceInstanceName = dataServiceInstanceName;
		this.consumedDataProvider = consumedDataProvider;
	}
}
