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
package com.ericsson.oss.adc.service.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.listener.BatchMessageListener;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;

@EmbeddedKafka(partitions = 1, ports = 9091)
@SpringBootTest(classes = {KafkaConsumerService.class})
public class KafkaConsumerServiceTest {

    Set<String> topics = new HashSet<>(Arrays.asList("topic1", "topic2", "topic3"));

    private BatchMessageListener messageListener = consumerRecords -> {
        System.out.println(consumerRecords);
    };

    @MockBean
    private KafkaListenerContainerFactory kafkaListenerContainerFactory;

    @MockBean
    private MessageListenerContainer messageListenerContainer;

    private KafkaConsumerService kafkaConsumerService;

    @BeforeEach
    public void setUp() {
        when(kafkaListenerContainerFactory.createContainer(ArgumentMatchers.<String>any())).thenReturn(messageListenerContainer);
        kafkaConsumerService = new KafkaConsumerService(kafkaListenerContainerFactory);
    }

    @Test
    public void testRegister() {
        kafkaConsumerService.register(topics, messageListener);
        verify(messageListenerContainer, times(0)).start();
    }

    @Test
    public void testRegister_Empty() {
        kafkaConsumerService.register(new HashSet<>(), messageListener);
        verify(messageListenerContainer, times(0)).start();
    }

    @Test
    void testStart_RunningContainer() {
        kafkaConsumerService.register(topics, messageListener);
        kafkaConsumerService.start();
        verify(messageListenerContainer, times(1)).start(); // Verify that start() was called on the container
    }

    @Test
    void testStart_StoppedContainer() {
        kafkaConsumerService.register(topics, messageListener);
        when(messageListenerContainer.isRunning()).thenReturn(false);
        kafkaConsumerService.start();
        verify(messageListenerContainer, times(1)).start();
    }


    @Test
    void testStop_StoppedContainer() {
        when(messageListenerContainer.isRunning()).thenReturn(false);
        kafkaConsumerService.register(topics, messageListener);

        kafkaConsumerService.stop();
        verify(messageListenerContainer, times(0)).stop(); // Verify that stop() was called on the container
    }

    @Test
    void testStop_RunningContainer() {
        when(messageListenerContainer.isRunning()).thenReturn(true);
        kafkaConsumerService.register(topics, messageListener);
        kafkaConsumerService.stop();
        verify(messageListenerContainer, times(1)).stop(); // Verify that stop() was called on the container
    }
}
