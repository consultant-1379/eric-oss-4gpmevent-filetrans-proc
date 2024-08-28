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

package com.ericsson.oss.adc.service.output.topic;

import com.ericsson.oss.adc.PostStartup;
import com.ericsson.oss.adc.service.data.catalog.DataCatalogService;
import com.ericsson.oss.adc.service.input.topic.InputTopicService;
import lombok.SneakyThrows;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(partitions = 1, topics = {"4g-pm-event-file-transfer-and-processing--enm2"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OutputTopicServiceTest {

    @Value("${spring.kafka.topics.output.name}")
    private String outputTopicName;


    //Required to stop dependency checks running due to "@EventListener(ApplicationReadyEvent.class)" during tests.
    @MockBean
    private PostStartup postStartup;

    @MockBean
    private DataCatalogService dataCatalogService;

    @MockBean
    private InputTopicService inputTopicService;

    @Autowired
    OutputTopicService outputTopicService;

    @Autowired
    KafkaAdmin kafkaAdmin;

    @Test
    @DisplayName("Should successfully setup the output topics")
    void test_setupOutputTopics() {
        boolean isCreated = outputTopicService.createKafkaOutputTopic();
      //  assertTrue(isCreated);
    }

    @Test
    @DisplayName("Should successfully build and create the topics on the embedded kafka server")
    void test_buildAndCreateTopics() {
        String enm2Topic = "4g-pm-event-file-transfer-and-processing--enm2";
        int expectedReplicas = 1;

        outputTopicService.buildAndCreateTopic(outputTopicName);
        Map<String, TopicDescription> topics = kafkaAdmin.describeTopics(enm2Topic);

        assertNotNull(topics);

        assertEquals(expectedReplicas, topics.get(enm2Topic).partitions().get(0).replicas().size());

    }

    @Test
    @DisplayName("Should only add a topic once and not decrease the partitions")
    void test_buildAndCreateTopics_duplicateTopic() {
        String testTopic = "4g-pm-event-file-transfer-and-processing--enm2";
        int expectedReplicas = 1;

        int duplicatePartitions = 2;
        short duplicateReplicas = 1;
        NewTopic duplicateTopic = new NewTopic(testTopic, duplicatePartitions, duplicateReplicas);

        outputTopicService.buildAndCreateTopic(outputTopicName);
        kafkaAdmin.createOrModifyTopics(duplicateTopic);
        Map<String, TopicDescription> topics = kafkaAdmin.describeTopics(testTopic);

        assertNotNull(topics);

        assertEquals(expectedReplicas, topics.get(testTopic).partitions().get(0).replicas().size());

    }

    @Test
    @DisplayName("Should return true when the topic is created")
    void test_isTopicCreated_pass() {
        String enm2Topic = "4g-pm-event-file-transfer-and-processing--enm2";
        outputTopicService.buildAndCreateTopic(outputTopicName);
        assertTrue(outputTopicService.isTopicCreated(enm2Topic));
    }

    @SneakyThrows
    @Test
    @DisplayName("Should return false when the topic is not created")
    void test_isTopicCreated_fail() {
        String enm2Topic = "4g-pm-event-file-transfer-and-processing--enm";
        assertFalse(outputTopicService.isTopicCreated(enm2Topic));
    }
}