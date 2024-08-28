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

package com.ericsson.oss.adc.config;

import com.ericsson.oss.adc.config.kafka.BootStrapServerConfigurationSupplier;
import com.ericsson.oss.adc.models.InputMessage;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.transaction.KafkaAwareTransactionManager;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.MicrometerConsumerListener;
import org.springframework.kafka.core.MicrometerProducerListener;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;
import java.util.UUID;

import static com.ericsson.oss.adc.config.kafka.AdminConfiguration.ADMIN_CONFIG;
import static com.ericsson.oss.adc.config.kafka.ConsumerConfiguration.CONSUMER_CONFIG;
import static com.ericsson.oss.adc.config.kafka.ProducerConfiguration.PRODUCER_CONFIG;

@Configuration
public class KafkaConfiguration implements ApplicationContextAware {

    private static final String TX_PREFIX = UUID.randomUUID().toString();

    @Value("${spring.kafka.topics.input.concurrency}")
    private int concurrency;
    
    @Autowired
    private BootStrapServerConfigurationSupplier bootStrapServerConfigurationSupplier;

    private ApplicationContext applicationContext = null;

    private MeterRegistry meterRegistry;

    public KafkaConfiguration(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> adminConfig = applicationContext.getBean(ADMIN_CONFIG, Map.class);
        KafkaAdmin kafkaAdmin = new KafkaAdmin(adminConfig);
        kafkaAdmin.setBootstrapServersSupplier(bootStrapServerConfigurationSupplier);
        kafkaAdmin.setModifyTopicConfigs(true);
        return kafkaAdmin;
    }
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InputMessage> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, InputMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerInputFactory());
        factory.setConcurrency(concurrency);

        factory.getContainerProperties().setEosMode(ContainerProperties.EOSMode.V2);
        factory.getContainerProperties().setTransactionManager(transactionManager()); //set the consumer factory to use same trans manager as the producer factory
        factory.setBatchListener(true);

        final long retryIntervalMillis = 10000;
        DefaultErrorHandler defaultErrorHandler = new DefaultErrorHandler(new FixedBackOff(retryIntervalMillis, FixedBackOff.UNLIMITED_ATTEMPTS));
        factory.setCommonErrorHandler(defaultErrorHandler);

        return factory;
    }

    @Bean
    public ConsumerFactory<String, InputMessage> consumerInputFactory() {
        Map<String, Object> consumerConfig = applicationContext.getBean(CONSUMER_CONFIG, Map.class);

        DefaultKafkaConsumerFactory<String, InputMessage> factory = new DefaultKafkaConsumerFactory<>(consumerConfig);
        factory.addListener(new MicrometerConsumerListener<>(meterRegistry));
        factory.setBootstrapServersSupplier(bootStrapServerConfigurationSupplier);

        return factory;
    }


    @Bean
    public KafkaTemplate<String, byte[]> kafkaOutputTemplateWithBinaryFormat() {
        return new KafkaTemplate<>(producerOutputFactoryWithBinaryFormat(), false);
    }

    @Bean
    public ProducerFactory<String, byte[]> producerOutputFactoryWithBinaryFormat() {

        Map<String, Object> producerConfig = applicationContext.getBean(PRODUCER_CONFIG, Map.class);
        DefaultKafkaProducerFactory<String, byte[]> factory = new DefaultKafkaProducerFactory<>(producerConfig);
        factory.addListener(new MicrometerProducerListener<>(meterRegistry));
        factory.setTransactionIdPrefix(TX_PREFIX); //still have to give producer factory an id
        factory.setBootstrapServersSupplier(bootStrapServerConfigurationSupplier);

        return factory;
    }

    @Bean
    public KafkaAwareTransactionManager<String, byte[]> transactionManager() {
        KafkaTransactionManager<String, byte[]> transactionManager = new KafkaTransactionManager<>(producerOutputFactoryWithBinaryFormat()); // provide transaction manager with producer factory
        transactionManager.setTransactionIdPrefix(TX_PREFIX);
        return transactionManager;
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
