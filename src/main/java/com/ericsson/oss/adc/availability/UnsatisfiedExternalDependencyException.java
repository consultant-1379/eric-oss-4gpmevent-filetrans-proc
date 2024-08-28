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

/**
 * Exception for when an external dependency (i.e. a pod) cannot be reached.
 */
public class UnsatisfiedExternalDependencyException extends Exception {

    public UnsatisfiedExternalDependencyException(final String message) {
        super(message);
    }

    public UnsatisfiedExternalDependencyException(final String message, final Throwable e) {
        super(message, e);
    }
}
