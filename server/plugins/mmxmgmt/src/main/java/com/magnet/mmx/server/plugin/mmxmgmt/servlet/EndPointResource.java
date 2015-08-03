/*   Copyright (c) 2015 Magnet Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.magnet.mmx.server.plugin.mmxmgmt.servlet;

import com.magnet.mmx.server.plugin.mmxmgmt.db.EndPointDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.EndPointDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.EndPointEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.SearchResult;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SearchValueValidator;
import com.magnet.mmx.server.plugin.mmxmgmt.search.endpoint.EndPointSearchOption;
import com.magnet.mmx.server.plugin.mmxmgmt.search.endpoint.EndPointSortOption;
import com.magnet.mmx.server.plugin.mmxmgmt.web.ValueHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Resource that deals with endpoints. Uses the Jersey framework for handling the requests.
 */
@Path("/endpoints")
public class EndPointResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(EndPointResource.class);

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{appid}/search")
  public SearchResult<EndPointEntity> search(@PathParam("appid") String appId,
                                             @QueryParam("searchby") String searchBy,
                                             @QueryParam("value") String searchValue,
                                             @QueryParam("value1") String searchValue2,
                                             @QueryParam("sortby") String sortBy,
                                             @QueryParam("sortorder") String sortOrder,
                                             @QueryParam("size") Integer size,
                                             @QueryParam("offset") Integer offset) {

    try {
      EndPointSearchOption searchOption = EndPointSearchOption.find(searchBy);
      ValueHolder vholder = new ValueHolder();
      vholder.setValue1(searchValue);
      vholder.setValue2(searchValue2);

      if (searchOption != null) {
        ErrorResponse validationResult = validateSearchValue(searchOption, vholder);
        if (validationResult != null) {
          LOGGER.info("Request validation failed:" +  validationResult);
          throw new WebApplicationException(
              Response
                  .status(Response.Status.BAD_REQUEST)
                  .entity(validationResult)
                  .build()
          );
        }
      }
      EndPointSortOption sortOptions = EndPointSortOption.build(sortBy, sortOrder).get(0);
      PaginationInfo pinfo = PaginationInfo.build(size, offset);

      EndPointDAO endPointDAO = new EndPointDAOImpl(new OpenFireDBConnectionProvider());
      SearchResult<EndPointEntity> result = endPointDAO.search(appId, searchOption, vholder, sortOptions, pinfo);
      for (EndPointEntity ee : result.getResults()) {
        String ownerId = ee.getDevice().getOwnerId();
        if (ownerId.indexOf('\\') >= 0) {
          ee.getDevice().setOwnerId(JID.unescapeNode(ownerId));
        }
      }
      return result;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Throwable t) {
      LOGGER.warn("Throwable during processing request", t);
      ErrorResponse error = new ErrorResponse();
      error.setCode(WebConstants.STATUS_ERROR);
      error.setMessage(t.getMessage());
      throw new WebApplicationException(
          Response
              .status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity(error)
              .build()
      );
    }
  }

  /**
   * Validate that the supplied values are correct for the chosen option.
   *
   * @param option
   * @param valueHolder
   * @return
   */
  private ErrorResponse validateSearchValue(EndPointSearchOption option, ValueHolder valueHolder) {
    SearchValueValidator validator = option.getValidator();
    SearchValueValidator.ValidatorResult result = validator.validate(valueHolder.getValue1(), valueHolder.getValue2());
    if (!result.isValid()) {
      return new ErrorResponse(result.getCode(), result.getMessage());
    }
    return null;
  }


}




