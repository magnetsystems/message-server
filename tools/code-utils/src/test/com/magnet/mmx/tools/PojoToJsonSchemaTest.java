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
package com.magnet.mmx.tools;

import com.google.gson.annotations.SerializedName;
import com.magnet.mmx.protocol.DevReg;
import org.junit.Test;

public class PojoToJsonSchemaTest {

  public static class PojoTest {
    private String strField1;
    @SerializedName("field2")
    private String strField2;
  }

  @Test
  public void testCreateJsonForAll() {
    PojoToJsonSchema.printAll(this.getClass().getClassLoader());
  }

  @Test
  public void testGetJsonFormat() throws Exception {
    DevReg devReg;
    String result = PojoToJsonSchema.getJsonSchemaFormat(DevReg.class).toString();
    System.out.print(result);
  }

  @Test
  public void testGetJsonFormatWithJackson() throws Exception {
    System.out.print(PojoToJsonSchema.getJsonFormatWithJackson(DevReg.class));
  }

  @Test
  public void testGetJsonFormatWithJackson2() throws Exception {
    System.out.print(PojoToJsonSchema.getJsonFormatWithJackson(PojoTest.class));
  }
}