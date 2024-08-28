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
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;


@Service
public class ConnectedSystemsService {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectedSystemsService.class);

    @Value("${connected.systems.base-url}:${connected.systems.port}${connected.systems.uri}")
    private String connectedSystemsUrl;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Get subsystems details and populate the systemsByNameMap instant attribute and return the Map.
     *
     * @return Map of String to Subsystem retrieved from connected systems.
     */
    public Map<String, Subsystem> getSubsystemDetails() {
        LOG.debug("Requesting subsystem details at url '{}'", connectedSystemsUrl);
        try {
            final Map<String, Subsystem> subsystemsByNameMap = new HashMap<>();
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            final HttpEntity<Object> entity = new HttpEntity<>(headers);
            final ResponseEntity<Subsystem[]> response = restTemplate.exchange(connectedSystemsUrl, HttpMethod.GET, entity, Subsystem[].class);
            formatResponse(response, subsystemsByNameMap);
            LOG.debug("Successfully executed request {}, response {} ", connectedSystemsUrl, response.getStatusCode().value());
            return subsystemsByNameMap;

        } catch (final Exception exception) {
            LOG.error("Failed to execute {} request: {}", connectedSystemsUrl, exception.getMessage());
            LOG.debug("Stack trace - Failed to execute {} request: ", connectedSystemsUrl, exception);
        }
        return Collections.emptyMap();
    }

    private Map<String, Subsystem> formatResponse(final ResponseEntity<Subsystem[]> response, final Map<String, Subsystem> subsystemsByNameMap) {
        LOG.debug("Formatting response '{}'", response);
        if (response.getStatusCode().value() == Response.Status.OK.getStatusCode()) {
            final List<Subsystem> subsystemList = Arrays.asList(response.getBody());

            if (!subsystemList.isEmpty()) {
                for (final Subsystem subsystem : subsystemList) {
                    subsystemsByNameMap.put(subsystem.getName(), subsystem);
                }
            }
            return subsystemsByNameMap;
        }
        return Collections.emptyMap();
    }

    public ConnectionProperties getConnectionPropertiesBySubsystemsName(final Map<String, Subsystem> subsystemsByNameMap, String eName) {
        if (subsystemsByNameMap.containsKey(eName)) {
            return subsystemsByNameMap.get(eName).getConnectionProperties().get(0);
        }
        LOG.debug("Getting connection properties by subsystem names '{}'", subsystemsByNameMap);

        return null;
    }
}
