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

package com.ericsson.oss.adc.availability;

import com.ericsson.oss.adc.config.CircuitBreakerConfig;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DependentServiceAvailabilityKafka extends DependentServiceAvailability {

    private static final Logger LOG = LoggerFactory.getLogger(DependentServiceAvailabilityKafka.class);

    @Value("${spring.kafka.topics.input.prefix}")
    private String inputTopicPrefix;

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Value("${spring.kafka.availability.retry-interval}")
    private int kafkaRetryInterval;

    @Value("${spring.kafka.availability.retry-attempts}")
    private int kafkaRetryAttempts;

    public DependentServiceAvailabilityKafka(){super(false);}

    protected AdminClient getAdminClient() {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }

    private Integer listTopicTimeout = 0;

   @Override
   boolean isServiceAvailable() throws UnsatisfiedExternalDependencyException {
       Map<String, Object> kafkaMap = kafkaAdmin.getConfigurationProperties();
       String bootstrapServerUrl = (String) kafkaMap.get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG);

       if (bootstrapServerUrl == null || bootstrapServerUrl.equals("")) {
           throw new UnsatisfiedExternalDependencyException("No bootstrap server found");
       } else {
           LOG.info("Checking if Input Topic exists, GET request on: '{}'", bootstrapServerUrl);
       }
       try (AdminClient client = getAdminClient()) {
           ListTopicsResult topics = listTopicTimeout == 0 ? client.listTopics() :
                   client.listTopics(new ListTopicsOptions().timeoutMs(listTopicTimeout));
           if (!(topics.names().get().isEmpty())) {
               LOG.info("Kafka is reachable : '{}'", bootstrapServerUrl);
               return true;
           }else{
               LOG.info("Kafka is not reachable : '{}'", bootstrapServerUrl);
               return false;
           }

       } catch (InterruptedException e) {
           LOG.error("Threading Interrupted Error Reaching Kafka: {}", e.getMessage());
           LOG.debug("Stack trace - Threading Interrupted Error Reaching Kafka ", e);
           Thread.currentThread().interrupt();
           throw new UnsatisfiedExternalDependencyException("Threading Interrupted Error", e);
       } catch (Exception e) {
           LOG.error("Error Reaching Kafka: {}", e.getMessage());
           LOG.debug("Stack trace - Error Reaching Kafka ", e);
           throw new UnsatisfiedExternalDependencyException("Kafka Unreachable", e);
       }
   }
    @Override
    CircuitBreakerConfig setCircuitBreakerConfig(){
        CircuitBreakerConfig circuitBreakerConfig=new CircuitBreakerConfig();
        circuitBreakerConfig.setRetryAttempts(kafkaRetryAttempts);
        circuitBreakerConfig.setRetryInterval(kafkaRetryInterval);
        return circuitBreakerConfig;
    }
}
