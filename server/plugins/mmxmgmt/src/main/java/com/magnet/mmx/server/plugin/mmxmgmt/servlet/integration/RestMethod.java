package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;

import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by mmicevic on 4/6/16.
 *
 */

public abstract class RestMethod<REQ, RESP> {

    private Response.Status okStatus = Response.Status.OK;

//    public RestMethod(Response.Status okStatus) {x
//        this.okStatus = okStatus;
//    }
//
//    public RestMethod() {
//    }

    public Response doMethod(REQ request) {

        try {
            RESP response = execute(request);
//            return RestUtils.getCreatedJAXRSResp(response);
            return createResponseMessage(okStatus, response);
        }
        catch (MMXException e) {
            //TODO LOGGER
            e.printStackTrace();

            ErrorResponse errorResponse = new ErrorResponse(e.getCode(), e.getMessage());
            Response.Status errorStatus = Response.Status.BAD_REQUEST;
            if (ErrorCode.UNKNOWN_ERROR.getCode() == e.getCode()) {
                errorStatus = Response.Status.INTERNAL_SERVER_ERROR;
            } else if (ErrorCode.ILLEGAL_ARGUMENT.getCode() == e.getCode()) {
                errorStatus = Response.Status.BAD_REQUEST;
            } else if (ErrorCode.NOT_FOUND.getCode() == e.getCode()) {
                errorStatus = Response.Status.NOT_FOUND;
            }
            return createResponseMessage(errorStatus, errorResponse);
        }
        catch (Throwable t) {
            //TODO LOGGER
            t.printStackTrace();
//            LOGGER.error(sqlex.getMessage(), sqlex);
            ErrorResponse errorResponse = new ErrorResponse(ErrorCode.UNKNOWN_ERROR, t.getMessage());
            return createResponseMessage(Response.Status.INTERNAL_SERVER_ERROR, errorResponse);
        }
    }
    public abstract RESP execute(REQ request) throws MMXException;

    private static Response createResponseMessage(Response.Status status, Object entity) {
        return Response.status(status)
                .entity(entity)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    public static class SimpleMessage {

        private String message;

        public SimpleMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
        public void setMessage(String message) {
            this.message = message;
        }
    }
}