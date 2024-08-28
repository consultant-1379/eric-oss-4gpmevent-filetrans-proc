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

import com.ericsson.oss.adc.models.MessageSchema;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(classes = {RestExecutor.class})
@AutoConfigureWebClient(registerRestTemplate = true)
class RestExecutorTest {
    private static final String URL = "http://localhost:33631";
    private static final String INVALID_URL = "invalidURL";
    private static final String NAME = "name";
    private static final String PASSWORD = "password";

    @Mock
    RestTemplate restTemplate;

    @InjectMocks
    RestExecutor restExecutor;

    @Mock
    ResponseEntity responseEntity;

    @Mock
    HttpEntity httpEntity;


    @Test
    void testRestExecutorExchangeReturnsResponseEntity() {
        Mockito.when(restTemplate.exchange(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(Class.class))).thenReturn(responseEntity);
        Assert.assertSame(responseEntity, restExecutor.exchange(URL, "testExchange", HttpMethod.GET, ResponseEntity.class).getResponseEntity());
    }

    @Test
    void testRestExecutorExchangeExceptionCaught() {
        Assertions.assertDoesNotThrow(() ->
                restExecutor.exchange(
                        INVALID_URL,
                        "testRestExecutorExchangeExceptionCaught",
                        HttpMethod.GET,
                        ResponseEntity.class
                ));
    }

    @Test
    void testRestExecutorExchangeWithAuthenticationReturnsResponseEntity() {
        Mockito.when(restTemplate.exchange(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.any(Class.class))).thenReturn(responseEntity);
        Assert.assertSame(responseEntity, restExecutor.exchangeWithAuthentication(NAME, PASSWORD, URL, "testExchange", HttpMethod.GET, ResponseEntity.class).getResponseEntity());
    }

    @Test
    void testRestExecutorPutEntity() {
        Mockito.when(restTemplate.exchange(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.any(Class.class))).thenReturn(responseEntity);
        Assert.assertSame(responseEntity, restExecutor.putForEntity(URL, "testExchange", HttpMethod.PUT, httpEntity, MessageSchema.class).getResponseEntity());
    }

    @Test
    void testRestExecutorExchangeWithAuthenticationExceptionCaught() {
        Mockito.when(restTemplate.exchange(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(Class.class))).thenThrow(RestClientException.class);

        Assertions.assertDoesNotThrow(() ->
                restExecutor.exchangeWithAuthentication(
                        NAME,
                        PASSWORD,
                        INVALID_URL,
                        "testRestExecutorExchangeExceptionCaught",
                        HttpMethod.GET,
                        ResponseEntity.class
                ));
    }

    @Test
    void testRestExecutorThrowException() {
        Mockito.when(restTemplate.exchange(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.any(Class.class))).thenThrow(new RestClientException("Rest Exception"));
        Assert.assertSame(HttpStatus.SERVICE_UNAVAILABLE,
                restExecutor.putForEntity(URL, "testExchange", HttpMethod.PUT, httpEntity, MessageSchema.class).getResponseEntity().getStatusCode());

    }

}