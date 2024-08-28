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

package com.ericsson.oss.adc.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Generic REST utility to class to handle REST exchanges, exception handling, and logging
 */

@Component
public class RestExecutor {

    @Autowired
    private RestTemplate restTemplate;

    private static final Logger LOG = LoggerFactory.getLogger(RestExecutor.class);

    private HttpEntity<Object> requestStructure(){
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(headers);
    }

    private HttpEntity<Object> requestStructureWithAuthentication(final String username, final String password){
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(username, password);
        return new HttpEntity<>(headers);
    }

    public <T> ResponseEntityDTO<T> exchange(final String url, final String logMessage, final HttpMethod httpMethod, final Class<T> responseType) {
        LOG.debug("{} : {}", logMessage, url);
        final ResponseEntityDTO<T> responseEntityDTO = new ResponseEntityDTO<>();

        try {
            ResponseEntity<T> response = restTemplate.exchange(url, httpMethod, requestStructure(), responseType);
            responseEntityDTO.setResponseEntity(response);
        } catch (final RestClientException error) {
            responseEntityDTO.setResponseEntity(new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE));
            LOG.error("Failed to query : {} , Error Msg: {}", url, error.getMessage());
            LOG.debug("Stack trace - Failed to query , URL : {}, Error Trace :", url, error);
        }
        return responseEntityDTO;
    }

    public <T> ResponseEntityDTO<T> exchangeWithParameterizedTypeReference(final String url, final String logMessage, final HttpMethod httpMethod, final ParameterizedTypeReference<T> responseType) {
        LOG.debug("{} : {}", logMessage, url);
        final ResponseEntityDTO<T> responseEntityDTO = new ResponseEntityDTO<>();

        try {
            ResponseEntity<T> response = restTemplate.exchange(url, httpMethod, requestStructure(), responseType);
            responseEntityDTO.setResponseEntity(response);
        } catch (final RestClientException error) {
            responseEntityDTO.setResponseEntity(new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE));
            LOG.error("Failed to query : {} , Error Msg: {}", url, error.getMessage());
            LOG.debug("Stack trace - Failed to query , URL : {}, Error Trace :", url, error);
        }
        return responseEntityDTO;
    }

    public <T> ResponseEntityDTO<T> exchangeWithAuthentication(final String username, final String password, final String url, final String logMessage,
                                                               final HttpMethod httpMethod, final Class<T> responseType) {
        LOG.debug("{} : {}", logMessage, url);
        final ResponseEntityDTO<T> responseEntityDTO = new ResponseEntityDTO<>();
        try {
            ResponseEntity<T> response = restTemplate.exchange(url, httpMethod, requestStructureWithAuthentication(username, password), responseType);
            responseEntityDTO.setResponseEntity(response);
        } catch (final RestClientException error) {
            responseEntityDTO.setResponseEntity(new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE));
            LOG.error("Failed to query with Authentication {} , Error Msg: {}", url, error.getMessage());
            LOG.debug("Stack trace - Failed to query , URL : {}, Error Trace: ", url, error);
        }
        return responseEntityDTO;

    }

    public <T> ResponseEntityDTO<T> putForEntity(final String url, final String logMessage, final HttpMethod httpMethod, final HttpEntity<String> body, final Class<T> responseType) {
        LOG.debug("{} : {}", logMessage, url);
        final ResponseEntityDTO<T> responseEntityDTO = new ResponseEntityDTO<>();
        try {
            ResponseEntity<T> response = restTemplate.exchange(url, httpMethod, body, responseType);
            responseEntityDTO.setResponseEntity(response);
        } catch (final HttpClientErrorException exception) {
            responseEntityDTO.setResponseEntity(new ResponseEntity<>(exception.getStatusCode()));
            LOG.error("Failed to query : {}, Error message : {}", url, exception.getMessage());
            LOG.debug("Stack trace - Failed to query , URL : {}, Error Trace : ", url, exception);

        } catch (final RestClientException error) {
            responseEntityDTO.setResponseEntity(new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE));
            LOG.error("Failed to query : {}, Error message : {}", url, error.getMessage());
            LOG.debug("Stack trace - Failed to query , URL : {}, Error Trace : ", url, error);
        }
        return responseEntityDTO;
    }
}
