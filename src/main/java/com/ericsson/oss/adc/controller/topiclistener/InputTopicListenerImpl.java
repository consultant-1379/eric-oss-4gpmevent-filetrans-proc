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

package com.ericsson.oss.adc.controller.topiclistener;


import com.ericsson.oss.adc.config.KafkaConfiguration;
import com.ericsson.oss.adc.models.InputMessage;
import com.ericsson.oss.adc.models.connected.systems.Subsystem;
import com.ericsson.oss.adc.service.file.processor.FileProcessorService;
import com.ericsson.oss.adc.service.sftp.SFTPService;
import com.ericsson.oss.adc.service.kafka.KafkaConsumerService;
import com.ericsson.oss.adc.util.ConsumerRecordSkipException;
import com.ericsson.oss.adc.util.ConsumerRecordRollbackException;
import com.ericsson.oss.adc.util.FileTransferMetricsUtil;
import com.ericsson.oss.adc.util.StartupUtil;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation for listening to 4G ENM event file notification topic to consume event files
 */
@Component
@EnableKafka
public class InputTopicListenerImpl {
    /**
     * Logger for the class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(InputTopicListenerImpl.class);

    @Value("${spring.kafka.topics.input.prefix}")
    private String inputTopicPrefix;


    private SFTPService sftpService;
    private FileProcessorService fileProcessorService;


    @Autowired
    StartupUtil startupUtil;

    @Autowired
    private KafkaConsumerService kafkaConsumerService;

    @Autowired
    KafkaConfiguration kafkaConfiguration;

    @Autowired
    FileTransferMetricsUtil metrics;

    private static final String NODE_NAME = "nodeName";
    private static final String SUB_SYSTEM_TYPE = "subSystemType";
    private static final String NODE_TYPE = "nodeType";
    private static final String DATA_TYPE = "dataType";

    private Lock lock = new ReentrantLock();

    private Set<String> currentListeningTopics = new HashSet<>();


    @Autowired
    public InputTopicListenerImpl(final SFTPService sftpService, final FileProcessorService fileProcessorService) {
        this.sftpService = sftpService;
        this.fileProcessorService = fileProcessorService;
    }

    /**
     * Listner method will receive the Input topic list form datacatlog and passing to the register Listner method
     * heartbeat to enm sftp server, in a rare scenario if enm fls is up and running and sftp is down case
     * FNS start still sends message to download the files
     * as sftp is down these will throw exception and offset commit fails as one sftp is down
     */
    @Scheduled(fixedDelay =  1 * 60 * 1000, initialDelay = 2 * 1000)
    public void startOrStopMessageContainers() throws InterruptedException {
        LOG.info("------heart beat check sftp connections start------");
        lock.lock();
        try {
            if (!startupUtil.isApplicationReady()) {
                LOG.info("application still not ready skipping");
                return;
            }
            List<String> inputTopics = startupUtil.getInputTopicList();
            Map<String, Subsystem> subsystemMap = sftpService.getSubsystemDetails();

            Set<String> sftpUpTopics = new HashSet<>();
            Set<String> sftpDownTopics = new HashSet<>();
            for (String enm : subsystemMap.keySet()) {
                for (String topic : inputTopics) {
                    //check topic belongs to this enm
                    if (topic.endsWith(enm)) {
                        //do not eject fast, retry 3 times with 5 seconds gap if still fails eject the message container
                        boolean isUp = checkSftpConnection(enm, topic, 3, 5);
                        if(isUp) {
                            sftpUpTopics.add(topic);
                        } else {
                            LOG.error("sftp connection is still not available the topic {} exception", topic);
                            sftpDownTopics.add(topic);
                        }
                    }
                }
            }

            checkAndRestartContainer(sftpUpTopics, sftpDownTopics);
        } catch(InterruptedException ex) {
            LOG.error("InterruptedException ex:", ex);
            throw ex;
        } catch (Exception ex) {
            LOG.error("exception in startOrStopMessageContainers scheduler", ex);
        } finally {
            lock.unlock();
        }
        LOG.info("------heart beat check sftp connections end------");
    }

    protected void checkAndRestartContainer(Set<String> sftpUpTopics, Set<String> sftpDownTopics) {
        boolean changeDetected = isChangeDetected(sftpUpTopics);

        if (changeDetected) {
            LOG.info("change detected in the sftp topics listen current running topics: {}, sftpUpTopics: {}, sftpDownTopics: {}"
                    , currentListeningTopics, sftpUpTopics, sftpDownTopics);
            //stop and start
            kafkaConsumerService.stop();
            if (!sftpUpTopics.isEmpty()) {
                kafkaConsumerService.register(sftpUpTopics, this::processRecord);
                kafkaConsumerService.start();
            }
            currentListeningTopics.clear();
            currentListeningTopics.addAll(sftpUpTopics);
        } else {
            LOG.info("no change detected in the sftp topics listen current running topics: {}, sftpUpTopics: {}, sftpDownTopics: {}"
                    , currentListeningTopics, sftpUpTopics, sftpDownTopics);
        }
    }

    private boolean isChangeDetected(Set<String> sftpUpTopics) {
        boolean changeDetected;

        if (currentListeningTopics.isEmpty() && sftpUpTopics.isEmpty()) {
            //both are empty
            changeDetected = false;
        } else if (currentListeningTopics.isEmpty() || sftpUpTopics.isEmpty()) {
            //one of them is empty
            changeDetected = true;
        } else if (currentListeningTopics.size() != sftpUpTopics.size()) {
            changeDetected = true;
        } else if (!currentListeningTopics.containsAll(sftpUpTopics)) {
            changeDetected = true;
        } else {
            changeDetected = false;
        }
        return changeDetected;
    }

    private boolean checkSftpConnection(String enm, String topic, int attempts, int sleepInSeconds) throws InterruptedException {
        for (int i = 0; i < attempts; i++) {
            try {
                LOG.info("checking sftp connection enm: {}, topic: {}", enm, topic);
                if (sftpService.checkSubsystemsConnection(enm)) {
                    LOG.info("sftp connection success -- kafkaListener register the topic {}", topic);
                    return true;
                }
                //retry after 10 seconds
                startupUtil.sleepInSeconds(sleepInSeconds);
            } catch(InterruptedException ex) {
                LOG.error("error in SECONDS sleep enm: {}, topic: {}", enm, topic, ex);
                throw ex;
            } catch(Exception ex) {
                LOG.error("sftp connection failure topic {} enm {} exception", topic, enm, ex);
            }
        }
        return false;
    }

    /**
     * Getting the consumer record from the topic and ObjectMapper to deserialize JSON data from a consumerRecord.value() into an instance of the InputMessage class.
     *
     * @param consumerRecords
     */
    public void processRecord(List<ConsumerRecord<String, InputMessage>> consumerRecords) {
        LOG.info("Batch '{}' received from topic of size: {}", consumerRecords.hashCode(), consumerRecords.size());
        long start = System.currentTimeMillis();
        metrics.printAllCounterValues("Output Topic Records Count");
        for(ConsumerRecord<String, InputMessage> consumerRecord: consumerRecords) {
            try {
                String topicName = consumerRecord.topic();
                Headers headers = consumerRecord.headers();
                Map<String, String> headerMap = new HashMap<>();
                for (Header header : headers) {
                    headerMap.put(header.key(), new String(header.value(), StandardCharsets.UTF_8));
                }
                InputMessage inputMessage = consumerRecord.value();
                InputMessage updInputMessage = constructInputMessage(headerMap, inputMessage);
                processInputMessage(updInputMessage, topicName);
            } catch (ConsumerRecordRollbackException ex) {
                LOG.error("Exception while processing the record -- rethrow to rollback the offsets and retry from kafka {}", consumerRecord, ex);
                throw ex;
            } catch (ConsumerRecordSkipException ex) {
                LOG.error("Exception while processing the record -- suppressing the file so it is not retried {}", consumerRecord, ex);
            }
        }
        metrics.printAllCounterValues("Output Topic Records Count");
        long end = System.currentTimeMillis();
        LOG.info("Finished processing batch '{}' of size {} processing time {}.", consumerRecords.hashCode(), consumerRecords.size(), (end-start));
    }

    /**
     * this will take all the header information and payload data,it will construct the InputMessage pojo
     *
     * @param headerMap
     * @param message
     * @return
     */
    public InputMessage constructInputMessage(Map<String, String> headerMap, InputMessage message) {
        return InputMessage.builder()
                .fileLocation(message.getFileLocation())
                .nodeName(message.getNodeName())
                .subSystemType(headerMap.get(SUB_SYSTEM_TYPE))
                .nodeType(message.getNodeType())
                .dataType(message.getDataType())
                .build();
    }

    /**
     * Non null records passed to the sftp service and download the files and Process the message records.
     *
     * @param consumerRecord
     * @param topicName
     * @throws ConnectException
     */
    public void processInputMessage(final InputMessage consumerRecord, String topicName) throws ConsumerRecordRollbackException, ConsumerRecordSkipException {

        String enmName = null;
        if (topicName.startsWith(inputTopicPrefix)) {
            enmName = topicName.substring(inputTopicPrefix.length());
            LOG.debug("enm-name{} from the topic", enmName);
        }

        if (consumerRecord != null) {
            final InputMessage fileNotificationsWithDownloadedFiles = sftpService.downloadSftpFile(consumerRecord, enmName);

            if (fileNotificationsWithDownloadedFiles != null) {
                if (fileNotificationsWithDownloadedFiles.getDownloadedFile() == null) {
                    LOG.error("Failed to to download the file {}, multiple retries exhausted skipping the file:",
                            consumerRecord.getFileLocation());
                } else {
                    fileProcessorService.processEventFile(fileNotificationsWithDownloadedFiles.getDownloadedFile(), fileNotificationsWithDownloadedFiles.getNodeName());
                }
            }
        }
    }
}
