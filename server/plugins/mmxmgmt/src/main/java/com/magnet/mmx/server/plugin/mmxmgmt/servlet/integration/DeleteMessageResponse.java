package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;

public class DeleteMessageResponse {


    private int code;
    private String message;

    public DeleteMessageResponse(){
        this.code = 200;
    }
    public DeleteMessageResponse(int code,String message){
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


}
