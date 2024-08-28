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

import com.ericsson.oss.adc.config.CircuitBreakerConfig;
import com.ericsson.oss.adc.models.*;
import com.ericsson.oss.adc.models.data.catalog.v2.*;
import com.ericsson.oss.adc.service.connected.systems.ConnectedSystemsService;
import com.ericsson.oss.adc.service.data.catalog.DataCatalogService;
import com.ericsson.oss.adc.service.data.catalog.DataCatalogServiceV2;
import com.ericsson.oss.adc.service.output.topic.OutputTopicService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {StartupUtil.class})
@EnableConfigurationProperties(value = DataCatalogProperties.class)
public class StartupUtilTest {

    @Autowired
    private StartupUtil startupUtil;

    // required to be mocked for tests to run though never used in test
    @MockBean
    private OutputTopicService outputTopicService;

    // required to be mocked for tests to run though never used in test

    @MockBean
    private DataCatalogService dataCatalogService;

    @MockBean
    private DataCatalogServiceV2 dataCatalogServiceV2;

    @Autowired
    private DataCatalogProperties dataCatalogProperties;

    @Value("${dmm.data-catalog.data-space}")
    private String dataSpaceName;

    @Value("${dmm.data-catalog.data-category}")
    private String dataCategory;

    @Value("${speccification.reference}")
    private String specificationReference;

    @Value("${dmm.data-catalog.availability.retry-interval}")
    private int retryInterval;

    @Value("${dmm.data-catalog.availability.retry-attempts}")
    private int retryAttempts;

    @MockBean
    RetryUtil retryUtil;

    List<String> mockEnmList = Arrays.asList( "enm1", "enm2");


    RetryTemplate retryTemplate = new RetryTemplate();
    private static final String INPUT_TOPIC_MESSAGE_BUS_UNAVAILABLE = "No Input Topic Message Bus Name Available From Data Catalog";
    private static final String INPUT_TOPIC_MESSAGE_BUS_STATUS_TOPIC_NAME_DATA_SPACE_UNAVAILABLE = "No Input Topic Message Bus, Message Status Topic Name Or Data Space Available From Data Catalog";
    private final String outputTopicName = "4g-pm-event-file-transfer-and-processing--enm2";
    private final MessageSchemaV2 messageSchemaV2 = new MessageSchemaV2(
            1,
            new MessageDataTopicV2(
                    "file-notification-service--4g-event--enm2",
                    new DataProviderType(
                            "providerVersion",
                            new DataSpace("4G"),
                            "enm2",
                            "RAN"),
                    new MessageBus(
                            1L,
                            "name",
                            "clusterName",
                            "nameSpace",
                            new ArrayList<String>(Arrays.asList("http://endpoint1:1234/", "eric-oss-dmm-data-message-bus-kf-client:9092",
                                    "http://localhost:9092")),
                            new ArrayList<Long>(Collections.singletonList(1L)),
                            new ArrayList<Long>(Collections.singletonList(1L)),
                            new ArrayList<Long>(Collections.singletonList(1L))),
                    new MessageStatusTopic(
                            "file-notification-service--4g-event--enm2",
                            1L
                    )
            ),
            specificationReference,
            new DataService(
                    "dataserviceinstanceName",
                    new DataServiceInstance[]{new DataServiceInstance()},
                    new SupportedPredicateParameter("nodeName", true)
            ),
            new DataType()
    );

    private final MessageSchemaV2 messageSchemaV2OutputTopic = new MessageSchemaV2(
            1,
            new MessageDataTopicV2(
                    outputTopicName,
                    new DataProviderType(
                            "providerVersion",
                            new DataSpace("4G"),
                            "enm2",
                            "RAN"),
                    new MessageBus(
                            1L,
                            "name",
                            "clusterName",
                            "nameSpace",
                            new ArrayList<String>(Arrays.asList("http://endpoint1:1234/", "eric-oss-dmm-data-message-bus-kf-client:9092",
                                    "http://localhost:9092")),
                            new ArrayList<Long>(Collections.singletonList(1L)),
                            new ArrayList<Long>(Collections.singletonList(1L)),
                            new ArrayList<Long>(Collections.singletonList(1L))),
                    new MessageStatusTopic(
                            outputTopicName,
                            1L
                    )
            ),
            specificationReference,
            new DataService(
                    "dataserviceinstanceName",
                    new DataServiceInstance[]{new DataServiceInstance()},
                    new SupportedPredicateParameter("nodeName", true)
            ),
            new DataType()
    );

    private final MessageSchemaV2 messageSchemaV2NullDataSpaceName = new MessageSchemaV2(
            1,
            new MessageDataTopicV2(
                    "file-notification-service--4g-event--enm2",
                    new DataProviderType(
                            "providerVersion",
                            new DataSpace(""),
                            "enm2",
                            "RAN"),
                    new MessageBus(
                            1L,
                            "name",
                            "clusterName",
                            "nameSpace",
                            new ArrayList<String>(Arrays.asList("http://endpoint1:1234/", "eric-oss-dmm-data-message-bus-kf-client:9092",
                                    "http://localhost:9092")),
                            new ArrayList<Long>(Collections.singletonList(1L)),
                            new ArrayList<Long>(Collections.singletonList(1L)),
                            new ArrayList<Long>(Collections.singletonList(1L))),
                    new MessageStatusTopic(
                            "file-notification-service--4g-event--enm2",
                            1L
                    )
            ),
            specificationReference,
            new DataService(
                    "dataserviceinstanceName",
                    new DataServiceInstance[]{new DataServiceInstance()},
                    new SupportedPredicateParameter("nodeName", true)
            ),
            new DataType()
    );

    private final MessageSchemaV2 messageSchemaV2NullMessageBus = new MessageSchemaV2(
            1,
            new MessageDataTopicV2(
                    "file-notification-service--4g-event--enm2",
                    new DataProviderType(
                            "providerVersion",
                            new DataSpace("4G"),
                            "enm2",
                            "RAN"),
                    new MessageBus(),
                    new MessageStatusTopic(
                            "file-notification-service--4g-event--enm2",
                            1L
                    )
            ),
            specificationReference,
            new DataService(
                    "dataserviceinstanceName",
                    new DataServiceInstance[]{new DataServiceInstance()},
                    new SupportedPredicateParameter("nodeName", true)
            ),
            new DataType()
    );

    private final MessageSchemaV2 messageSchemaV2NullDataSpaceMessageStatusTopicNameMessageBusName = new MessageSchemaV2(
            1,
            new MessageDataTopicV2(
                    "name",
                    new DataProviderType(
                            "providerVersion",
                            new DataSpace("XL"),
                            "enm2",
                            "RAN"),
                    new MessageBus(
                            1L,
                            "name",
                            "clusterName",
                            "nameSpace",
                            new ArrayList<String>(Arrays.asList("http://endpoint1:1234/", "eric-oss-dmm-data-message-bus-kf-client:9092",
                                    "http://localhost:9092")),
                            new ArrayList<Long>(Collections.singletonList(1L)),
                            new ArrayList<Long>(Collections.singletonList(1L)),
                            new ArrayList<Long>(Collections.singletonList(1L))),
                    new MessageStatusTopic(
                            "file-notification-service--4g-event--enm2",
                            1L
                    )
            ),
            specificationReference,
            new DataService(
                    "dataserviceinstanceName",
                    new DataServiceInstance[]{new DataServiceInstance()},
                    new SupportedPredicateParameter("nodeName", true)
            ),
            new DataType()
    );

    private final MessageSchemaListV2 messageSchemaListV2 = new MessageSchemaListV2();


    @Test
    @DisplayName("Should return true after successfully fetching access end points from DataCatalog")
    void test_allObjectsSetupCorrectly() {
        messageSchemaListV2.add(messageSchemaV2);
        messageSchemaListV2.add(messageSchemaV2OutputTopic);
        ResponseEntity<MessageSchemaV2> messageSchemaResponseEntity = new ResponseEntity<>(messageSchemaV2, HttpStatus.OK);
        ResponseEntity<MessageSchemaListV2> messageSchemaListV2ResponseEntity = new ResponseEntity<>(messageSchemaListV2, HttpStatus.OK);

        when(dataCatalogServiceV2.getMessageSchemaListV2ByDataSpaceAndDataCategory(dataSpaceName, dataCategory)).thenReturn(messageSchemaListV2ResponseEntity);
        when(dataCatalogService.registerMessageSchema(any(MessageSchemaPutRequest.class))).thenReturn(messageSchemaResponseEntity);
        when(outputTopicService.getOutputTopicName()).thenReturn(outputTopicName);

//        assertTrue(startupUtil.queryDataCatalogForInputTopic());
//        assertTrue(startupUtil.registerInDataCatalog());
    }

    @Test
    @DisplayName("Should return true when DataCatalog returns no access end points and configmap access end point is provided instead")
    void test_allObjectsSetupFailScenario() {
        messageSchemaListV2.add(messageSchemaV2);
        messageSchemaListV2.add(messageSchemaV2OutputTopic);
        ResponseEntity<MessageSchemaV2> messageSchemaResponseEntity = new ResponseEntity<>(messageSchemaV2, HttpStatus.OK);
        ResponseEntity<MessageSchemaListV2> messageSchemaListV2ResponseEntity = new ResponseEntity<>(messageSchemaListV2, HttpStatus.OK);

        when(dataCatalogServiceV2.getMessageSchemaListV2ByDataSpaceAndDataCategory(dataSpaceName, dataCategory)).thenReturn(messageSchemaListV2ResponseEntity);
        when(dataCatalogService.registerMessageSchema(any(MessageSchemaPutRequest.class))).thenReturn(messageSchemaResponseEntity);

        //startupUtil.queryDataCatalogForInputTopic();
        startupUtil.registerInDataCatalog(mockEnmList);
        assertTrue(true);
    }

    @Test
    @DisplayName("Failure to get Message Bus in Data Catalog should throw NullPointer Exception")
    void test_FailureMessageBusUnavailableInDataCatalogThrowNullPointer() {
        messageSchemaListV2.add(messageSchemaV2NullMessageBus);
        messageSchemaListV2.add(messageSchemaV2OutputTopic);
        ResponseEntity<MessageSchemaV2> messageSchemaResponseEntity = new ResponseEntity<>(messageSchemaV2NullMessageBus, HttpStatus.OK);
        ResponseEntity<MessageSchemaListV2> messageSchemaListV2ResponseEntity = new ResponseEntity<>(messageSchemaListV2, HttpStatus.OK);

        when(dataCatalogServiceV2.getMessageSchemaListV2ByDataSpaceAndDataCategory("4G", dataCategory)).thenReturn(messageSchemaListV2ResponseEntity);
        when(dataCatalogService.registerMessageSchema(any(MessageSchemaPutRequest.class))).thenReturn(messageSchemaResponseEntity);


        assertTrue(INPUT_TOPIC_MESSAGE_BUS_UNAVAILABLE.equals(INPUT_TOPIC_MESSAGE_BUS_UNAVAILABLE));
    }

    @Test
    @DisplayName("Failure to register MessageSchema because of 409 response in Data Catalog should not cause error scenario")
    void test_RegistrationFailureMessageSchemaBecauseOf409ShouldNotCauseError() {
        messageSchemaListV2.add(messageSchemaV2);
        messageSchemaListV2.add(messageSchemaV2OutputTopic);
        ResponseEntity<MessageSchemaListV2> messageSchemaListV2ResponseEntity = new ResponseEntity<>(messageSchemaListV2, HttpStatus.OK);
        ResponseEntity<MessageSchemaV2> messageSchemaResponseEntity = new ResponseEntity<>(messageSchemaV2, HttpStatus.valueOf(409));

        when(dataCatalogService.registerMessageSchema(any())).thenReturn(messageSchemaResponseEntity);
        when(dataCatalogServiceV2.getMessageSchemaListV2ByDataSpaceAndDataCategory(dataSpaceName, dataCategory)).thenReturn(messageSchemaListV2ResponseEntity);
        when(outputTopicService.getOutputTopicName()).thenReturn(outputTopicName);
        when(retryUtil.retryTemplate(retryAttempts, retryInterval)).thenReturn(retryTemplate);
        assertTrue(startupUtil.queryDataCatalogForInputTopic());
        assertTrue(startupUtil.registerInDataCatalog(mockEnmList));
    }

    @Test
    @DisplayName("Failure to register MessageSchema because of 408 response in Data Catalog should cause error scenario")
    void test_RegistrationFailureMessageSchemaBecauseOf408ShouldCauseError() {
        messageSchemaListV2.add(messageSchemaV2);
        messageSchemaListV2.add(messageSchemaV2OutputTopic);
        ResponseEntity<MessageSchemaListV2> messageSchemaListV2ResponseEntity = new ResponseEntity<>(messageSchemaListV2, HttpStatus.OK);
        ResponseEntity<MessageSchemaV2> messageSchemaResponseEntity = new ResponseEntity<>(messageSchemaV2, HttpStatus.valueOf(408));

        when(dataCatalogService.registerMessageSchema(any())).thenReturn(messageSchemaResponseEntity);
        when(dataCatalogServiceV2.getMessageSchemaListV2ByDataSpaceAndDataCategory(dataSpaceName, dataCategory)).thenReturn(messageSchemaListV2ResponseEntity);
        when(outputTopicService.getOutputTopicName()).thenReturn(outputTopicName);
        when(retryUtil.retryTemplate(retryAttempts, retryInterval)).thenReturn(retryTemplate);
        assertTrue(startupUtil.queryDataCatalogForInputTopic());
        assertFalse(startupUtil.registerInDataCatalog(mockEnmList));
    }

    @Test
    @DisplayName("Failure to register MessageSchema because of 4XX response in Data Catalog should cause error scenario")
    void test_RegistrationFailureMessageSchemaBecauseOf4XXShouldCauseError() {
        messageSchemaListV2.add(messageSchemaV2);
        messageSchemaListV2.add(messageSchemaV2OutputTopic);
        ResponseEntity<MessageSchemaListV2> messageSchemaListV2ResponseEntity = new ResponseEntity<>(messageSchemaListV2, HttpStatus.OK);
        ResponseEntity<MessageSchemaV2> messageSchemaResponseEntity = new ResponseEntity<>(messageSchemaV2, HttpStatus.valueOf(400));

        when(dataCatalogService.registerMessageSchema(any())).thenReturn(messageSchemaResponseEntity);
        when(dataCatalogServiceV2.getMessageSchemaListV2ByDataSpaceAndDataCategory(dataSpaceName, dataCategory)).thenReturn(messageSchemaListV2ResponseEntity);
        when(outputTopicService.getOutputTopicName()).thenReturn(outputTopicName);
        when(retryUtil.retryTemplate(retryAttempts, retryInterval)).thenReturn(retryTemplate);
        assertTrue(startupUtil.queryDataCatalogForInputTopic());
        assertFalse(startupUtil.registerInDataCatalog(mockEnmList));
    }

    @Test
    @DisplayName("Should return true after successfully fetching access end points from DataCatalog with valid message schema PUT request")
    void test_allObjectsSetupCorrectlyWithValidMessageSchemaPutRequest() {
        messageSchemaListV2.add(messageSchemaV2);
        messageSchemaListV2.add(messageSchemaV2OutputTopic);
        ResponseEntity<MessageSchemaV2> messageSchemaResponseEntity = new ResponseEntity<>(messageSchemaV2, HttpStatus.OK);
        ResponseEntity<MessageSchemaListV2> messageSchemaListV2ResponseEntity = new ResponseEntity<>(messageSchemaListV2, HttpStatus.OK);
        MessageSchemaPutRequest messageSchemaPutRequest = createMessageSchemaRequest("enm2");
        validatedConfigurableFields(messageSchemaPutRequest);

        when(dataCatalogServiceV2.getMessageSchemaListV2ByDataSpaceAndDataCategory(dataSpaceName, dataCategory)).thenReturn(messageSchemaListV2ResponseEntity);
        when(dataCatalogService.registerMessageSchema(any())).thenReturn(messageSchemaResponseEntity);
        when(outputTopicService.getOutputTopicName()).thenReturn(outputTopicName);
        when(retryUtil.retryTemplate(retryAttempts, retryInterval)).thenReturn(retryTemplate);
        assertTrue(startupUtil.queryDataCatalogForInputTopic());
        assertTrue(startupUtil.registerInDataCatalog(mockEnmList));
        verify(dataCatalogService).registerMessageSchema(messageSchemaPutRequest);
    }

    private void validatedConfigurableFields(MessageSchemaPutRequest messageSchemaPutRequest) {
        assertEquals("4G", messageSchemaPutRequest.getDataSpace().getName());
        assertEquals("eric-oss-4gpmevent-filetrans-proc", messageSchemaPutRequest.getDataService().getDataServiceName());
        assertEquals("eric-oss-4gpmevent-filetrans-proc--enm2", messageSchemaPutRequest.getDataServiceInstance().getDataServiceInstanceName());
        assertEquals("enm2", messageSchemaPutRequest.getDataServiceInstance().getConsumedDataProvider());

        assertEquals(outputTopicName, messageSchemaPutRequest.getMessageDataTopic().getName());
        assertEquals(outputTopicName, messageSchemaPutRequest.getMessageStatusTopic().getName());
        assertEquals(1L, messageSchemaPutRequest.getMessageDataTopic().getMessageBusId());
        assertEquals(1L, messageSchemaPutRequest.getMessageStatusTopic().getMessageBusId());
        assertEquals(specificationReference, messageSchemaPutRequest.getMessageSchema().getSpecificationReference());
    }

    private MessageSchemaPutRequest createMessageSchemaRequest(String enm ) {
        String dataServiceInstanceName = dataCatalogProperties.getDataServiceName() + "--" + enm;
        return MessageSchemaPutRequest
                .builder()
                .dataSpace(new DataSpace(dataCatalogProperties.getDataSpace()))
                .dataService(new DataServiceForMessageSchemaPut(dataCatalogProperties.getDataServiceName()))
                .dataServiceInstance(new DataServiceInstance(dataServiceInstanceName, enm))
                .dataCategory(new DataCategory())
                .dataProviderType(new DataProviderTypeForMessageSchemaPUT())
                .messageStatusTopic(new MessageStatusTopic(outputTopicName, 1L))
                .messageDataTopic(new MessageDataTopic(outputTopicName, 1L))
                .dataType(new DataType())
                .supportedPredicateParameter(new SupportedPredicateParameter())
                .messageSchema(new MessageSchema(specificationReference))
                .build();
    }

    @Test
    @DisplayName("Should return true list of topic")
    void test_allObjectsSetupCorrectlyWithValidTopicList() {
        messageSchemaListV2.add(messageSchemaV2);
        ResponseEntity<MessageSchemaListV2> messageSchemaListV2ResponseEntity = new ResponseEntity<>(messageSchemaListV2, HttpStatus.OK);
        when(dataCatalogServiceV2.getMessageSchemaListV2ByDataSpaceAndDataCategory(dataSpaceName, dataCategory)).thenReturn(messageSchemaListV2ResponseEntity);
        assertNotNull(startupUtil.getInputTopicList());
    }

    @Test
    @DisplayName("Should return true list of enm instance")
    void test_allObjectsSetupCorrectlyWithValidInstanceList() {
        messageSchemaListV2.add(messageSchemaV2);
        ResponseEntity<MessageSchemaListV2> messageSchemaListV2ResponseEntity = new ResponseEntity<>(messageSchemaListV2, HttpStatus.OK);
        when(dataCatalogServiceV2.getMessageSchemaListV2ByDataSpaceAndDataCategory(dataSpaceName, dataCategory)).thenReturn(messageSchemaListV2ResponseEntity);
        assertNotNull(startupUtil.getAllEnmsFromDataCatalog());
    }

    @Test
    @DisplayName("Should getter and setter")
    void test_GetterAndSetter() throws InterruptedException {
        startupUtil.setApplicationReady(false);
        startupUtil.sleepInSeconds(2);
        assertFalse(startupUtil.isApplicationReady());
    }

}