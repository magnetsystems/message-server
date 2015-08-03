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
package com.magnet.mmx.server.api.v1;

/**
 */

import com.magnet.mmx.server.api.v1.protocol.DeviceInfo;
import com.magnet.mmx.server.api.v1.protocol.DeviceSearchResult;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.api.AbstractBaseResource;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorMessages;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.DateRange;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.DeviceQuery;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceNotFoundException;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceQueryBuilder;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceStatus;
import com.magnet.mmx.server.plugin.mmxmgmt.db.QueryBuilderResult;
import com.magnet.mmx.server.plugin.mmxmgmt.db.SearchResult;
import com.magnet.mmx.server.plugin.mmxmgmt.db.TagDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.TagEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.push.ResolutionException;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SortInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SortOrder;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.Helper;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Path("/devices")
@Produces({MediaType.APPLICATION_JSON})
public class DevicesResource extends AbstractBaseResource {
  private static Logger LOGGER = LoggerFactory.getLogger(DevicesResource.class);

  @Context
  private HttpServletRequest servletRequest;

  static final String DEVICEID_PATH_PARAM_KEY = "deviceId";
  static final String REGISTERED_SINCE_PARAM_KEY = "registered_since";
  static final String REGISTERED_UNTIL_PARAM_KEY = "registered_until";
  static final String DEVICE_STATUS_PARAM_KEY = "status";
  static final String OS_TYPE_PARAM_KEY = "os_type";
  static final String TAG_TYPE_PARAM = "tag";

  @GET
  @Path("{" + DEVICEID_PATH_PARAM_KEY + "}")
  public Response getDeviceById(@PathParam(DEVICEID_PATH_PARAM_KEY) String deviceId) {
    final String methodName = "getDeviceById";
    try {
      long startTime = System.nanoTime();
      AppEntity appEntity;

      Object o = servletRequest.getAttribute(MMXServerConstants.MMX_APP_ENTITY_PROPERTY);

      if(o instanceof AppEntity) {
        appEntity = (AppEntity) o;
        LOGGER.debug("getDeviceById : retrieiving appEntity from servletRequestContext entity={}", appEntity);
      } else {
        LOGGER.error("getDeviceById : appEntity is not set");
        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .build();
      }
      Response response;
      if (deviceId == null || deviceId.isEmpty()) {
        ErrorResponse badDeviceId = new ErrorResponse(ErrorCode.INVALID_DEVICE_ID, ErrorMessages.ERROR_INVALID_DEVICE_ID);
        response = Response
            .status(Response.Status.BAD_REQUEST)
            .entity(badDeviceId)
            .build();
      } else {
        String appId = appEntity.getAppId();
        DeviceDAO deviceDAO = DBUtil.getDeviceDAO();
        DeviceEntity deviceEntity = null;
        try {
          deviceEntity = deviceDAO.getDevice(appId, deviceId);
        } catch (DeviceNotFoundException e) {
        }
        if (deviceEntity == null) {
          LOGGER.info("{}: Device not found for appId:{} and deviceId:{}", methodName, appId, deviceId);
          ErrorResponse badDeviceId = new ErrorResponse(ErrorCode.INVALID_DEVICE_ID, ErrorMessages.ERROR_DEVICE_NOT_FOUND);
          response = Response
              .status(Response.Status.NOT_FOUND)
              .entity(badDeviceId)
              .build();
        } else {
          DeviceInfo devInfo = DeviceEntity.toDeviceInfo(deviceEntity);
          //get tags also for the device
          TagDAO tagDAO = DBUtil.getTagDAO();
          List<TagEntity> tagList = tagDAO.getTagEntitiesForDevice(deviceEntity);
          List<String> tags = new ArrayList<String>(tagList.size());
          for (TagEntity te : tagList) {
            tags.add(te.getTagname());
          }
          devInfo.setTags(tags);
          response = Response
              .status(Response.Status.OK)
              .entity(devInfo)
              .build();
        }
      }
      long endTime = System.nanoTime();
      LOGGER.info("{}: Completed processing in {} milliseconds", methodName,
          TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS));
      return response;

    } catch (Throwable t) {
      LOGGER.warn("Throwable during send message", t);
      throw new WebApplicationException(
          Response
              .status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity(new ErrorResponse(ErrorCode.ISE_DEVICES_GET_BY_ID, t.getMessage()))
              .build()
      );
    }
  }

  @GET
  public Response searchDevices(@QueryParam(OS_TYPE_PARAM_KEY) String osType,
                                @QueryParam(REGISTERED_SINCE_PARAM_KEY) String registeredSince,
                                @QueryParam(REGISTERED_UNTIL_PARAM_KEY)  String registeredUntil,
                                @QueryParam(TAG_TYPE_PARAM) List<String> tags,
                                @QueryParam(DEVICE_STATUS_PARAM_KEY) String status,
                                @QueryParam(MMXServerConstants.SORT_BY_PARAM) String sortByInput,
                                @QueryParam(MMXServerConstants.SORT_ORDER_PARAM) String sortOrderInput,
                                @QueryParam(MMXServerConstants.SIZE_PARAM) Integer size,
                                @QueryParam(MMXServerConstants.OFFSET_PARAM) Integer offset) {
    final String methodName = "searchDevices";

    try {
      long startTime = System.nanoTime();
      AppEntity appEntity;

      Object o = servletRequest.getAttribute(MMXServerConstants.MMX_APP_ENTITY_PROPERTY);

      if(o instanceof AppEntity) {
        appEntity = (AppEntity) o;
        LOGGER.debug("searchDevices : retrieiving appEntity from servletRequestContext entity={}", appEntity);
      } else {
        LOGGER.error("searchDevices : appEntity is not set");
        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .build();
      }
      Response response;
      DeviceQuery query = new DeviceQuery();
      query.setOsType(Helper.enumerateOSType(osType));
      query.setTags(tags);
      DeviceStatus deviceStatus = validateDeviceStatus(status);
      query.setStatus(deviceStatus);

      Date regSince = parseRegisteredSince(registeredSince);
      Date regUntil = parseRegisteredUntil(registeredUntil);
      DateRange registeredRange = new DateRange();
      if (regSince != null) {
        registeredRange.setStart(Integer.valueOf((int)(regSince.getTime()/1000L)));
      }
      if (regUntil != null) {
        registeredRange.setEnd(Integer.valueOf((int) (regUntil.getTime() / 1000L)));
      }

      if (DateRange.isValid(registeredRange)) {
        query.setRegistrationDate(registeredRange);
      }

      DeviceQueryBuilder.DeviceSortBy sortBy = validateSortBy(sortByInput);

      if (sortBy == null) {
        sortBy = DeviceQueryBuilder.DeviceSortBy.REGISTRATION_DATE;
      }

      SortOrder sortOrder = validateSortOrder(sortOrderInput);

      if (sortOrder == null) {
        sortOrder = SortOrder.ASCENDING;
      }
      SortInfo sortInfo = SortInfo.build(sortBy.name(), sortOrder.name());
      PaginationInfo paginationInfo = PaginationInfo.build(size, offset);
      DeviceQueryBuilder queryBuilder = new DeviceQueryBuilder(false, true);
      QueryBuilderResult builtQuery = queryBuilder.buildQuery(query, appEntity.getAppId(), paginationInfo, sortInfo);
      SearchResult<DeviceEntity> deviceEntitySearchResult = DBUtil.getDeviceDAO().searchDevices(builtQuery, paginationInfo);
      DeviceSearchResult searchResult = new DeviceSearchResult();
      List<DeviceEntity> deviceEntities = deviceEntitySearchResult.getResults();
      List<DeviceInfo> deviceInfoList = new ArrayList<DeviceInfo>(deviceEntities.size());
      for (DeviceEntity de : deviceEntities) {
        deviceInfoList.add(DeviceEntity.toDeviceInfo(de));
      }
      searchResult.setResults(deviceInfoList);
      searchResult.setSize(deviceEntitySearchResult.getSize());
      searchResult.setOffset(deviceEntitySearchResult.getOffset());
      searchResult.setTotal(deviceEntitySearchResult.getTotal());
      response = Response
          .status(Response.Status.OK)
          .entity(searchResult)
          .build();

      long endTime = System.nanoTime();
      LOGGER.info("{}: Completed processing in {} milliseconds", methodName,
          TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS));
      return response;
    } catch (ValidationException e) {
      LOGGER.warn("ValidationException", e);
      throw new WebApplicationException(
          Response
              .status(Response.Status.BAD_REQUEST)
              .entity(e.getError())
              .build()
      );
    } catch (ResolutionException e) {
      LOGGER.warn("ResolutionException", e);
      throw new WebApplicationException(
          Response
              .status(Response.Status.BAD_REQUEST)
              .entity(new ErrorResponse(ErrorCode.INVALID_DEVICE_SEARCH_CRITERIA, ErrorMessages.ERROR_INVALID_DEVICE_SEARCH_CRITERIA))
              .build()
      );
    } catch (Throwable t) {
      LOGGER.warn("Throwable during send message", t);
      throw new WebApplicationException(
          Response
              .status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity(new ErrorResponse(ErrorCode.ISE_DEVICES_SEARCH, t.getMessage()))
              .build()
      );
    }
  }


  private DeviceQueryBuilder.DeviceSortBy validateSortBy(String input) throws ValidationException {
    if (input == null || input.isEmpty()) {
      return null;
    }
    DeviceQueryBuilder.DeviceSortBy sortBy = DeviceQueryBuilder.DeviceSortBy.find(input);
    if (sortBy == null) {
      String message = String.format(ErrorMessages.ERROR_INVALID_SORT_BY_VALUE, input);
      LOGGER.warn(message);
      throw new ValidationException(new ErrorResponse(ErrorCode.INVALID_SORT_BY_VALUE, message));
    }
    return sortBy;
  }

  private SortOrder validateSortOrder(String input) throws ValidationException {
    if (input == null || input.isEmpty()) {
      return null;
    }
    SortOrder sortOrder = SortOrder.from(input);
    if (sortOrder == null) {
      String message = String.format(ErrorMessages.ERROR_INVALID_SORT_ORDER_VALUE, input);
      LOGGER.info(message);
      throw new ValidationException(new ErrorResponse(ErrorCode.INVALID_SORT_ORDER_VALUE, message));
    }
    return sortOrder;
  }

  private DeviceStatus validateDeviceStatus(String input) throws ValidationException {
    if (input == null || input.isEmpty()) {
      return null;
    }
    DeviceStatus deviceStatus = DeviceStatus.find(input);
    if (deviceStatus == null) {
      String message = String.format(ErrorMessages.ERROR_INVALID_DEVICE_STATUS_VALUE, input);
      LOGGER.info(message);
      throw new ValidationException(new ErrorResponse(ErrorCode.INVALID_DEVICE_STATUS_VALUE, message));
    }
    return deviceStatus;
  }


  private Date parseRegisteredSince (String input) throws ValidationException {
    if (input == null || input.isEmpty()) {
      return null;
    }
    Date dateTime = null;
    try {
      dateTime = Utils.buildISO8601DateFormat().parse(input);
    } catch (ParseException e) {
      String message = String.format(ErrorMessages.ERROR_INVALID_REGISTERED_SINCE_VALUE, input);
      LOGGER.info(message);
      throw new ValidationException(new ErrorResponse(ErrorCode.INVALID_DEVICE_REGISTERED_SINCE_VALUE, message));
    }
    return dateTime;
  }

  private Date parseRegisteredUntil (String input) throws ValidationException {
    if (input == null || input.isEmpty()) {
      return null;
    }
    Date parsedDate = null;
    try {
      parsedDate = Utils.buildISO8601DateFormat().parse(input);
    } catch (ParseException e) {
      String message = String.format(ErrorMessages.ERROR_INVALID_REGISTERED_UNTIL_VALUE, input);
      LOGGER.info(message);
      throw new ValidationException(new ErrorResponse(ErrorCode.INVALID_DEVICE_REGISTERED_UNTIL_VALUE, message));
    }
    return parsedDate;
  }


}
