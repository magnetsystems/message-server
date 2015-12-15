package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;



import com.magnet.mmx.protocol.ChannelInfo;

import java.util.List;


public class QueryChannelResponse {

    private int code;
    private String message;
    private List<ChannelInfo> channels;

    public QueryChannelResponse(){
        this.code = 200;
    }

    public QueryChannelResponse(int code,String message){
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public QueryChannelResponse setCode(int code) {
        this.code = code;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public QueryChannelResponse setMessage(String message) {
        this.message = message;
        return this;
    }

    public List<ChannelInfo> getChannels() {
        return channels;
    }

    public QueryChannelResponse setChannels(List<ChannelInfo> channels) {
        this.channels = channels;
        return this;
    }
}
