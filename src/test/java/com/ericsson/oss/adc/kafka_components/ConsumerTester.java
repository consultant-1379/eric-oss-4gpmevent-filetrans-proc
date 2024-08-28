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

import com.ericsson.oss.adc.models.DecodedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;



/**
 * Kafka consumer for testing purposes
 */
@Component
public class ConsumerTester {

    private static final String TOPIC =  "4g-pm-event-file-transfer-and-processing";

    private CountDownLatch latch = new CountDownLatch(1);
    private DecodedEvent eventData = null;
    private int recordCount = 0;

    @KafkaListener(containerFactory="consumerKafkaListenerOutputTestContainerFactory", topics = TOPIC)
    public void receive( @Payload Object message,
                         @Header("event_id") String eventId){




        recordCount++;
        latch.countDown();
    }

    /**
     * Sets countdown latch
     * @param count
     */
    public void setCountDownLatch(int count) {
        latch = new CountDownLatch(count);
    }

    /**
     * Returns countdown latch
     * @return
     */
    public CountDownLatch getLatch() {
        return latch;
    }

    /**
     * Returns count for number of records that have been consumed
     * @return
     */
    public int getRecordCount() {
        return recordCount;
    }

    /**
     * Returns event data consumed
     * @return
     */
    public DecodedEvent getEventData() {
        return eventData;
    }

    /**
     * Resets consumer variables
     */
    public void reset(){
        latch = new CountDownLatch(1);
        eventData = null;
        recordCount = 0;
    }
}
