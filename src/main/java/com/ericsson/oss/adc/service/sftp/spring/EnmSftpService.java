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
import com.ericsson.oss.adc.service.sftp.ISFTPService;
import com.ericsson.oss.adc.util.ConsumerRecordRollbackException;
import com.ericsson.oss.adc.util.ConsumerRecordSkipException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.common.SftpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.file.remote.session.DelegatingSessionFactory;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Optional;

@Service
@Slf4j
public class EnmSftpService implements ISFTPService {

    @Autowired
    DelegatingSessionFactory delegatingSessionFactory;

    @Autowired
    SftpRemoteFileTemplate remoteFileTemplate;

    @Autowired
    private EventFileDownloadConfiguration evtFileDownConfig;

    @Autowired
    private EnmSftpSessionFactoryLocator enmSftpSessionFactoryLocator;

    @Override
    public Optional<File> downloadFile(String enmName, String remoteFilePath, String localFilePath) throws ConsumerRecordRollbackException, ConsumerRecordSkipException {
        for (int i = 1; i <= evtFileDownConfig.getNumberOfEventFileDownloadRetries(); i++) {
            try {
                File localFile = new File(localFilePath);
                downloadFileWithoutRetry(enmName, remoteFilePath, localFilePath);
                return Optional.of(localFile);
            } catch (Exception ex) {
                if (ex.getCause() instanceof SftpException sftpEx && sftpEx.getStatus() == SftpConstants.SSH_FX_NO_SUCH_FILE) {
                    log.error("Unable to download the file, no file found and event file will be skipped for {}", remoteFilePath);
                    throw new ConsumerRecordSkipException("File not found skipping the process of this " + remoteFilePath, ex);
                }

                log.error("file download failed retrying {}/{} file {}", i, evtFileDownConfig.getNumberOfEventFileDownloadRetries(), remoteFilePath, ex);
                if (i == evtFileDownConfig.getNumberOfEventFileDownloadRetries()) {
                    enmSftpSessionFactoryLocator.refreshSessionFactoryForEnm(enmName);
                    throw new ConsumerRecordRollbackException("download failed", ex);
                }
            }
        }
        return Optional.empty();
    }

    private void downloadFileWithoutRetry(String enmName, String remoteFilePath, String localFilePath) {
        delegatingSessionFactory.setThreadKey(enmName);
        File localFile = new File(localFilePath);
        try {
            remoteFileTemplate.get(remoteFilePath, sftpStream -> FileUtils.copyInputStreamToFile(sftpStream, localFile));
        } finally {
            delegatingSessionFactory.clearThreadKey();
        }
    }

    public boolean checkConnection(String enmName) {
        delegatingSessionFactory.setThreadKey(enmName);
        try (Session session = remoteFileTemplate.getSessionFactory().getSession()) {
            return session.isOpen();
        } finally {
            delegatingSessionFactory.clearThreadKey();
        }
    }
}
