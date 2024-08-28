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

package com.ericsson.oss.adc.service.data.catalog;

import com.ericsson.oss.adc.models.data.catalog.v2.MessageSchemaPutRequest;
import com.ericsson.oss.adc.models.data.catalog.v2.MessageSchemaV2;
import com.ericsson.oss.adc.util.ResponseEntityDTO;
import com.ericsson.oss.adc.util.RestExecutor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation of a handler to communicate with the Data Management and Movement (DM&M) Data Catalog service.
 * This includes different ways of getting the notification topic entity and registering the output topic to and
 * from the service, including by id, name, name and message bus ID, and simply getting all notification topics
 * stored on the data catalog service.
 */
@Service
public class DataCatalogService {

    @Value("${dmm.data-catalog.base-url}")
    private String dataCatalogBaseUrl;

    @Value("${dmm.data-catalog.base-port}")
    private String dataCatalogBasePort;

    @Value("${dmm.data-catalog.message-schema-uri-v1}")
    private String messageSchemaUriV1;

    @Value("${dmm.data-catalog.data-service-uri}")
    private String dataServiceUri;

    @Autowired
    private RestExecutor restExecutor;

    private static final Logger LOG = LoggerFactory.getLogger(DataCatalogService.class);

    private static final String LOG_MESSAGE = "[Requesting {0} from data-catalog]";

    /**
     * Delete {@link com.ericsson.oss.adc.models.data.catalog.v2.DataServiceInstance} from {@link DataCatalogService}
     * @param dataServiceName The name of the data service (i.e. this services name)
     * @param dataServiceInstanceName The name of the data service instance (i.e this service name and the name of the ENM instance.
     * @return The response from the API indicating status of the request.
     */
    public ResponseEntity<Void> deleteDataServiceInstance(String dataServiceName, String dataServiceInstanceName) {
        final String url = MessageFormat.format("{0}{1}{2}?dataServiceName={3}&dataServiceInstanceName={4}", dataCatalogBaseUrl, dataCatalogBasePort,
                dataServiceUri, dataServiceName, dataServiceInstanceName);
        LOG.debug("DELETE DataServiceInstance by Params: {}", url);
        final ResponseEntityDTO<Void> responseEntityDTO = restExecutor.exchange(url, LOG_MESSAGE, HttpMethod.DELETE, Void.class);
        return responseEntityDTO.getResponseEntity();
    }

    /**
     * Generic request body for post notifications.
     *
     * @param httpBody The body that will be used in the HttpEntity.
     * @return the HTTPEntity object.
     * @throws JsonProcessingException Throw a JSON processing exception if an error occurs.
     */
    public HttpEntity<String> requestBodyStructure(final Map<String, Object> httpBody) throws JsonProcessingException {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonData = new ObjectMapper().writeValueAsString(httpBody);
        LOG.debug("JSON to be Sent {}", jsonData);
        return new HttpEntity<>(jsonData, headers);
    }

    /**
     * Register a MessageSchema on the data catalog.
     *
     * @param messageSchema The object to pass which will fill the payload.
     * @return response entity containing the registered message schema
     */
    public ResponseEntity<MessageSchemaV2> registerMessageSchema(final MessageSchemaPutRequest messageSchema) {
        final String url = MessageFormat.format("{0}{1}{2}/", dataCatalogBaseUrl, dataCatalogBasePort, messageSchemaUriV1);
        final Map<String, Object> messageSchemaTypeBody  = generateMessageSchemaPUTHttpBody(messageSchema);
        LOG.debug("POST Message Schema: {}", url);
        try {
            ResponseEntityDTO<MessageSchemaV2> messageSchemaResponseEntity = restExecutor.putForEntity(url, LOG_MESSAGE, HttpMethod.PUT, requestBodyStructure(messageSchemaTypeBody), MessageSchemaV2.class);
            return messageSchemaResponseEntity.getResponseEntity();
        } catch (final Exception exception) {
            LOG.error("Failed to create the message schema type due to bad request {}", exception.getMessage());
            LOG.debug("Stack trace - Failed to create the message schema type due to bad request: ", exception);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    private Map<String, Object> generateMessageSchemaPUTHttpBody(final MessageSchemaPutRequest messageSchema) {
        final Map<String, Object> messageSchemaBody = new LinkedHashMap<>(6);
        messageSchemaBody.put("dataSpace", messageSchema.getDataSpace());
        messageSchemaBody.put("dataService", messageSchema.getDataService());
        messageSchemaBody.put("dataCategory", messageSchema.getDataCategory());
        messageSchemaBody.put("dataProviderType", messageSchema.getDataProviderType());
        messageSchemaBody.put("messageStatusTopic", messageSchema.getMessageStatusTopic());
        messageSchemaBody.put("messageDataTopic", messageSchema.getMessageDataTopic());
        messageSchemaBody.put("dataServiceInstance", messageSchema.getDataServiceInstance());
        messageSchemaBody.put("dataType", messageSchema.getDataType());
        messageSchemaBody.put("supportedPredicateParameter", messageSchema.getSupportedPredicateParameter());
        messageSchemaBody.put("messageSchema", messageSchema.getMessageSchema());
        return messageSchemaBody;
    }
}
