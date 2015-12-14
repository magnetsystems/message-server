package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;

import com.magnet.mmx.protocol.ChannelAction;
import com.magnet.mmx.server.api.v1.protocol.ChannelCreateInfo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;


public class QueryChannelRequest  {


    private List<String> subscribers;
    private String userId;
    private String mmxAppId;
    private String deviceId;

    private ChannelAction.ListType channelType;
    private QueryChannelRequest.MatchType matchFilter;

    public ChannelAction.ListType getChannelType() {
        return channelType;
    }

    public QueryChannelRequest setChannelType(ChannelAction.ListType channelType) {
        this.channelType = channelType;
        return this;
    }

    public MatchType getMatchFilter() {
        return matchFilter;
    }

    public QueryChannelRequest setMatchFilter(MatchType matchFilter) {
        this.matchFilter = matchFilter;
        return this;
    }


    public enum MatchType {
        exact_match,
        subset_match,
        any_match
    }

    public List<String> getSubscribers() {
        return subscribers;
    }

    public QueryChannelRequest setSubscribers(List<String> subscribers) {
        this.subscribers = subscribers;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public QueryChannelRequest setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getMmxAppId() {
        return mmxAppId;
    }

    public QueryChannelRequest setMmxAppId(String mmxAppId) {
        this.mmxAppId = mmxAppId;
        return this;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public QueryChannelRequest setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        return this;
    }


}
