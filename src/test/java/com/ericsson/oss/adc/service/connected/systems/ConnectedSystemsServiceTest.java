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
package com.ericsson.oss.adc.service.connected.systems;

import com.ericsson.oss.adc.models.connected.systems.ConnectionProperties;
import com.ericsson.oss.adc.models.connected.systems.Subsystem;
import com.ericsson.oss.adc.util.StartupUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@AutoConfigureWebClient(registerRestTemplate = true)
@SpringBootTest(classes = {ConnectedSystemsService.class})
public class ConnectedSystemsServiceTest {

    @Autowired
    private ConnectedSystemsService connectedSystemsService;


    @Autowired
    private RestTemplate restTemplate;

    @Value("${connected.systems.base-url}:${connected.systems.port}${connected.systems.uri}")
    private String connectedSystemsAccessPoint;

    private MockRestServiceServer mockServer;



    @MockBean
    StartupUtil startupUtil;

    @Test
    @Order(1)
    @DisplayName("Verify map of size zero is returned when getting subsystems details with empty response.")
    public void verifyGetSubsystemsDetailsWithEmptyResponse() throws Exception {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        mockServer.expect(ExpectedCount.once(), requestTo(new URI(connectedSystemsAccessPoint)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new InputStreamResource(Objects.requireNonNull(getClass().getClassLoader()
                                .getResourceAsStream("EmptySubsystemResponse.json")))));
        final Map<String, Subsystem> map = connectedSystemsService.getSubsystemDetails();
        assertNotNull(map);
        assertEquals(0, map.size());
        mockServer.verify();
    }

    @Test
    @Order(2)
    @DisplayName("Verify map of size zero is returned when getting subsystems details but exception is thrown.")
    public void verifyGetSubsystemsDetailsExceptionThrown() throws Exception {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        mockServer.expect(ExpectedCount.once(), requestTo(new URI(connectedSystemsAccessPoint)))
                .andExpect(method(HttpMethod.GET))
                //BAD_REQUEST guides flow into an exception in getSubsystemDetails while returning a map of size 0 by not specifying the body
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));
        final Map<String, Subsystem> map = connectedSystemsService.getSubsystemDetails();
        assertNotNull(map);
        assertEquals(0, map.size());
        mockServer.verify();
    }

    @Test
    @Order(3)
    @DisplayName("Verify map of size greater than zero is returned when getting subsystems details with non empty response.")
    public void verifyGetSubsystemsDetailsWithNonEmptyResponse() throws Exception {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        mockServer.expect(ExpectedCount.once(), requestTo(new URI(connectedSystemsAccessPoint)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new InputStreamResource(Objects.requireNonNull(getClass().getClassLoader()
                                .getResourceAsStream("GetSFTPSubsystemsResponse.json")))));
        final Map<String, Subsystem> map = connectedSystemsService.getSubsystemDetails();
        assertNotNull(map);
        assertTrue(map.size() > 0);
        mockServer.verify();
    }

    @Test
    @Order(4)
    @DisplayName("Verify get Connection Properties by instance's subsystem name.")
    public void verifyGetConnectionPropertiesBySubsystemsName() throws Exception {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        mockServer.expect(ExpectedCount.once(), requestTo(new URI(connectedSystemsAccessPoint)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new InputStreamResource(Objects.requireNonNull(getClass().getClassLoader()
                                .getResourceAsStream("GetSFTPSubsystemsResponse.json")))));
        final Map<String, Subsystem> map = connectedSystemsService.getSubsystemDetails();
        final ConnectionProperties connectionProperties =
                connectedSystemsService.getConnectionPropertiesBySubsystemsName(map,"enm2");
        assertNotNull(connectionProperties);
        mockServer.verify();
    }

    @Test
    @Order(5)
    @DisplayName("Verify get Connection Properties by instance's subsystem name when map is empty.")
    public void verifyGetConnectionPropertiesBySubsystemsNameWhenMapIsEmpty() throws Exception {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        mockServer.expect(ExpectedCount.once(), requestTo(new URI(connectedSystemsAccessPoint)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new InputStreamResource(Objects.requireNonNull(getClass().getClassLoader()
                                .getResourceAsStream("EmptySubsystemResponse.json")))));
        final Map<String, Subsystem> map = connectedSystemsService.getSubsystemDetails();
        assertNotNull(map);
        assertEquals(0, map.size());
        final ConnectionProperties connectionProperties =
                connectedSystemsService.getConnectionPropertiesBySubsystemsName(map,"");
        assertNull(connectionProperties);
        mockServer.verify();
    }

    @Test
    @Order(6)
    @DisplayName("Verify get subsystem name")
    public void verifyGetSubsystemName()  throws Exception {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        mockServer.expect(ExpectedCount.once(), requestTo(new URI(connectedSystemsAccessPoint)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new InputStreamResource(Objects.requireNonNull(getClass().getClassLoader()
                                .getResourceAsStream("GetSFTPSubsystemsResponse.json")))));
        final Map<String, Subsystem> map = connectedSystemsService.getSubsystemDetails();
        assertNotNull(map);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("enm2"));
        mockServer.verify();
    }
}
