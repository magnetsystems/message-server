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
package com.magnet.mmx.server.plugin.mmxmgmt.db;


/**
 * Simple subclass to wrap all database related exceptions.
 */
public class DbInteractionException extends RuntimeException {

  public DbInteractionException() {
  }

  public DbInteractionException(String message) {
    super(message);
  }

  public DbInteractionException(String message, Throwable cause) {
    super(message, cause);
  }

  public DbInteractionException(Throwable cause) {
    super(cause);
  }

  public DbInteractionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
