package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;

import com.magnet.mmx.server.plugin.mmxmgmt.api.query.DateRange;

import java.util.List;
import java.util.Map;

public class ChannelSummaryRequest {

    private String deviceId;

    //private List<String> channelIds;
    private List<ChannelLookupKey> channelIds;

    private int numOfSubcribers;

    private int numOfMessages;

    private long messagesSince;

    private boolean includeOwnerInfo;

    private String requestingUserId;

    private String appId;

    public boolean isIncludeOwnerInfo() {
        return includeOwnerInfo;
    }

    public void setIncludeOwnerInfo(boolean includeOwnerInfo) {
        this.includeOwnerInfo = includeOwnerInfo;
    }

    public int getNumOfMessages() {
        return numOfMessages;
    }

    public void setNumOfMessages(int numOfMessages) {
        this.numOfMessages = numOfMessages;
    }

    public int getNumOfSubcribers() {
        return numOfSubcribers;
    }

    public void setNumOfSubcribers(int numOfSubcribers) {
        this.numOfSubcribers = numOfSubcribers;
    }

    public String getRequestingUserId() {
        return requestingUserId;
    }

    public void setRequestingUserId(String requestingUserId) {
        this.requestingUserId = requestingUserId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public long getMessagesSince() {
        return messagesSince;
    }

    public void setMessagesSince(long messagesSince) {
        this.messagesSince = messagesSince;
    }

    public List<ChannelLookupKey> getChannelIds() {
        return channelIds;
    }

    public ChannelSummaryRequest setChannelIds(List<ChannelLookupKey> channelIds) {
        this.channelIds = channelIds;
        return this;
    }
}
