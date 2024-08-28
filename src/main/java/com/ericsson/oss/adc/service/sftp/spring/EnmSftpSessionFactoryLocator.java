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
package com.ericsson.oss.adc.service.sftp.spring;

import com.ericsson.oss.adc.config.EventFileDownloadConfiguration;
import com.ericsson.oss.adc.models.connected.systems.ConnectionProperties;
import com.ericsson.oss.adc.models.connected.systems.Subsystem;
import com.ericsson.oss.adc.service.connected.systems.ConnectedSystemsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.DefaultSessionFactoryLocator;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.remote.session.SessionFactoryLocator;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class EnmSftpSessionFactoryLocator implements SessionFactoryLocator {
    private final Lock lock = new ReentrantLock();

    @Autowired
    private ConnectedSystemsService connectedSystemsService;

    @Autowired
    private EventFileDownloadConfiguration eventFileDownloadConfiguration;

    @Autowired
    private DefaultSessionFactoryLocator locator;

    @Value("${spring.kafka.topics.input.concurrency}")
    private int concurrency;

    @Value("${sftp.use.shared.sftp-connection}")
    private boolean useSharedSftpConnection;


    @Override
    public SessionFactory getSessionFactory(Object enmName) {
        SessionFactory enmSessionFactory = locator.getSessionFactory(enmName);
        if (enmSessionFactory == null) {
            refreshSessionFactoryForEnm(enmName.toString());
            enmSessionFactory = locator.getSessionFactory(enmName);
        }
        return enmSessionFactory;
    }

    public void refreshSessionFactoryForEnm(String enmName) {
        lock.lock();
        try {
            SessionFactory enmSessionFactory = locator.getSessionFactory(enmName);
            if (enmSessionFactory != null && enmSessionFactory instanceof CachingSessionFactory<?> cachingSessionFactory) {
                locator.removeSessionFactory(enmName);
                cachingSessionFactory.resetCache();
            }
            enmSessionFactory = generateSessionFactoryForEnm(enmName);
            locator.addSessionFactory(enmName, enmSessionFactory);
        } finally {
            lock.unlock();
        }
    }

    private SessionFactory generateSessionFactoryForEnm(String enmName) {
        final Map<String, Subsystem> subsystemDetailsMap = connectedSystemsService.getSubsystemDetails();
        Subsystem enmSubsystem = subsystemDetailsMap.get(enmName);
        if (enmSubsystem == null) {
            log.error("enmSubsystem is null");
            return null;
        }
        ConnectionProperties enmConnectionProperties = enmSubsystem.getConnectionProperties().get(0);
        final String scriptingVMs = enmConnectionProperties.getScriptingVMs();
        if (scriptingVMs == null) {
            log.error("enmSubsystem scriptingVMs is null");
            return null;
        }

        DefaultSftpSessionFactory defaultSessionFactory = new DefaultSftpSessionFactory(useSharedSftpConnection);
        //scripting vm should contain only one public ip; if it's multiple a different one is picked up if the connectivity fails
        if (scriptingVMs.contains(",")) {
            int maxIndex = scriptingVMs.split(",").length;
            int rand = new Random().nextInt(0, maxIndex);
            log.warn("enmSubsystem.scriptingVMs contains multiple picking the first item");
            defaultSessionFactory.setHost(scriptingVMs.split(",")[rand].trim());
        } else {
            defaultSessionFactory.setHost(scriptingVMs.trim());
        }
        defaultSessionFactory.setPort(Integer.valueOf(enmConnectionProperties.getSftpPort()));
        defaultSessionFactory.setUser(enmConnectionProperties.getUsername());
        defaultSessionFactory.setPassword(enmConnectionProperties.getPassword());
        defaultSessionFactory.setAllowUnknownKeys(true);
        defaultSessionFactory.setTimeout(eventFileDownloadConfiguration.getSftpConnectionTimeoutMs());

        CachingSessionFactory cachingSessionFactory = new CachingSessionFactory(defaultSessionFactory);
        cachingSessionFactory.setPoolSize(concurrency + 1);  //one extra for periodic sftp connection check
        cachingSessionFactory.setSessionWaitTimeout(eventFileDownloadConfiguration.getSftpSessionTimeoutMs());
        cachingSessionFactory.resetCache();

        return cachingSessionFactory;
    }
}

