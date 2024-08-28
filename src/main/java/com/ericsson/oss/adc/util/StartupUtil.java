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

import com.ericsson.oss.adc.availability.UnsatisfiedExternalDependencyException;
import com.ericsson.oss.adc.models.*;
import com.ericsson.oss.adc.models.data.catalog.v2.*;
import com.ericsson.oss.adc.service.data.catalog.DataCatalogService;
import com.ericsson.oss.adc.service.data.catalog.DataCatalogServiceV2;
import com.ericsson.oss.adc.service.output.topic.OutputTopicService;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component("first")
public class StartupUtil {

    @Autowired
    private DataCatalogService dataCatalogService;

    @Autowired
    private DataCatalogServiceV2 dataCatalogServiceV2;

    @Autowired
    private DataCatalogProperties dataCatalogProperties;

    @Autowired
    private OutputTopicService outputTopicService;


    @Value("${spring.kafka.topics.input.prefix}")
    private String inputTopicPrefix;


    @Value("${speccification.reference}")
    private String specificationReference;

    @Value("${dmm.data-catalog.availability.retry-interval}")
    private int retryInterval;

    @Value("${dmm.data-catalog.availability.retry-attempts}")
    private int retryAttempts;

    @Autowired
    RetryUtil retryUtil;



    private volatile boolean isApplicationReady = false;


    private static final String INPUT_TOPIC_MESSAGE_BUS_UNAVAILABLE = "No Input Topic Message Bus Name Available From Data Catalog";
    private static final String INPUT_TOPIC_MESSAGE_BUS_STATUS_TOPIC_NAME_DATA_SPACE_UNAVAILABLE = "No Input Topic Message Bus, Message Status Topic Name Or Data Space Available From Data Catalog";
    private MessageBus messageBus;
    protected Boolean retrieveFromDataCatalog = false;
    protected Boolean outputTopicSetup = false;
    private Long messageBusId = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(StartupUtil.class);

    /**
     * Check if we got an OK Response code.
     *
     * @param response The response to check the code for.
     * @return True if valid response code found.
     * @throws UnsatisfiedExternalDependencyException If not OK response code found.
     */
    public static boolean isOkResponseCode(final ResponseEntity<String> response) throws UnsatisfiedExternalDependencyException {
        if (response.getStatusCode().value() == Response.Status.OK.getStatusCode()) {
            return true;
        }
        throw new UnsatisfiedExternalDependencyException("OK status code not found, found: " + response.getStatusCode().value());
    }

    /**
     * Take an {@link java.net.URI} and if it doesn't end with a slash add one.
     *
     * @param uri The {@link java.net.URI} to check.
     * @return The {@link String} with a guaranteed slash appended.
     */
    public static String addTrailingSlash(String uri) {
        if (!uri.endsWith("/")) { // if not ending / append one
            uri = uri + "/";
        }
        return uri;
    }


    /**
     * Queries DataCatalog for Message Schema V2 relating to the InputTopic.
     * This query must succeed before the setting up of the Output Topic on both DataCatalog and KafkaMessageBus
     *
     * @return boolean representing if the Message Schema V2 was present or not
     */
    public boolean queryDataCatalogForInputTopic() {
        return retryUtil.retryTemplate(retryAttempts, retryInterval).execute(context -> retrieveFromDataCatalog = retrieveDataCatalogEntries());
    }

    private MessageSchemaPutRequest createMessageSchemaRequest(String enmName) {
        //setting dataServiceInstance name to be dataServiceName--dataCollectorName for now. If a requirement comes in the future for 4g to have separate deployments this will need to change
        //If any of the data in the models change the message schema should be updated as long as dataServiceName + messageDataTopic.name + specificationReference are the same

        String dataServiceInstanceName = dataCatalogProperties.getDataServiceName() + "--" + enmName;
        return MessageSchemaPutRequest
                .builder()
                .dataSpace(new DataSpace(dataCatalogProperties.getDataSpace()))
                .dataService(new DataServiceForMessageSchemaPut(dataCatalogProperties.getDataServiceName()))
                .dataServiceInstance(new DataServiceInstance(dataServiceInstanceName, enmName))
                .dataCategory(new DataCategory())
                .dataProviderType(new DataProviderTypeForMessageSchemaPUT())
                .messageStatusTopic(new MessageStatusTopic(outputTopicService.getOutputTopicName(), messageBusId))
                .messageDataTopic(new MessageDataTopic(outputTopicService.getOutputTopicName(), messageBusId))
                .dataType(new DataType())
                .supportedPredicateParameter(new SupportedPredicateParameter())
                .messageSchema(new MessageSchema(specificationReference))
                .build();
    }

    /**
     * Utility function for the registration and retrieval of details from DataCatalog
     *
     * @return boolean if retrieval/registration was successful
     */
    public boolean registerInDataCatalog(List<String> enms) {
        try {
            LOG.info("Data Catalog Details retrieved from input Topic File Format");
            retryUtil.retryTemplate(retryAttempts, retryInterval).execute(context -> registerMessageSchema(enms));
            retryUtil.retryTemplate(retryAttempts, retryInterval).execute(context -> outputTopicSetup = setupOutputTopic());

            LOG.info("Message Schema registered in Data Catalog");
        } catch (Exception e) {
            LOG.error("Failed to register in Data Catalog: {}", e.getMessage());
            LOG.debug("Stack trace - Failed to register in Data Catalog:", e);
            return false;
        }
        return true;
    }



    private boolean retrieveDataCatalogEntries() throws NullPointerException, NotFoundException {
        List<MessageSchemaV2> inputTopicMessageSchemaV2ResponseList = getMessageSchemaBasedOnTopic(inputTopicPrefix);
        MessageSchemaV2 inputTopicMessageSchemaV2Response = inputTopicMessageSchemaV2ResponseList.get(0);
        if (inputTopicMessageSchemaV2Response.getMessageDataTopic().getMessageBus().getName() != null) {

            DataSpace dataSpace = inputTopicMessageSchemaV2Response.getMessageDataTopic().getDataProviderType().getDataSpace();
            messageBus = inputTopicMessageSchemaV2Response.getMessageDataTopic().getMessageBus();

            return (dataSpace.getName() != null && messageBus != null);
        }

        LOG.error("Input Topic Message Bus Not Available From Data Catalog");
        throw new NullPointerException(INPUT_TOPIC_MESSAGE_BUS_UNAVAILABLE);
    }

    private boolean setupOutputTopic() {
        String outputTopicName = outputTopicService.getOutputTopicName();
        LOG.info("OutputTopic name set to: {}", outputTopicName);
        return outputTopicService.createKafkaOutputTopic();
    }

    /**
     * Registers Message Schema in Data Catalog through REST calls
     * If response returns 409 (already exists) or 401 (failed to register) the DTO will be set by
     * retrieving the existing Message Schema in Data Catalog if available else null pointer exception is thrown
     *
     * @return Response Entity Contain the MessageSchema DTO
     */
    private List<MessageSchemaV2> registerMessageSchema(List<String> enms) {
        LOG.debug("Register Message Schema");
        List<MessageSchemaV2> listMessageSchemaV2= new ArrayList<>();
        for(String enm : enms){
            MessageSchemaPutRequest messageSchemaRequest = createMessageSchemaRequest(enm);
            ResponseEntity<MessageSchemaV2> messageSchemaResponseEntity = dataCatalogService.registerMessageSchema(messageSchemaRequest);
            if (messageSchemaResponseEntity.getStatusCode().is2xxSuccessful()) {
                listMessageSchemaV2.add(messageSchemaResponseEntity.getBody());
            }

            if (messageSchemaResponseEntity.getStatusCode().value() == 409) {
                LOG.info("Unable to register Message Schema as it already exists");
                LOG.info("The message schema request sent to Data Catalog : {}", messageSchemaRequest);
                LOG.info("Existing message Schema: {}", getMessageSchemaBasedOnTopic(outputTopicService.getOutputTopicName()));
                listMessageSchemaV2.add(messageSchemaResponseEntity.getBody());
            } else  if (messageSchemaResponseEntity.getStatusCode().value() == 408) {
                LOG.info("Unable to register Message Schema as another process is already attempting to register. We should retry");
                throw new RestClientException("Unable to register Message Schema. Error code from Data Catalog: " + messageSchemaResponseEntity.getStatusCode());
            } else  if (messageSchemaResponseEntity.getStatusCode().is4xxClientError()) {
                LOG.error("Unable to register Message Schema. Error code: {}", messageSchemaResponseEntity.getStatusCode());
                LOG.info("The message schema request sent to Data Catalog : {}", messageSchemaRequest);
                LOG.info("Existing message Schema: {}", getMessageSchemaBasedOnTopic(outputTopicService.getOutputTopicName()));
                throw new RestClientException("Unable to register Message Schema. Error code from Data Catalog: " + messageSchemaResponseEntity.getStatusCode());
            }
        }
        if (listMessageSchemaV2.isEmpty()) {
            LOG.error("Unable to register Message Schema, params not set correctly");
            throw new NullPointerException("Message Schema Params Not Set Correctly");
        }

        return listMessageSchemaV2;

    }

    public List<MessageSchemaV2> getMessageSchemaBasedOnTopic(String topicPrefix) {
        LOG.debug("Get Input Topic details in Message Schema V2 format");
        List<MessageSchemaV2> messageSchemaV2List = new ArrayList<>();
        ResponseEntity<MessageSchemaListV2> responseEntity =
                dataCatalogServiceV2.getMessageSchemaListV2ByDataSpaceAndDataCategory(dataCatalogProperties.getDataSpace(), dataCatalogProperties.getDataCategory());
        MessageSchemaListV2 messageSchemaListV2 = responseEntity.getBody();
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            for (MessageSchemaV2 messageSchemaV2 : messageSchemaListV2) {
                if (MessageSchemaV2Utils.canGetTopicName(messageSchemaV2) &&
                        MessageSchemaV2Utils.canGetDataSpaceName(messageSchemaV2) &&
                        messageSchemaV2.getMessageDataTopic().getName().startsWith(topicPrefix) &&
                        messageSchemaV2.getMessageDataTopic().getDataProviderType().getDataSpace().getName().equals(dataCatalogProperties.getDataSpace())) {
                    messageSchemaV2List.add(messageSchemaV2);
                }
            }
        }
        if (!messageSchemaV2List.isEmpty()) {
            return messageSchemaV2List;
        }
        LOG.error("Input topic not available yet");
        throw new NotFoundException(INPUT_TOPIC_MESSAGE_BUS_STATUS_TOPIC_NAME_DATA_SPACE_UNAVAILABLE);
    }

    public List<String> getInputTopicList() {
        List<MessageSchemaV2> inputMessageSchemaV2List = getMessageSchemaBasedOnTopic(inputTopicPrefix);
        return inputMessageSchemaV2List.stream().map(MessageSchemaV2::getMessageDataTopic).toList().stream()
                .map(MessageDataTopicV2::getName).collect(Collectors.toList());
    }

    public boolean isApplicationReady() {
        return isApplicationReady;
    }

    public void setApplicationReady(boolean applicationReady) {
        isApplicationReady = applicationReady;
    }

    public void sleepInSeconds(int sleepInSeconds) throws InterruptedException {
        TimeUnit.SECONDS.sleep(sleepInSeconds);
    }

    public List<String> getAllEnmsFromDataCatalog () {
        List<String> topicList = getInputTopicList();
        List<String> enmList = new ArrayList();
        for (String inputTopicName : topicList) {
            String enmName = inputTopicName.substring(inputTopicPrefix.trim().length());
            enmList.add(enmName);
        }
        return enmList;
    }
}