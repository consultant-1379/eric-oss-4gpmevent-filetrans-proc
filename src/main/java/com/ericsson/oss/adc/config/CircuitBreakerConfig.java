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

import lombok.Data;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Data
public class CircuitBreakerConfig {

    private int retryInterval;
    private int retryAttempts;
    private int circuitBreakerRetryAttempts;
    private int circuitBreakerResetTimeOut;
    private int circuitBreakerOpenTimeout;
    private int circuitBreakerBackoff;

    public int getRetryInterval() {
        return retryInterval;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public int getCircuitBreakerRetryAttempts() {
        return circuitBreakerRetryAttempts;
    }

    public int getCircuitBreakerResetTimeOut() {
        return circuitBreakerResetTimeOut;
    }

    public int getCircuitBreakerOpenTimeout() {
        return circuitBreakerOpenTimeout;
    }

    public int getCircuitBreakerBackoff() {
        return circuitBreakerBackoff;
    }
}
