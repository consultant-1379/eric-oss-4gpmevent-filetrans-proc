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

package com.ericsson.oss.adc.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.CircuitBreakerRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
public class RetryUtil {
    private static final Logger LOG = LoggerFactory.getLogger(RetryUtil.class);

    /**
     * Simpled retry template that use application variables set at deploy time for the number of
     * retry attempt and interval in mS.
     *
     * @returns false if all retries are exhausted
     */
    public RetryTemplate retryTemplate(int retryAttempts, int retryInterval) {

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(retryAttempts);

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(retryInterval);

        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(retryPolicy);
        template.setBackOffPolicy(backOffPolicy);

        return template;
    }

    /**
     * A simple generic {@link RetryListener} that logs the number of retry attempts.
     *
     * @return A @{@link RetryListener} that can be used in a {@link org.springframework.retry.support.RetryTemplate}.
     */
    public static RetryListener getRetryListener() {
        return new RetryListener() {
            @Override
            public <T, E extends Throwable> boolean open(final RetryContext retryContext, final RetryCallback<T, E> retryCallback) {
                return true; // called before first retry
            }

            @Override
            public <T, E extends Throwable> void close(final RetryContext retryContext, final RetryCallback<T, E> retryCallback, final Throwable throwable) {
                LOG.error("Retry closed after {} attempts", retryContext.getRetryCount(), throwable);
            }

            @Override
            public <T, E extends Throwable> void onError(final RetryContext retryContext, final RetryCallback<T, E> retryCallback, final Throwable throwable) {
                LOG.error("Retry Triggered {}", retryContext.getRetryCount(), throwable);
            }
        };
    }

    /**
     * For Max attempts, try to execute request within open timeout. Repeat on interval until successful.
     *
     * @param circuitBreakerRetryAttempts Retry attempts before opening the circuit.
     * @param circuitBreakerResetTimeOut  After circuit opens, it will re-close after this time, need to re-close circuit before next attempt to
     *                                    reset the circuit.
     * @param circuitBreakerOpenTimeout   If delegate policy cannot retry and this timeout has not elapsed, the circuit is opened, and we exit the
     *                                    retry.
     * @param circuitBreakerBackoff       Back off for this amount of time before retrying between attempts.
     * @return RetryTemplate with the desired configuration.
     */
    public static RetryTemplate buildCircuitBreakerRetry(final int circuitBreakerRetryAttempts,
                                                         final long circuitBreakerResetTimeOut,
                                                         final long circuitBreakerOpenTimeout,
                                                         final long circuitBreakerBackoff) {
        RetryTemplate retryTemplate = new RetryTemplate();
        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
        // Max number of times to retry before opening the circuit and exiting retry
        // Note: If circuit closed after tripping, this count is reset (desired scenario)
        simpleRetryPolicy.setMaxAttempts(circuitBreakerRetryAttempts);

        CircuitBreakerRetryPolicy circuitBreakerRetryPolicy = new CircuitBreakerRetryPolicy(simpleRetryPolicy);

        // After circuit opens, it will re-close after this time, need to re-close circuit before next attempt to reset the circuit (retry count)
        circuitBreakerRetryPolicy.setResetTimeout(circuitBreakerResetTimeOut);

        // if delegate policy cannot retry and this timeout has not elapsed.
        // The circuit is opened, and we exit the retry (undesirable scenario for startup)
        circuitBreakerRetryPolicy.setOpenTimeout(circuitBreakerOpenTimeout);

        retryTemplate.setRetryPolicy(circuitBreakerRetryPolicy);

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        // Back off for this amount of time before retrying between attempts
        fixedBackOffPolicy.setBackOffPeriod(circuitBreakerBackoff);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy); // number of seconds to wait between retries

        retryTemplate.registerListener(new RetryListener() {
            @Override
            public <T, E extends Throwable> boolean open(final RetryContext retryContext, final RetryCallback<T, E> retryCallback) {
                LOG.error("Circuit Breaker open with retry count {}, retry status {}", retryContext.getRetryCount(),
                        circuitBreakerRetryPolicy.canRetry(retryContext));

                return circuitBreakerRetryPolicy.canRetry(retryContext);
            }

            @Override
            public <T, E extends Throwable> void close(final RetryContext retryContext, final RetryCallback<T, E> retryCallback,
                                                       final Throwable throwable) {
                LOG.error("Circuit Breaker exit after {} attempts, retry status {}", retryContext.getRetryCount(),
                        circuitBreakerRetryPolicy.canRetry(retryContext), throwable);
            }

            @Override
            public <T, E extends Throwable> void onError(final RetryContext retryContext, final RetryCallback<T, E> retryCallback,
                                                         final Throwable throwable) {
                LOG.error("Circuit Breaker Retry Triggered {}, retry status {}", retryContext.getRetryCount(), circuitBreakerRetryPolicy.canRetry(retryContext),
                        throwable);
            }
        });

        return retryTemplate;
    }


}
