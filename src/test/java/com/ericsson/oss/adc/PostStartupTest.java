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

import com.ericsson.oss.adc.availability.DependentServiceAvailabilityConnectedSystems;
import com.ericsson.oss.adc.availability.DependentServiceAvailabilityDataCatalog;
import com.ericsson.oss.adc.availability.DependentServiceAvailabilityKafka;
import com.ericsson.oss.adc.availability.DependentServiceAvailabilityScriptingVm;
import com.ericsson.oss.adc.config.kafka.BootStrapServerConfigurationSupplier;
import com.ericsson.oss.adc.controller.topiclistener.InputTopicListenerImpl;
import com.ericsson.oss.adc.models.data.catalog.v2.DataService;
import com.ericsson.oss.adc.models.data.catalog.v2.DataType;
import com.ericsson.oss.adc.models.data.catalog.v2.MessageDataTopicV2;
import com.ericsson.oss.adc.models.data.catalog.v2.MessageSchemaV2;
import com.ericsson.oss.adc.service.sftp.SFTPService;
import com.ericsson.oss.adc.util.StartupUtil;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {PostStartup.class, DependentServiceAvailabilityDataCatalog.class, DependentServiceAvailabilityKafka.class, StartupUtil.class, DependentServiceAvailabilityConnectedSystems.class, KafkaListenerEndpointRegistry.class, BootStrapServerConfigurationSupplier.class})
class PostStartupTest {

    @Autowired
    private PostStartup postStartup;

    @MockBean
    private StartupUtil startupUtil;

    @MockBean
    private DependentServiceAvailabilityKafka dependentServiceAvailabilityKafka;

    @MockBean
    private DependentServiceAvailabilityDataCatalog dependentServiceAvailabilityDataCatalog;

    @MockBean
    private DependentServiceAvailabilityConnectedSystems dependentServiceAvailabilityConnectedSystems;

    @MockBean
    private DependentServiceAvailabilityScriptingVm dependentServiceAvailabilityScriptingVm;

    @MockBean
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Mock
    private MessageListenerContainer messageListenerContainer;

    @MockBean
    SFTPService sftpService;

    @MockBean
    InputTopicListenerImpl inputTopicListener;

    List<String> mockTopicList = Arrays.asList("enm1", "enm2");

    @SneakyThrows
    @Test
    @DisplayName("Should register data on DataCatalog and start listener when services are available")
    void test_allServicesAreAvailable() {
        List<MessageSchemaV2> inputTopicMessageSchemaV2ResponseList=new ArrayList<>();
        MessageSchemaV2 messageSchemaV2 =new MessageSchemaV2(0,new MessageDataTopicV2(),"",new DataService(),new DataType());
        messageSchemaV2.getMessageDataTopic().setName("file-notification-service--4g-event--enm1");
        MessageSchemaV2 messageSchemaV1 =new MessageSchemaV2(0,new MessageDataTopicV2(),"",new DataService(),new DataType());
        messageSchemaV1.getMessageDataTopic().setName("file-notification-service--4g-event--enm2");
        inputTopicMessageSchemaV2ResponseList.add(messageSchemaV2);
        inputTopicMessageSchemaV2ResponseList.add(messageSchemaV1);
        when(dependentServiceAvailabilityScriptingVm.checkService()).thenReturn(true);
        when(startupUtil.getMessageSchemaBasedOnTopic("file-notification-service--4g-event--".trim())).thenReturn(inputTopicMessageSchemaV2ResponseList);
        when(dependentServiceAvailabilityDataCatalog.checkService()).thenReturn(true);
        when(startupUtil.queryDataCatalogForInputTopic()).thenReturn(true);
        when(dependentServiceAvailabilityKafka.checkService()).thenReturn(true);
        when(dependentServiceAvailabilityConnectedSystems.checkService()).thenReturn(true);
        when(kafkaListenerEndpointRegistry.getListenerContainer(any())).thenReturn(messageListenerContainer);
        when(messageListenerContainer.isAutoStartup()).thenReturn(false);
        when(messageListenerContainer.isRunning()).thenReturn(false);
        when(sftpService.checkSubsystemDetails()).thenReturn(true);
        doNothing().when(messageListenerContainer).start();
        postStartup.afterStartUp();
        verify(startupUtil, times(1)).setApplicationReady(true);
      //  verify(startupUtil, times(1)).registerInDataCatalog();
//        verify(inputTopicListener, times(1)).listener();
    }

    @SneakyThrows
    @Test
    @DisplayName("Should fail to register data on DataCatalog and start listener when services aren't available")
    void test_noServicesAreAvailable() {
        List<MessageSchemaV2> inputTopicMessageSchemaV2ResponseList=new ArrayList<>();
        MessageSchemaV2 messageSchemaV2 =new MessageSchemaV2(0,new MessageDataTopicV2(),"",new DataService(),new DataType());
        messageSchemaV2.getMessageDataTopic().setName("file-notification-service--4g-event--enm1");
        MessageSchemaV2 messageSchemaV1 =new MessageSchemaV2(0,new MessageDataTopicV2(),"",new DataService(),new DataType());
        messageSchemaV1.getMessageDataTopic().setName("file-notification-service--4g-event--enm2");
        inputTopicMessageSchemaV2ResponseList.add(messageSchemaV2);
        inputTopicMessageSchemaV2ResponseList.add(messageSchemaV1);
        when(dependentServiceAvailabilityScriptingVm.checkService()).thenReturn(false);
        when(dependentServiceAvailabilityDataCatalog.checkService()).thenReturn(false);
        when(startupUtil.getMessageSchemaBasedOnTopic("file-notification-service--4g-event--".trim())).thenReturn(inputTopicMessageSchemaV2ResponseList);
        when(dependentServiceAvailabilityKafka.checkService()).thenReturn(false);
        when(dependentServiceAvailabilityConnectedSystems.checkService()).thenReturn(false);
        when(kafkaListenerEndpointRegistry.getListenerContainer(any())).thenReturn(messageListenerContainer);
        when(messageListenerContainer.isAutoStartup()).thenReturn(false);
        when(messageListenerContainer.isRunning()).thenReturn(false);
        doNothing().when(messageListenerContainer).start();

        postStartup.afterStartUp();

        verify(startupUtil, times(0)).registerInDataCatalog(mockTopicList);
        verify(messageListenerContainer, times(0)).start();
    }

    @SneakyThrows
    @Test
    @DisplayName("Should fail to register data on DataCatalog when input topic entries are not available")
    void test_noDataCatalogEntriesArePresent() {
        when(dependentServiceAvailabilityScriptingVm.checkService()).thenReturn(true);
        when(dependentServiceAvailabilityDataCatalog.checkService()).thenReturn(true);
        when(startupUtil.queryDataCatalogForInputTopic()).thenReturn(false);
        when(dependentServiceAvailabilityKafka.checkService()).thenReturn(false);
        when(dependentServiceAvailabilityConnectedSystems.checkService()).thenReturn(true);
        when(kafkaListenerEndpointRegistry.getListenerContainer(any())).thenReturn(messageListenerContainer);
        when(messageListenerContainer.isAutoStartup()).thenReturn(false);
        when(messageListenerContainer.isRunning()).thenReturn(false);
        doNothing().when(messageListenerContainer).start();

        postStartup.afterStartUp();

        verify(startupUtil, times(0)).registerInDataCatalog(mockTopicList);
        verify(messageListenerContainer, times(0)).start();
    }

    @SneakyThrows
    @Test
    @DisplayName("Should not start Listener if is already running")
    void test_tryToStartListenerWhileAlreadyRunning() {
        List<MessageSchemaV2> inputTopicMessageSchemaV2ResponseList=new ArrayList<>();
        MessageSchemaV2 messageSchemaV2 =new MessageSchemaV2(0,new MessageDataTopicV2(),"",new DataService(),new DataType());
        messageSchemaV2.getMessageDataTopic().setName("file-notification-service--4g-event--enm1");
        MessageSchemaV2 messageSchemaV1 =new MessageSchemaV2(0,new MessageDataTopicV2(),"",new DataService(),new DataType());
        messageSchemaV1.getMessageDataTopic().setName("file-notification-service--4g-event--enm2");
        inputTopicMessageSchemaV2ResponseList.add(messageSchemaV2);
        inputTopicMessageSchemaV2ResponseList.add(messageSchemaV1);
        when(dependentServiceAvailabilityScriptingVm.checkService()).thenReturn(true);
        when(dependentServiceAvailabilityDataCatalog.checkService()).thenReturn(true);
        when(startupUtil.queryDataCatalogForInputTopic()).thenReturn(true);
        when(dependentServiceAvailabilityKafka.checkService()).thenReturn(true);
        when(dependentServiceAvailabilityConnectedSystems.checkService()).thenReturn(true);
        when(startupUtil.getMessageSchemaBasedOnTopic("file-notification-service--4g-event--".trim())).thenReturn(inputTopicMessageSchemaV2ResponseList);
        when(kafkaListenerEndpointRegistry.getListenerContainer(any())).thenReturn(messageListenerContainer);
        when(messageListenerContainer.isAutoStartup()).thenReturn(false);
        when(messageListenerContainer.isRunning()).thenReturn(true);
        doNothing().when(messageListenerContainer).start();
        postStartup.afterStartUp();

        verify(messageListenerContainer, times(0)).start();
    }

    @SneakyThrows
    @Test
    @DisplayName("Should not start Listener if autostart is true")
    void test_tryToStartListenerWhenAutoStartFalse() {
        when(dependentServiceAvailabilityScriptingVm.checkService()).thenReturn(true);
        when(dependentServiceAvailabilityDataCatalog.checkService()).thenReturn(true);
        when(startupUtil.queryDataCatalogForInputTopic()).thenReturn(true);
        when(dependentServiceAvailabilityKafka.checkService()).thenReturn(true);
        when(dependentServiceAvailabilityConnectedSystems.checkService()).thenReturn(true);
        when(kafkaListenerEndpointRegistry.getListenerContainer(any())).thenReturn(messageListenerContainer);
        when(messageListenerContainer.isAutoStartup()).thenReturn(true);
        when(messageListenerContainer.isRunning()).thenReturn(false);
        doNothing().when(messageListenerContainer).start();
        postStartup.afterStartUp();

        verify(messageListenerContainer, times(0)).start();
    }

    @SneakyThrows
    @Test
    @DisplayName("Should set up subsystem details and start listener when services are available")
    void test_SubsystemAvailable() {
        when(dependentServiceAvailabilityScriptingVm.checkService()).thenReturn(true);
        when(dependentServiceAvailabilityDataCatalog.checkService()).thenReturn(true);
        when(startupUtil.queryDataCatalogForInputTopic()).thenReturn(true);
        when(dependentServiceAvailabilityKafka.checkService()).thenReturn(true);
        when(dependentServiceAvailabilityConnectedSystems.checkService()).thenReturn(true);
        when(kafkaListenerEndpointRegistry.getListenerContainer(any())).thenReturn(messageListenerContainer);
        when(messageListenerContainer.isAutoStartup()).thenReturn(false);
        when(messageListenerContainer.isRunning()).thenReturn(false);
        /*when(sftpService.setupSubsystemDetails()).thenReturn(true);*/
        doNothing().when(messageListenerContainer).start();
        postStartup.afterStartUp();
        sftpService.checkSubsystemDetails();
  //      verify(sftpService, times(2)).setupSubsystemDetails();
    }
}