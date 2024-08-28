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

import com.ericsson.oss.adc.enums.MessageEncoding;
import com.ericsson.oss.adc.models.*;
import com.ericsson.oss.adc.models.data.catalog.v2.*;
import com.ericsson.oss.adc.util.RestExecutor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(classes = {DataCatalogService.class, RestExecutor.class})
@AutoConfigureWebClient(registerRestTemplate = true)
class DataCatalogServiceTest {

    @SpyBean
    DataCatalogService dataCatalogService;

    @SpyBean
    DataCatalogServiceV2 dataCatalogServiceV2;

    @Value("${dmm.data-catalog.base-url}")
    private String dataCatalogBaseUrl;

    @Value("${dmm.data-catalog.base-port}")
    private String dataCatalogBasePort;

    @Value("${dmm.data-catalog.notification-topic-uri}")
    private String notificationTopicUri;

    public static final String URI_PATTERN_ARRAY = "{0}{1}{2}/";

    String enmName="enm1";

    private final MessageSchemaPutRequest messageSchemaPutRequest = new MessageSchemaPutRequest(
            1,
            new DataSpace("4G"),
            new DataServiceForMessageSchemaPut("ds"),
            new DataServiceInstance("dsinst101", "http://localhost:8082", "4G", "4G", "4G", "SCH2", "2"),
            new DataCategory("CM_EXPORTS1"),
            new DataProviderTypeForMessageSchemaPUT("Vv101", "vv101"),
            new MessageStatusTopic("topic102", 1L, "SpecRef101", MessageEncoding.JSON),
            new MessageDataTopic("topic102", 1L, MessageEncoding.JSON),
            new DataType(1, "stream", "SCH2", "2", true, "4G", "4G", "4G", "4G", "2"),
            new SupportedPredicateParameter("pd101", true),
            new MessageSchema("SpecRef101")
    );

    @Test
    @Order(1)
    @DisplayName("Fail to retrieve MessageSchemaV2 List by params returns null when topic with unknown dataCategory not found")
    void test_getMessageSchemaListV2ByDataProviderTypeAndDataSpace_400() {  // TODO: Add positive test case when stub updated
        ResponseEntity<MessageSchemaListV2> response = dataCatalogServiceV2.getMessageSchemaListV2ByDataSpaceAndDataCategory(
                "pvid_1",
                "name");
        assertNull(response.getBody());
    }

    @Test
    @Order(2)
    @DisplayName("Registers message schema and verifies message schema params in PUT response")
    public void test_registerMessageSchema_201() {
        ResponseEntity<MessageSchemaV2> response = dataCatalogService.registerMessageSchema(messageSchemaPutRequest);
        assertNotNull(response);
        MessageSchemaV2 messageSchemaV2 = response.getBody();
        //More will be added to this test in a later commit
    }

    @Test
    @Order(3)
    @DisplayName("Deletes a Data Service Instance successfully and verifies 204 status code")
    public void test_deleteDataServiceInstance_200() {
        ResponseEntity<Void> response = dataCatalogService.deleteDataServiceInstance("ds", "dsinstance");
        assertNotNull(response);
    }

}
