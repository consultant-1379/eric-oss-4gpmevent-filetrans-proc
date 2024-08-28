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

package com.ericsson.oss.adc.kafka_components;

import com.ericsson.oss.adc.config.KafkaHeadersConfig;
import com.ericsson.oss.adc.models.EventHeader;
import com.ericsson.oss.adc.service.output.topic.OutputTopicService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static com.ericsson.oss.adc.config.KafkaHeadersConfig.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(topics = {"4g-pm-event-file-transfer-and-processing"}, partitions = 3,
        brokerProperties = {"transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1"})
@ActiveProfiles("test")
class KafkaMesssageTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMesssageTest.class);

    @Autowired
    OutputTopicService outputTopicService;

    @Autowired
    KafkaAdmin kafkaAdmin;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaTemplate<String, byte []> kafkaOutputTemplate;

   @MockBean
   private Producer<String, byte[]> producer;

    @MockBean
    private KafkaTemplate<String, byte [] > kafkaOutputTemplateMock;

    @Autowired
    private KafkaHeadersConfig kafkaHeadersConfig;

    @Captor
    ArgumentCaptor<ProducerRecord<String, byte[]>> produerArgumentCaptor;


    @Test
    @DisplayName("Send successfully message to Kafka")
    void testSendMessage() {
        byte[] recordHeader = new byte[]{0, -76, 0, 4, 0, 0, 8, 6, 45, 6, 1, -8, 4, 0, 0, 0, 8, -76, -92, 11, 4, 16, -13, 18, -44, 20, 97, 0, 5, -11, 16, -59, 69, 40, 5};
        byte[] recordData = new byte[]{-48, -96, -12, 0, 24, 29, 1, 0, -125, 32, 27, -86, 0, 64, 17, 0, 34, 16, 2, 37, -5, -60, -104, 64, -82, 38, 127, -31, -110, -73, -17, -101, -86, 0, -46, 0, 3, 0, 32, 40, 9, -63, 126, 117, 87, -96, -76, 0, -54, 0, -64, 104, 5, 68, -47, 16, 15, 93, -114, 43, 49, 9, -93, 32, 30, -69, 28, 87, -94, 39, -24, 1, -69, -92, -114, -92, -2, 84, 62, 0, 12, 0, 12, 6, 7, -123, 13, 4, -127, -80, 66, 0, 92, 80, 110, -48, 48, 13, 64, 0, -100, 40, 66, 63, 9, -127, 24, 36, 4, -84, 38, -124, 112, -110, 18, -64, -111, 19, 83, 0, 113, 24, 36, -62, 10, -87, 2, -26, 64, 90, 8, -116, -128, 0, 0, 0, 76, 35, 0, 0, 0, 0, 108, 0, 4};
        byte[] combinedRecord = new byte[recordHeader.length + recordData.length];

        EventHeader eventHeader = new EventHeader();
        eventHeader.setEventId(5);
        eventHeader.setDataVersion("abc");
        eventHeader.setNodeName("4GNode");

        ReflectionTestUtils.setField(outputTopicService, "partitions", 1);

        outputTopicService.init();
        outputTopicService.sendKafkaMessage(combinedRecord, eventHeader);

        verify(kafkaOutputTemplateMock, times(1)).send(produerArgumentCaptor.capture());
        ProducerRecord<String, byte []> cptrProducerRecord = produerArgumentCaptor.getValue();
        String cptrEventId = new String(cptrProducerRecord.headers().headers(EVENT_ID).iterator().next().value());
        String cptrNodeName = new String(cptrProducerRecord.headers().headers(NODE_NAME).iterator().next().value());
        String cptrDataVersion = new String(cptrProducerRecord.headers().headers(DATA_VERSION).iterator().next().value());
        String cptrVersion = new String(cptrProducerRecord.headers().headers(VERSION).iterator().next().value());

        Assertions.assertEquals("4g-pm-event-file-transfer-and-processing", cptrProducerRecord.topic());
        Assertions.assertEquals(cptrEventId, Integer.toString(eventHeader.getEventId()));
        Assertions.assertEquals(cptrNodeName, eventHeader.getNodeName());
        Assertions.assertEquals(cptrDataVersion, eventHeader.getDataVersion());
        Assertions.assertEquals(cptrVersion, Integer.toString(kafkaHeadersConfig.kafkaEventHeaderVersion));
        Assertions.assertTrue(Arrays.equals(combinedRecord, cptrProducerRecord.value()));
    }
}
