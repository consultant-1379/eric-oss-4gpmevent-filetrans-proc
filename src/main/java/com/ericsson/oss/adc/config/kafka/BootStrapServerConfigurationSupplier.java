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

package com.ericsson.oss.adc.config.kafka;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;
@Slf4j
@Component
public class BootStrapServerConfigurationSupplier implements Supplier<String> {
    @Value("${spring.kafka.bootstrap-server}")
    private String messageBusAccessEndpoints;

    @Override
    public String get() {
        return messageBusAccessEndpoints;
    }


    @PostConstruct
    public void init() {
        log.info("Setting bootstrap server as: {}", messageBusAccessEndpoints);
    }
}
