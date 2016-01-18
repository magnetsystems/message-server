package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;

import com.magnet.mmx.server.api.v1.protocol.ChannelCreateInfo;

import java.util.Map;


public class PublishMessageToChatRequest extends ChannelCreateInfo {

    private String userId;
    private String mmxAppId;
    private String deviceId;

    private Map<String, String> content;
    private String messageType;
    private String contentType;
    private String recipients[];


    public String getMmxAppId() {
        return mmxAppId;
    }

    public void setMmxAppId(String mmxAppId) {
        this.mmxAppId = mmxAppId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String[] getRecipients() {
        return recipients;
    }

    public void setRecipients(String[] recipients) {
        this.recipients = recipients;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Map<String, String> getContent() {
        return content;
    }

    public void setContent(Map<String, String> content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
}
