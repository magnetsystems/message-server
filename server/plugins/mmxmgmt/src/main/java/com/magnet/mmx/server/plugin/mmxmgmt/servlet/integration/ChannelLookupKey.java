package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;




public class ChannelLookupKey {

    private String channelName;

    //Set to true to make channel private, false to make public.
    //Default to false
    private boolean privateChannel;
    private String userId;


    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;

    }
    public boolean isPrivateChannel() {
        return privateChannel;
    }

    public void setPrivateChannel(boolean privateChannel) {
        this.privateChannel = privateChannel;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}