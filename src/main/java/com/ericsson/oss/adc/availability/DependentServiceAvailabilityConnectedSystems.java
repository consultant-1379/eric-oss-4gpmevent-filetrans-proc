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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import static com.ericsson.oss.adc.util.StartupUtil.isOkResponseCode;

@Service
public class DependentServiceAvailabilityConnectedSystems extends DependentServiceAvailability {

    private static final Logger LOG = LoggerFactory.getLogger(DependentServiceAvailabilityConnectedSystems.class);

    private static final String HEALTH_PATH = "actuator/health";



    @Value("${connected.systems.base-url}:${connected.systems.port}/")
    private String connectedSystemsUrl;


    @Value("${connected.systems.availability.retry-interval}")
    private int csRetryInterval;
    @Value("${connected.systems.availability.retry-attempts}")
    private int csRetryAttempts;

    @Value("${connected.systems.availability.circuit-breaker-retry-attempts}")
    private int csCircuitBreakerRetryAttempts;

    @Value("${connected.systems.availability.circuit-breaker-reset-timeout}")
    private int csCircuitBreakerResetTimeOut;

    @Value("${connected.systems.availability.circuit-breaker-open-timeout}")
    private int csCircuitBreakerOpenTimeout;

    @Value("${connected.systems.availability.circuit-breaker-backoff}")
    private int csCircuitBreakerBackoff;

    @Autowired
    private RestTemplate restTemplate;

    public DependentServiceAvailabilityConnectedSystems(){
        super(true);
    }

    @Override
    boolean isServiceAvailable() throws UnsatisfiedExternalDependencyException {
        LOG.info("Checking if Connected Systems is reachable, GET request on: '{}'", connectedSystemsUrl);
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<Object> entity = new HttpEntity<>(headers);

        try {
            final ResponseEntity<String> response = restTemplate.exchange(
                    connectedSystemsUrl+HEALTH_PATH , HttpMethod.GET, entity, String.class);
            return isOkResponseCode(response);
        } catch (Exception e) {
            LOG.error("Connected Systems Unreachable, GET request on: '{}'", connectedSystemsUrl);
            throw new UnsatisfiedExternalDependencyException("Connected Systems Unreachable", e);
        }
    }
    @Override
    CircuitBreakerConfig setCircuitBreakerConfig(){
        CircuitBreakerConfig circuitBreakerConfig=new CircuitBreakerConfig();
        circuitBreakerConfig.setCircuitBreakerRetryAttempts(csCircuitBreakerRetryAttempts);
        circuitBreakerConfig.setCircuitBreakerResetTimeOut(csCircuitBreakerResetTimeOut);
        circuitBreakerConfig.setCircuitBreakerOpenTimeout(csCircuitBreakerOpenTimeout);
        circuitBreakerConfig.setCircuitBreakerBackoff(csCircuitBreakerBackoff);
        circuitBreakerConfig.setRetryAttempts(csRetryAttempts);
        circuitBreakerConfig.setRetryInterval(csRetryInterval);
        return circuitBreakerConfig;
    }
}
