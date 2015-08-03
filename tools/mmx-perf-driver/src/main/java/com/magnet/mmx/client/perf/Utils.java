/**
 * Copyright (c) 2014-2015 Magnet Systems, Inc.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.magnet.mmx.client.perf;

import java.nio.CharBuffer;
import java.util.Arrays;

public class Utils {
  public static CharSequence pad(CharSequence val, int size, char c) {
    if (val.length() == size)
      return val;
    CharBuffer cb = CharBuffer.allocate(size);
    int len = Math.min(size, val.length());
    cb.append(val, 0, len);
    if (len < size) {
      Arrays.fill(cb.array(), len, size, c);
    }
    return new String(cb.array());
  }
  
  public static CharSequence pad(int val, int size, char c) {
    return pad(String.valueOf(val), size, c);
  }
  
  public static CharSequence pad(long val, int size, char c) {
    return pad(String.valueOf(val), size, c);
  }
  
  public static CharSequence pad(CharSequence val, int size) {
    return pad(val, size, ' ');
  }
  
  public static CharSequence pad(int val, int size) {
    return pad(val, size, ' ');
  }
  
  public static CharSequence pad(long val, int size) {
    return pad(val, size, ' ');
  }
}
