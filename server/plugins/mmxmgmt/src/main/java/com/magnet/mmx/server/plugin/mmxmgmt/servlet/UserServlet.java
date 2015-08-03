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

import com.magnet.mmx.server.plugin.mmxmgmt.db.*;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PostProcessor;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SearchValueValidator;
import com.magnet.mmx.server.plugin.mmxmgmt.search.UserEntityPostProcessor;
import com.magnet.mmx.server.plugin.mmxmgmt.search.user.UserSearchOption;
import com.magnet.mmx.server.plugin.mmxmgmt.search.user.UserSortOption;
import com.magnet.mmx.server.plugin.mmxmgmt.web.ValueHolder;
import com.magnet.mmx.util.GsonData;
import org.jivesoftware.admin.AuthCheckFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Servlet that handles queries for users
 */
public class UserServlet extends AbstractSecureServlet {
  private static final Logger LOGGER = LoggerFactory.getLogger(UserServlet.class);

  private static final String PATH = "mmxmgmt/users";
  private static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";

  private static final String KEY_SEARCH_BY = "searchby";
  private static final String KEY_APP_ID = "appId";
  private static final String KEY_SEARCH_VALUE = "value";
  private static final String KEY_SEARCH_VALUE2 = "value2";
  private static final String KEY_SORT_BY = "sortby";
  private static final String KEY_SORT_ORDER = "orderby";
  private static final String KEY_OFFSET = "offset";
  private static final String KEY_SIZE = "size";

  private static final String ERROR_INVALID_APPID = "Supplied application id is invalid.";
  private static final String ERROR_INVALID_SEARCH_VALUE = "Supplied search value is invalid.";

  @Override
  public void init(ServletConfig config) throws ServletException {
    LOGGER.info("Initializing:" + UserServlet.class);
    super.init(config);
    // Exclude this check so that the request won't be redirected to the login page.
    AuthCheckFilter.addExclude(PATH);
  }

  @Override
  public void destroy() {
    super.destroy();
    AuthCheckFilter.removeExclude(PATH);
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String appId = request.getParameter(KEY_APP_ID);
    String searchBy = request.getParameter(KEY_SEARCH_BY);
    String searchValue = request.getParameter(KEY_SEARCH_VALUE);
    String searchValue2 = request.getParameter(KEY_SEARCH_VALUE2);
    String sortBy = request.getParameter(KEY_SORT_BY);
    String sortOrder = request.getParameter(KEY_SORT_ORDER);
    String offset = request.getParameter(KEY_OFFSET);
    String size = request.getParameter(KEY_SIZE);

    response.setContentType(CONTENT_TYPE_JSON);
    PrintWriter out = response.getWriter();

    //validate stuff
    if (appId == null || appId.isEmpty()) {
      writeErrorResponse(out, MessageCode.INVALID_APPLICATION_ID.name(), ERROR_INVALID_APPID);
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    UserSearchOption searchOption = UserSearchOption.find(searchBy);
    ValueHolder vholder = new ValueHolder();
    vholder.setValue1(searchValue);
    vholder.setValue2(searchValue2);

    if (searchOption != null) {
      ErrorResponse vresult = validateSearchValue(searchOption, vholder);
      if (vresult != null) {
        writeErrorResponse(out, vresult);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
    }
    UserSortOption sortOptions = UserSortOption.build(sortBy, sortOrder).get(0);

    PaginationInfo pinfo = PaginationInfo.build(size, offset);
    UserDAO userDAO = new UserDAOImpl(new OpenFireDBConnectionProvider());
    UserSearchResult searchResult = userDAO.searchUsers(appId, searchOption, vholder, sortOptions, pinfo);
    /**
     * Apply the postProcessor
     */
    PostProcessor<UserEntity> postProcessor = new UserEntityPostProcessor();
    for (UserEntity me : searchResult.getResults()) {
      postProcessor.postProcess(me);
    }

    GsonData.getGson().toJson(searchResult, out);
    out.flush();
    response.setStatus(HttpServletResponse.SC_OK);
  }

  protected void writeErrorResponse(PrintWriter writer, String code, String message) {
    ErrorResponse error = new ErrorResponse();
    error.setCode(code);
    error.setMessage(message);
    GsonData.getGson().toJson(error, writer);
  }

  protected void writeErrorResponse(PrintWriter writer, ErrorResponse error) {
    GsonData.getGson().toJson(error, writer);
  }

  /**
   * Validate that the supplied values are correct for the chosen option.
   *
   * @param option
   * @param valueHolder
   * @return
   */
  private ErrorResponse validateSearchValue(UserSearchOption option, ValueHolder valueHolder) {
    SearchValueValidator validator = option.getValidator();
    SearchValueValidator.ValidatorResult result = validator.validate(valueHolder.getValue1(), valueHolder.getValue2());

    if (!result.isValid()) {
      return new ErrorResponse(result.getCode(), result.getMessage());
    }
    return null;
  }
}
