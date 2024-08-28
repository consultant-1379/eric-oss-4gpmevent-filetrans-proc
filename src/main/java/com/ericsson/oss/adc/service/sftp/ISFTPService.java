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

import com.ericsson.oss.adc.util.ConsumerRecordRollbackException;
import com.ericsson.oss.adc.util.ConsumerRecordSkipException;

import java.io.File;
import java.util.Optional;

public interface ISFTPService {
    Optional<File> downloadFile(final String enmName, final String remoteFilePath, String localPath) throws ConsumerRecordRollbackException, ConsumerRecordSkipException;

    boolean checkConnection(String enmName);


}
