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
package com.magnet.mmx.server.plugin.mmxmgmt.util;

import com.magnet.mmx.server.plugin.mmxmgmt.db.*;

/**
 */
public class DBUtil {
  public static DeviceDAO getDeviceDAO() {
    return new DeviceDAOImpl(new OpenFireDBConnectionProvider());
  }

  public static AppDAO getAppDAO() {
    return new AppDAOImpl(new OpenFireDBConnectionProvider());
  }

  public static MessageDAO getMessageDAO() {
    return new MessageDAOImpl(new OpenFireDBConnectionProvider());
  }

  public static PushMessageDAO getPushMessageDAO() {
    return new PushMessageDAOImpl(new OpenFireDBConnectionProvider());
  }

  public static WakeupEntityDAO getWakeupEntityDAO() {
    return new WakeupEntityDAOImpl(new OpenFireDBConnectionProvider());
  }

  public static TagDAO getTagDAO() {
    return new TagDAOImpl(new OpenFireDBConnectionProvider());
  }

  public static UserDAO getUserDAO() {
    return new UserDAOImpl(new OpenFireDBConnectionProvider());
  }

  public static TopicDAO getTopicDAO() { return new TopicDAOImpl(new OpenFireDBConnectionProvider()); }

  public static TopicItemDAO getTopicItemDAO() {return new TopicItemDAOImpl(new OpenFireDBConnectionProvider()); }
}
