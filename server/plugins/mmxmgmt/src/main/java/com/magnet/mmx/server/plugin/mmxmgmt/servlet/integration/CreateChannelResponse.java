package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;


import com.magnet.mmx.protocol.ChannelAction;
import com.magnet.mmx.protocol.MMXStatus;

import java.util.HashMap;
import java.util.Map;

public class CreateChannelResponse {

    private int code;
    private String message;

    private Map<String,ChannelAction.SubscribeResponse> subscribeResponse =
                new HashMap<String,ChannelAction.SubscribeResponse>();

    public CreateChannelResponse(){
        this.code = 200;
    }
    public CreateChannelResponse(int code,String message){
        this.code = code;
        this.message = message;
    }
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, ChannelAction.SubscribeResponse> getSubscribeResponse() {
        return subscribeResponse;
    }

    public void setSubscribeResponse(Map<String, ChannelAction.SubscribeResponse> subscribeResponse) {
        this.subscribeResponse = subscribeResponse;
    }


}
