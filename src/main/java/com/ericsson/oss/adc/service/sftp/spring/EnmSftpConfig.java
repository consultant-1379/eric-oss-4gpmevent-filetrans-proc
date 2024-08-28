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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.file.remote.session.DefaultSessionFactoryLocator;
import org.springframework.integration.file.remote.session.DelegatingSessionFactory;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;

import java.util.HashMap;

@Configuration
public class EnmSftpConfig {
    @Bean
    public DefaultSessionFactoryLocator defaultSessionFactoryLocator() {
        return new DefaultSessionFactoryLocator(new HashMap<>());
    }

    @Bean
    public DelegatingSessionFactory delegatingSessionFactory(EnmSftpSessionFactoryLocator enmSftpSessionFactoryLocator) {
        return new DelegatingSessionFactory(enmSftpSessionFactoryLocator);
    }

    @Bean
    public SftpRemoteFileTemplate remoteFileTemplate(DelegatingSessionFactory delegatingSessionFactory) {
        return new SftpRemoteFileTemplate(delegatingSessionFactory);
    }
}
