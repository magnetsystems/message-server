package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;


import java.util.List;

public class DeleteMessageRequest {

    private String userId;
    private String messageId;
    private String appId;
    private List<String> roles;

    public String getUserId() {
        return userId;
    }

    public DeleteMessageRequest setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getMessageId() {
        return messageId;
    }

    public DeleteMessageRequest setMessageId(String messageId) {
        this.messageId = messageId;
        return this;
    }

    public String getAppId() {
        return appId;
    }

    public DeleteMessageRequest setAppId(String appId) {
        this.appId = appId;
        return this;
    }

    public List<String> getRoles() {
        return roles;
    }

    public DeleteMessageRequest setRoles(List<String> roles) {
        this.roles = roles;
        return this;
    }
}
