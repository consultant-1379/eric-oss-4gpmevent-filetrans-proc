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


import com.ericsson.oss.adc.models.InputMessage;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.listener.BatchMessageListener;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Kafka runtime listener
 * It does not provide new topic listening at runtime.
 * It provides new topic registration, de-registration, start, stop at runtime.
 */
@Service
@Slf4j
public class KafkaConsumerService {


    private static final Logger LOG = LoggerFactory.getLogger(KafkaConsumerService.class);
    private final KafkaListenerContainerFactory kafkaListenerContainerFactory;
    private Optional<MessageListenerContainer> messageListenerContainerOpt = Optional.empty();

    private final Lock lock;

    public KafkaConsumerService(KafkaListenerContainerFactory kafkaListenerContainerFactory) {
        LOG.info(" KafkaConsumerService  {}", kafkaListenerContainerFactory);
        this.kafkaListenerContainerFactory = kafkaListenerContainerFactory;
        this.lock = new ReentrantLock();
    }

    /**
     * Kafka listener registration at runtime.
     **/
    public void register(final Set<String> topics, final BatchMessageListener<String, InputMessage> messageListener) {
        LOG.info("kafkaListener register method inside");

        lock.lock();
        try {
            if (topics.isEmpty()) {
                return;
            }
            for (String topic : topics) {
                LOG.info("kafkaListener topics----{}", topic);
            }

            MessageListenerContainer messageListenerContainer = kafkaListenerContainerFactory.createContainer(topics.toArray(String[]::new));
            messageListenerContainer.setupMessageListener(messageListener);
            messageListenerContainerOpt = Optional.of(messageListenerContainer);
        } finally {
            lock.unlock();
        }
    }

    public void start() {
        lock.lock();
        try {
            if (messageListenerContainerOpt.isPresent()) {
                MessageListenerContainer messageListenerContainer = messageListenerContainerOpt.get();
                if (messageListenerContainer.isRunning()) {
                    LOG.info("message kafka listener already started and container is running-----{}", messageListenerContainer.isRunning());
                    return;
                }
                messageListenerContainer.start();
                LOG.info("container else condition is running-----{}", messageListenerContainer.isRunning());
            }
        } finally {
            lock.unlock();
        }
    }

    public void stop() {
        lock.lock();
        try {

            if (messageListenerContainerOpt.isPresent()) {
                MessageListenerContainer messageListenerContainer = messageListenerContainerOpt.get();
                if (!messageListenerContainer.isRunning()) {
                    log.info("File available message Kafka listener already stopped");
                    return;
                }
                messageListenerContainer.stop();
                messageListenerContainerOpt = Optional.empty();
                log.info("File available message Kafka listener stopped");
            }
        } finally {
            lock.unlock();
        }
    }
}

