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
package com.magnet.mmx.server.plugin.mmxmgmt.web;

/**
 * Interface for validating message search values
 */
public interface MessageSearchValidator {

  /**
   * Validate the supplied values are appropriate for the search
   *
   * @param value1
   * @param value2
   * @return ValidatorResult which contains results of the validation
   */
  public ValidatorResult validate(String value1, String value2);


  /**
   * Interface that defines the validator result.
   */
  public interface ValidatorResult {

    /**
     * Are the values valid.
     *
     * @return true if they are false other wise
     */
    public boolean isValid();

    /**
     * Get a code related to why validation failed
     *
     * @return
     */
    public String getCode();

    /**
     * Get a message related to why validation failed.
     *
     * @return
     */
    public String getMessage();

  }
}
