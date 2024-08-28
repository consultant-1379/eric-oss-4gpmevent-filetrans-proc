/*******************************************************************************
 * COPYRIGHT Ericsson 2022
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

package com.ericsson.oss.adc.service.input.topic;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest(classes = {InputTopicService.class, SimpleMeterRegistry.class})
public class InputTopicServiceTest {

    @Autowired
    InputTopicService inputTopicService;

    @SpyBean
    SimpleMeterRegistry simpleMeterRegistry;

    @Test
    @DisplayName("Should successfully increment the input topic timer metric")
    public void test_recordTimer() {
        Long timetoProcess = 2354346L;
        int recordNumber = 400;
        Double endTimeTotal=2354346.0;

        inputTopicService.recordTimer(timetoProcess, recordNumber);


        assertEquals(2354346.0, endTimeTotal);

    }



}
