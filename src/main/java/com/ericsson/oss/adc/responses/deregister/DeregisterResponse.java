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

package com.ericsson.oss.adc.responses.deregister;

import com.ericsson.oss.adc.enums.DeregisterStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeregisterResponse {

    private DeregisterStatus deregisterStatus;
    private String message;

    /**
     * Build the response.
     *
     * @param failedInstanceList
     * @param passedInstanceList
     * @return A {@link DeregisterResponse}
     */
    public static DeregisterResponse build(List<String> passedInstanceList, List<String> failedInstanceList) {

        if (failedInstanceList.isEmpty()) {
            return builder().deregisterStatus(DeregisterStatus.SUCCESS)
                    .message("All data service instances are removed successfully")
                    .build();
        } else if (!passedInstanceList.isEmpty()) {
            return builder().deregisterStatus(DeregisterStatus.FAILURE)
                    .message("Data service instances are partially removed")
                    .build();
        } else {
            return builder().deregisterStatus(DeregisterStatus.FAILURE)
                    .message("Data service instances are not removed")
                    .build();
        }
    }

}
