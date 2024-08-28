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

import com.ericsson.oss.adc.config.KafkaHeadersConfig;
import com.ericsson.oss.adc.models.EventHeader;
import com.ericsson.oss.adc.service.data.catalog.DataCatalogServiceV2;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.TopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

import static com.ericsson.oss.adc.config.KafkaHeadersConfig.*;
import static java.nio.charset.StandardCharsets.UTF_8;




/**
 * Implementation for creating the Kafka output topic
 */
@Component
public class OutputTopicService {

    @Value("${spring.kafka.topics.output.name}")
    private String outputTopicName;

    @Autowired
    private KafkaAdmin kafkaAdmin;


    @Value("${spring.kafka.topics.output.partitions}")
    private int partitions;

    @Value("${spring.kafka.topics.output.replicas}")
    private short replicas;

    @Value("${spring.kafka.topics.output.retentionPeriodMS}")
    private String retentionPeriodMS;

    @Value("${spring.kafka.topics.output.retentionBytesTopic}")
    private String retentionBytesPerTopic;

    private static final String COMPRESSION_TYPE = "producer";

    private static final Logger LOG = LoggerFactory.getLogger(OutputTopicService.class);

    @Autowired
    private DataCatalogServiceV2 dataCatalogServiceV2;

    @Autowired
    private KafkaHeadersConfig kafkaHeadersConfig;

    @Autowired
    private KafkaTemplate<String, byte [] > kafkaOutputTemplate;

    private byte[] kafkaEventHeaderVersionBytes;

    private final ReentrantLock lock = new ReentrantLock();
    private final byte[][] eventIdCache = new byte[10_000][];

    public void init() {
        kafkaEventHeaderVersionBytes = Integer.toString(kafkaHeadersConfig.kafkaEventHeaderVersion).getBytes(UTF_8);
    }

    /**
     * Construct the output topic name and create the topic in Kafka
     */
    public boolean createKafkaOutputTopic() {
        LOG.info("Creating the output topic");
        init();
        buildAndCreateTopic(outputTopicName);
        return isTopicCreated(outputTopicName);
    }




    public String getOutputTopicName() {
        return outputTopicName;
    }

    /**
     * Build the output topic and create it on kafka server
     */
    protected void buildAndCreateTopic(String outputTopicName) {
        final String SEGMENT_MS = "300000";
        BigInteger bytesPerPartition = new BigInteger(retentionBytesPerTopic).divide(BigInteger.valueOf(partitions));

        LOG.info("Creating or attempting to modify " +
                        "topic: '{}', partitions: '{}', replicas: '{}', " +
                        "compressionType: '{}', retentionPeriodMs: '{}, retentionBytesPerTopic: {}', retentionBytesPerPartition: {}",
                outputTopicName, partitions, replicas, COMPRESSION_TYPE, retentionPeriodMS,retentionBytesPerTopic,bytesPerPartition);
        NewTopic outputTopic = TopicBuilder.name(outputTopicName)
                .partitions(partitions)
                .replicas(replicas)
                .config(TopicConfig.COMPRESSION_TYPE_CONFIG, COMPRESSION_TYPE)
                .config(TopicConfig.RETENTION_MS_CONFIG, retentionPeriodMS)
                .config(TopicConfig.RETENTION_BYTES_CONFIG, bytesPerPartition.toString())
                .config(TopicConfig.SEGMENT_MS_CONFIG, SEGMENT_MS)
                .build();

        // NOTE: this only creates a topic or changes the partition count
        kafkaAdmin.createOrModifyTopics(outputTopic);
    }

    /**
     * Does the output topic exist in Kafka
     *
     * @param outputTopicName string of topic name to check if it has been created on kafka server
     * @return boolean to indicate the creation of topic
     */
    protected boolean isTopicCreated(final String outputTopicName) {
        try (AdminClient client = getAdminClient()) {
            Set<String> existingTopics = client.listTopics().names().get();
            if (!existingTopics.contains(outputTopicName)) {
                LOG.debug("Topic was not created: {}", outputTopicName);
                return false;
            }
        } catch (ExecutionException | InterruptedException e) {
            LOG.error("Error checking topic creation: {}", e.getMessage());
            LOG.debug("Stack trace - Error checking topic creation: ", e);
            Thread.currentThread().interrupt();
            return false;
        }
        return true;
    }

    /**
     * Creates and returns an {@link AdminClient} instance with bootstrap server configured.
     *
     * @return {@link AdminClient} instance
     */
    protected AdminClient getAdminClient() {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }

    /**
     * Publish the record to Kafka output topic
     *
     * @param combinedRecord the header and body  to write to kafka topic
     * @param eventHeader     record data
     */

    public void sendKafkaMessage(byte[] combinedRecord, EventHeader eventHeader) {
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(outputTopicName, eventHeader.getNodeName(), combinedRecord);
        int eventId = eventHeader.getEventId();
        if (eventId < 10_000) {
            if (eventIdCache[eventId] == null) {
                lock.lock();
                try {
                    eventIdCache[eventId] = Integer.toString(eventId).getBytes(StandardCharsets.UTF_8);
                } finally {
                    lock.unlock();
                }
            }
            record.headers().add(EVENT_ID, eventIdCache[eventId]);
        } else {
            //eventId generally does not go above 10_000
            record.headers().add(EVENT_ID, Integer.toString(eventId).getBytes(StandardCharsets.UTF_8));
        }
        record.headers().add(NODE_NAME, eventHeader.getNodeNameBytes());
        record.headers().add(TIME_STAMP, Long.toString(System.currentTimeMillis()).getBytes(UTF_8));
        record.headers().add(DATA_VERSION, eventHeader.getDataVersionBytes());
        record.headers().add(VERSION, kafkaEventHeaderVersionBytes);
        kafkaOutputTemplate.send(record);
    }
}
