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
package com.ericsson.oss.adc.responses.deregister;

import com.ericsson.oss.adc.enums.DeregisterStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

class DeregisterResponseTest {

    @Test
    @DisplayName("Verify can create Success response")
    public void test_canBuildSuccessResponse() {

        ArrayList<String> successEnm = new ArrayList<>();
        ArrayList<String> failedEnm = new ArrayList<>();
        successEnm.add("enm1");
        successEnm.add("enm2");
        final String successResponse = "All data service instances are removed successfully";
        DeregisterResponse response = DeregisterResponse.build(successEnm, failedEnm);

        assertEquals(DeregisterStatus.SUCCESS, response.getDeregisterStatus());
        assertEquals(successResponse, response.getMessage());
    }

    @Test
    @DisplayName("Verify can create Failure response")
    public void test_canBuildFailureResponse() {

        ArrayList<String> successEnm = new ArrayList<>();
        successEnm.add("enm1");
        successEnm.add("enm2");
        final String failureResponse = "Data service instances are partially removed";
        DeregisterResponse response = DeregisterResponse.build(successEnm, successEnm);

        assertEquals(DeregisterStatus.FAILURE, response.getDeregisterStatus());
        assertEquals(failureResponse, response.getMessage());
    }

    @Test
    @DisplayName("Verify can create Exception response")
    public void test_canBuildDefaultResponse() {
        ArrayList<String> successEnm = new ArrayList<>();
        successEnm.add("enm1");
        successEnm.add("enm2");
        final String failureResponse = "Data service instances are partially removed";
        DeregisterResponse response = DeregisterResponse.build(successEnm, successEnm);

        assertEquals(DeregisterStatus.FAILURE, response.getDeregisterStatus());
        assertEquals(failureResponse, response.getMessage());
    }

    @Test
    @DisplayName("Verify if instance report is empty response")
    public void test_instanceReportEmptyResponse() {
        ArrayList<String> successEnm = new ArrayList<>();
        successEnm.add("enm1");
        successEnm.add("enm2");
        final String failureResponse = "Data service instances are partially removed";
        DeregisterResponse response = DeregisterResponse.build(successEnm, successEnm);

        assertEquals(DeregisterStatus.FAILURE, response.getDeregisterStatus());
        assertEquals(failureResponse, response.getMessage());
    }
}