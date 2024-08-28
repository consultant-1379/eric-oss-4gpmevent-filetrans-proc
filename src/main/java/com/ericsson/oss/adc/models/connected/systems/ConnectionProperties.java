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
package com.ericsson.oss.adc.models.connected.systems;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class ConnectionProperties {
    private Long id;
    private Long subsystemId;
    private String name;
    private String tenant;
    @Getter
    private String username;
    @Getter
    private String password;
    @Getter
    private String scriptingVMs;
    @Getter
    private String sftpPort;
    private List<String> encryptedKeys;
    private List<SubsystemUsers> subsystemUsers;

    public void setId(Long id) {
        this.id = id;
    }

    public void setSubsystemId(final Long subsystemId) {
        this.subsystemId = subsystemId;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setTenant(final String tenant) {
        this.tenant = tenant;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public void setSftpPort(final String sftpPort) {
        this.sftpPort = sftpPort;
    }

    public void setEncryptedKeys(final List<String> encryptedKeys) {
        this.encryptedKeys = new ArrayList<>(encryptedKeys);
    }

    public void setSubsystemUsers(final List<SubsystemUsers> subsystemUsers) {
        this.subsystemUsers = new ArrayList<>(subsystemUsers);
    }

    public void setScriptingVMs(final String scriptingVMs) {
        this.scriptingVMs = scriptingVMs;
    }

}
