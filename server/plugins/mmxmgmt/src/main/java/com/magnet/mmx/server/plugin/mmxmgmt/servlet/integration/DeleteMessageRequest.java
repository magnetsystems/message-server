package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;


import java.util.List;

public class DeleteMessageRequest {

    private String ownerId;
    private String channelId;
    private String userId;
    private String messageId;
    private String appId;
    private List<String> roles;

    /**
     * Get the owner ID of the private channel.
     * @return Owner ID of a private channel, or null for public channel.
     */
    public String getOwnerId() {
      return ownerId;
    }

    /**
     * Specify the owner ID of the private channel.
     * @param ownerId Owner ID of a private channel, or null for public channel.
     * @return
     */
    public DeleteMessageRequest setOwnerId(String ownerId) {
      this.ownerId = ownerId;
      return this;
    }

    public String getChannelId() {
      return channelId;
    }

    public DeleteMessageRequest setChannelId(String channelId) {
      this.channelId = channelId;
      return this;
    }

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
