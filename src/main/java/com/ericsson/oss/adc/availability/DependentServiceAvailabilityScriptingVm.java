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

package com.ericsson.oss.adc.availability;

import com.ericsson.oss.adc.config.CircuitBreakerConfig;
import com.ericsson.oss.adc.service.sftp.SFTPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class DependentServiceAvailabilityScriptingVm extends DependentServiceAvailability {

    private static final Logger LOG = LoggerFactory.getLogger(DependentServiceAvailabilityScriptingVm.class);

    @Autowired
    private SFTPService sftpService;

    @Value("${scriptingVM.availability.retry-interval}")
    private int scriptingVMRetryInterval;

    @Value("${scriptingVM.availability.retry-attempts}")
    private int scriptingVMRetryAttempts;

    @Value("${scriptingVM.availability.circuit-breaker-retry-attempts}")
    private int scriptingVMCircuitBreakerRetryAttempts;

    @Value("${scriptingVM.availability.circuit-breaker-reset-timeout}")
    private int scriptingVMCircuitBreakerResetTimeOut;

    @Value("${scriptingVM.availability.circuit-breaker-open-timeout}")
    private int scriptingVMCircuitBreakerOpenTimeout;

    @Value("${scriptingVM.availability.circuit-breaker-backoff}")
    private int scriptingVMCircuitBreakerBackoff;

    public DependentServiceAvailabilityScriptingVm(){super(true);}

    @Override
    boolean isServiceAvailable() throws UnsatisfiedExternalDependencyException {
        boolean sftpConnectionIsEstablished = sftpService.checkSubsystemsConnections();

        if (!sftpConnectionIsEstablished) {
            LOG.error("SFTP service is not reachable");
            throw new UnsatisfiedExternalDependencyException("SFTP service is not reachable");
        }
        LOG.info("SFTP service is reachable");
        return true;
    }
    @Override
    CircuitBreakerConfig setCircuitBreakerConfig(){
        CircuitBreakerConfig circuitBreakerConfig=new CircuitBreakerConfig();
        circuitBreakerConfig.setCircuitBreakerRetryAttempts(scriptingVMCircuitBreakerRetryAttempts);
        circuitBreakerConfig.setCircuitBreakerResetTimeOut(scriptingVMCircuitBreakerResetTimeOut);
        circuitBreakerConfig.setCircuitBreakerOpenTimeout(scriptingVMCircuitBreakerOpenTimeout);
        circuitBreakerConfig.setCircuitBreakerBackoff(scriptingVMCircuitBreakerBackoff);
        circuitBreakerConfig.setRetryAttempts(scriptingVMRetryAttempts);
        circuitBreakerConfig.setRetryInterval(scriptingVMRetryInterval);
        return circuitBreakerConfig;

    }
}