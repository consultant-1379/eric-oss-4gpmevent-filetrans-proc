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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.io.File;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter(AccessLevel.PUBLIC)
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder(toBuilder = true)
public class InputMessage {
    private String nodeName;
    private String nodeType;
    private String fileLocation;
    private String dataType;
    private File downloadedFile;
    private String subSystemType;
}
