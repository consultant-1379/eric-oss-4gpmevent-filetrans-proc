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

package com.ericsson.oss.adc.service.sftp;

import com.ericsson.oss.adc.availability.UnsatisfiedExternalDependencyException;
import com.ericsson.oss.adc.models.InputMessage;
import com.ericsson.oss.adc.models.connected.systems.Subsystem;
import com.ericsson.oss.adc.service.connected.systems.ConnectedSystemsService;
import com.ericsson.oss.adc.util.ConsumerRecordRollbackException;
import com.ericsson.oss.adc.util.FileTransferMetricsUtil;
import com.ericsson.oss.adc.util.ConsumerRecordSkipException;
import com.ericsson.oss.adc.util.RetryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static com.ericsson.oss.adc.util.FileTransferMetricsUtil.NUM_SUCCESSFULL_FILES_TRANSFERRED;
import static com.ericsson.oss.adc.util.FileTransferMetricsUtil.TRANSFERRED_FILE_DATA_VOLUME;

import static com.ericsson.oss.adc.util.FileTransferMetricsUtil.NUM_FAILED_FILES_TRANSFERRED;

import static com.ericsson.oss.adc.util.FileTransferMetricsUtil.NUM_FILES_DELETED_DUETO_CONNECTION_EXCEPTION;




@Component
public class SFTPService {

    private static final Logger LOG = LoggerFactory.getLogger(SFTPService.class);

    private static final String UNIX_SEPARATOR = "/";

    @Autowired
    private RetryUtil retryUtil;

    @Value("${temp-directory}")
    private String tempDirectory;

    @Value("${scriptingVM.availability.retry-interval}")
    private int retryInterval;

    @Value("${scriptingVM.availability.retry-attempts}")
    private int retryAttempts;

    @Autowired
    private ConnectedSystemsService connectedSystemsService;

    @Autowired
    ISFTPService enmSftpService;

    @Autowired
    private FileTransferMetricsUtil metricsUtil;



    /**
     * download the file and sets downloaLocation
     * @param inputMessage
     * @param enmName
     * @return
     * @throws IOException
     */
    public InputMessage downloadSftpFile(final InputMessage inputMessage, String enmName) throws ConsumerRecordRollbackException, ConsumerRecordSkipException {
        return download(inputMessage, enmName);
    }


    /**
     * download the file and sets downloaLocation
     * @param inputMessage
     * @param enmName
     * @return
     * @throws IOException
     */
    private InputMessage download(final InputMessage inputMessage, final String enmName) throws ConsumerRecordRollbackException, ConsumerRecordSkipException {
        try {
            final String remoteFilePath = inputMessage.getFileLocation();
            final String localFilePath = tempDirectory + UNIX_SEPARATOR + new File(remoteFilePath).getName();
            LOG.info("Downloading '{}' to '{}'", remoteFilePath, localFilePath);

            Optional<File> optionalFile = enmSftpService.downloadFile(enmName, remoteFilePath, localFilePath);

            if (optionalFile.isPresent() && optionalFile.get().exists()) {
                File downloadedFile = optionalFile.get();
                inputMessage.setDownloadedFile(downloadedFile);
                metricsUtil.incrementCounterByName(NUM_SUCCESSFULL_FILES_TRANSFERRED);
                metricsUtil.addToGaugeByName(TRANSFERRED_FILE_DATA_VOLUME,downloadedFile.length());
            } else {
                LOG.error("File download failed for file {}", inputMessage.getFileLocation());
                metricsUtil.incrementCounterByName(NUM_FAILED_FILES_TRANSFERRED);
            }
        } catch(ConsumerRecordRollbackException | ConsumerRecordSkipException ex) {
            LOG.error("Exception while downloading the file {}", inputMessage.getFileLocation());
            if (inputMessage.getDownloadedFile() != null && inputMessage.getDownloadedFile().exists()) {
                deleteFile(inputMessage);
            }
            throw ex;
        } catch (Exception exception) {
            if (inputMessage.getDownloadedFile() != null && inputMessage.getDownloadedFile().exists()) {
                deleteFile(inputMessage);
            }
            LOG.error("Exception while downloading the file {}", inputMessage.getFileLocation(), exception);
            throw new ConsumerRecordRollbackException(exception.getMessage(), exception);
        }
        return inputMessage;
    }

    protected void deleteFile(final InputMessage inputMessageWithFileToDelete) {
        final String remoteFilePath = inputMessageWithFileToDelete.getFileLocation();
        final String localFilePath = tempDirectory + UNIX_SEPARATOR + new File(remoteFilePath).getName();
        try {
            Path filePath = new File(localFilePath).toPath();
            LOG.info("Deleting file {}", filePath);
            Files.delete(filePath);
            metricsUtil.incrementCounterByName(NUM_FILES_DELETED_DUETO_CONNECTION_EXCEPTION);

        } catch (Exception e) {
            LOG.error("Error Attempting to delete file: {}", e.getMessage());
        }
    }

    /**
     * This method will check subsystem Details are available or not
     * based on system availability it will return boolean response.
     *
     * @return the boolean
     */
    public boolean checkSubsystemDetails() throws UnsatisfiedExternalDependencyException {
        LOG.info("Requested Subsystems from Connected Systems");
        return retryUtil.retryTemplate(retryAttempts, retryInterval).execute(context -> this.isSubsystemDetailsAvailable());
    }

    private boolean isSubsystemDetailsAvailable() throws UnsatisfiedExternalDependencyException {
        final boolean isSubsystemsDetailsAvailable;
        Map<String, Subsystem> subsystemsDetailsMap = connectedSystemsService.getSubsystemDetails();
        if(subsystemsDetailsMap == null || subsystemsDetailsMap.isEmpty()) {
            throw new UnsatisfiedExternalDependencyException("Connected Systems details not found ");
        } else {
            isSubsystemsDetailsAvailable = true;
        }
        return isSubsystemsDetailsAvailable;
    }

    public Map<String, Subsystem> getSubsystemDetails() {
        return connectedSystemsService.getSubsystemDetails();
    }

    /**
     * This method gets the substem details and checks if the connection can be made to all of them
     * based on the connectivity it will return boolean response
     * @return
     */
    public boolean checkSubsystemsConnections() {
        Map<String, Subsystem> subsystemsDetailsMap = connectedSystemsService.getSubsystemDetails();
        boolean atleastOneUp = false;
        if (subsystemsDetailsMap == null || subsystemsDetailsMap.isEmpty()) {
            LOG.info("subsystemsDetailsMap are empty");
            return false;
        }

        for (String enm : subsystemsDetailsMap.keySet()) {
            if (checkSubsystemsConnection(enm)) {
                LOG.info("successful sftp connection made to enm: {} ", enm);
                atleastOneUp = true;
            }
        }
        return atleastOneUp;
    }

    public boolean checkSubsystemsConnection(String enm) {
        try {
            LOG.info("checking sftp connection to enm: {}", enm);
            return enmSftpService.checkConnection(enm);
        } catch (Exception ex) {
            LOG.error("connection to sftp failed enm: {}", enm, ex);
        }
        return false;
    }

}
