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

package com.ericsson.oss.adc;

import com.ericsson.oss.adc.availability.*;
import com.ericsson.oss.adc.controller.topiclistener.InputTopicListenerImpl;
import com.ericsson.oss.adc.models.data.catalog.v2.MessageSchemaV2;
import com.ericsson.oss.adc.service.sftp.SFTPService;
import com.ericsson.oss.adc.util.StartupUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PostStartup {

    private static final Logger LOG = LoggerFactory.getLogger(PostStartup.class);

    @Autowired
    private StartupUtil startupUtil;

    @Autowired
    private DependentServiceAvailabilityKafka dependentServiceAvailabilityKafka;

    @Autowired
    private DependentServiceAvailabilityDataCatalog dependentServiceAvailabilityDataCatalog;

    @Autowired
    private DependentServiceAvailabilityConnectedSystems dependentServiceAvailabilityConnectedSystems;

    @Autowired
    private DependentServiceAvailabilityScriptingVm dependentServiceAvailabilityScriptingVm;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Autowired
    private SFTPService sftpService;

    @Autowired
    private InputTopicListenerImpl inputTopicListenerImpl;

    @Value("${spring.kafka.topics.input.prefix}")
    private String inputTopicPrefix;




    /**
     * Async Listener for when the spring boot application is started and ready. Needs to be Async so that liveliness and readiness probes are
     * unaffected by retries.
     *
     * @throws UnsatisfiedExternalDependencyException This exception cannot be thrown as it is in effectively an infinite retry.
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void afterStartUp() throws UnsatisfiedExternalDependencyException, InterruptedException {
        //Query if DataCatalog is available through health check, then ensure relevant info is available.
        if (dependentServiceAvailabilityDataCatalog.checkService() && startupUtil.queryDataCatalogForInputTopic()) {
            LOG.info("Input topic details received from data catalog ");
        }
        if (dependentServiceAvailabilityKafka.checkService()
                && dependentServiceAvailabilityConnectedSystems.checkService()
                && sftpService.checkSubsystemDetails()
                && dependentServiceAvailabilityScriptingVm.checkService()
        ) {
            List<MessageSchemaV2> inputTopicMessageSchemaV2ResponseList = startupUtil.getMessageSchemaBasedOnTopic(inputTopicPrefix.trim());
            List<String> enmList=new ArrayList();
            for(MessageSchemaV2 messageSchemaV2:inputTopicMessageSchemaV2ResponseList) {
                String inputTopicName = messageSchemaV2.getMessageDataTopic().getName();
                String  enmName = inputTopicName.substring(inputTopicPrefix.trim().length());
                enmList.add(enmName);
            }
            startupUtil.registerInDataCatalog(enmList);
            LOG.info("setting application as ready");
            startupUtil.setApplicationReady(true);
            inputTopicListenerImpl.startOrStopMessageContainers();
        }

    }

}
