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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.List;

/**
 */
public class SqlUtil {
  public static String OP_BRACE = " ( ";
  public static String CL_BRACE = " ) ";

  public static String getQs(int num) {
    return Joiner.on(", ").join(Iterables.limit(Iterables.cycle("?"), num));
  }

  public static String getValueListUnboundedStr(int num) {
    return " VALUES "  + OP_BRACE + getQs(num) + CL_BRACE + " ";
  }

  public static String getCommaSepBraces(List<String> values) {
    return  " " + OP_BRACE + Joiner.on(",").join(values) + CL_BRACE + " ";
  }

  public static String getCommaSepBracesSingleQuoted(List<String> values) {
    Function<String, String> singleQuotedListTransform =
            new Function<String,String>() {
              public String apply(String input) { return "'" + input + "'"; }
            };
    List<String> newList = Lists.transform(values, singleQuotedListTransform);
    return  " " + OP_BRACE + Joiner.on(",").join(newList) + CL_BRACE + " ";
  }
}
