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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@SpringBootTest(classes = {DependentServiceAvailabilityConnectedSystems.class, CircuitBreakerConfig.class})
@AutoConfigureWebClient(registerRestTemplate = true)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class DependentServiceAvailabilityConnectedSystemsTest {

    private static final String HEALTH_PATH = "actuator/health";

    @Value("${connected.systems.base-url}:${connected.systems.port}/")
    private String connectedSystemsUri;

    @Value("${connected.systems.availability.retry-attempts}")
    private int retryAttempts;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DependentServiceAvailabilityConnectedSystems dependentServiceAvailabilityConnectedSystems;

    private MockRestServiceServer mockServer;

    @BeforeEach
    public void setup() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    @DisplayName("When health status UP/OK, availability should be true")
    public void get_health_status_ok() throws Exception {

        mockServer.expect(ExpectedCount.once(),
                requestTo(new URI(connectedSystemsUri+HEALTH_PATH )))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(getBody(true)));
        boolean result = dependentServiceAvailabilityConnectedSystems.checkService();
        mockServer.verify();
        assertTrue(result);
    }




    @Test
    @DisplayName("When health status DOWN/Not Found, availability should be false, retry and get success")
    public void get_health_status_not_ok() throws Exception {

        mockServer.expect(ExpectedCount.times(retryAttempts),
                requestTo(new URI(connectedSystemsUri+HEALTH_PATH )))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(getBody(false)));

        dependentServiceAvailabilityConnectedSystems.useCiruitBreaker = false;
        boolean result = dependentServiceAvailabilityConnectedSystems.checkService();
        mockServer.verify();
        assertFalse(result);
    }

    private String getBody(final boolean up) {
        final String body = "{\"status\":\"%1$s\",\"groups\":[\"liveness\",\"readiness\"]}";
        if (up) {
            return String.format(body, "UP");
        }
        return String.format(body, "DOWN");
    }
}
