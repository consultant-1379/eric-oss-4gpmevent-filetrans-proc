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

package com.ericsson.oss.adc.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@Component
@Getter
public class EventFileDownloadConfiguration {

    @Value("${eventFileDownload.numberOfEventFileDownloadRetries}")
    private int numberOfEventFileDownloadRetries;

    @Value("${eventFileDownload.sftpConnectionTimeoutMs}")
    private int sftpConnectionTimeoutMs;

    @Value("${eventFileDownload.sftpSessionTimeoutMs}")
    private int sftpSessionTimeoutMs;
}
