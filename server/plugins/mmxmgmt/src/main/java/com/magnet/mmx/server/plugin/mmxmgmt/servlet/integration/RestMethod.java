package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;

import com.magnet.mmx.server.api.v1.RestUtils;
import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;

import javax.ws.rs.core.Response;

/**
 * Created by mmicevic on 4/6/16.
 *
 */

public abstract class RestMethod<REQ, RESP> {

    public Response doMethod(REQ request) {

        try {
            RESP response = execute(request);
            return RestUtils.getCreatedJAXRSResp(response);
        }
        catch (MMXException e) {
            e.printStackTrace();
            ErrorResponse errorResponse = new ErrorResponse(e.getCode(), e.getMessage());
            return RestUtils.getBadReqJAXRSResp(errorResponse);
        }
        catch (Throwable t) {
            t.printStackTrace();
//            LOGGER.error(sqlex.getMessage(), sqlex);
            ErrorResponse errorResponse = new ErrorResponse(ErrorCode.UNKNOWN_ERROR, "");
            return RestUtils.getBadReqJAXRSResp(errorResponse);
        }
    }
    public abstract RESP execute(REQ request) throws MMXException;
}
