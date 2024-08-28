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
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import static com.ericsson.oss.adc.util.StartupUtil.addTrailingSlash;
import static com.ericsson.oss.adc.util.StartupUtil.isOkResponseCode;

@Component
public class DependentServiceAvailabilityDataCatalog extends DependentServiceAvailability {

    private static final Logger LOG = LoggerFactory.getLogger(DependentServiceAvailabilityDataCatalog.class);
    private static final String HEALTH_PATH = "actuator/health";

    @Value("${dmm.data-catalog.base-url}")
    private String dataCatalogUrl;

    @Value("${dmm.data-catalog.base-port}")
    private String dataCatalogPort;

    @Value("${dmm.data-catalog.availability.retry-interval}")
    private int dcRetryInterval;

    @Value("${dmm.data-catalog.availability.retry-attempts}")
    private int dcRetryAttempts;

    @Value("${dmm.data-catalog.availability.circuit-breaker-retry-attempts}")
    private int dcCircuitBreakerRetryAttempts;

    @Value("${dmm.data-catalog.availability.circuit-breaker-reset-timeout}")
    private int dcCircuitBreakerResetTimeOut;

    @Value("${dmm.data-catalog.availability.circuit-breaker-open-timeout}")
    private int dcCircuitBreakerOpenTimeout;

    @Value("${dmm.data-catalog.availability.circuit-breaker-backoff}")
    private int dcCircuitBreakerBackoff;

    @Autowired
    private RestTemplate restTemplate;

    public DependentServiceAvailabilityDataCatalog(){super(true);}
    @Override
    boolean isServiceAvailable()  throws UnsatisfiedExternalDependencyException {
        LOG.info("Checking if Data Catalog is reachable, GET request on: '{}'", dataCatalogUrl);
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<Object> entity = new HttpEntity<>(headers);

        try {
            final ResponseEntity<String> response = restTemplate.exchange(dataCatalogUrl + addTrailingSlash(dataCatalogPort) + HEALTH_PATH,
                    HttpMethod.GET, entity,
                    String.class);
            return isOkResponseCode(response);
        } catch (Exception e) {
            LOG.error("Data Catalog Unreachable, GET request on: '{}'", dataCatalogUrl);
            throw new UnsatisfiedExternalDependencyException("Data Catalog Unreachable", e);
        }
    }

    @Override
    CircuitBreakerConfig setCircuitBreakerConfig(){
        CircuitBreakerConfig circuitBreakerConfig=new CircuitBreakerConfig();
        circuitBreakerConfig.setCircuitBreakerRetryAttempts(dcCircuitBreakerRetryAttempts);
        circuitBreakerConfig.setCircuitBreakerResetTimeOut(dcCircuitBreakerResetTimeOut);
        circuitBreakerConfig.setCircuitBreakerOpenTimeout(dcCircuitBreakerOpenTimeout);
        circuitBreakerConfig.setCircuitBreakerBackoff(dcCircuitBreakerBackoff);
        circuitBreakerConfig.setRetryAttempts(dcRetryAttempts);
        circuitBreakerConfig.setRetryInterval(dcRetryInterval);
        return circuitBreakerConfig;

    }
}
