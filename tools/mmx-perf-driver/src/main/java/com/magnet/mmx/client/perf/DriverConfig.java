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

import com.magnet.mmx.client.common.Log;

abstract class DriverConfig {
  public String appName = "PerfDrvApp";
  public String host = "54.183.139.43";
  public String userPrefix = "u-";
  public int logLevel = Log.DEBUG;
  public int numClients = 1;        // # clients plus one driver.
  public int minSize = 2000;        // in bytes
  public int maxSize = 200000;
  public long refreshTime = 15000;  // in msec
  public long minWaitTime = 250;    // in msec
  public long maxWaitTime = 500;    // in msec
  public long duration = 60 * 1000;        // in msec
  public boolean registerApp = false;

  public abstract int parseExtraOption(String[] args, int index);
  public abstract void reportExtraConfig(StringBuilder sb);
  public abstract void printExtraUsage();
  public abstract void printExtraHint();
  
  public StringBuilder reportConfig(StringBuilder sb) {
    sb.append(Utils.pad("Host:", 15)).append(host).append('\n')
      .append(Utils.pad("AppName:", 15)).append(appName).append('\n')
      .append(Utils.pad("# Clients:", 15)).append(numClients).append('\n')
      .append(Utils.pad("Size:", 15)).append(minSize).append(',').append(maxSize).append('\n')
      .append(Utils.pad("Wait:", 15)).append(minWaitTime).append(',').append(maxWaitTime).append("ms").append('\n')
      .append(Utils.pad("Duration:", 15)).append(duration/1000).append('s').append('\n');
    reportExtraConfig(sb);
    return sb;
  }
  
  public void printUsage() {
    System.out.println("[-h host] [-r] -n AppName [-l s|v|d|i|w|e]"+
        "[-c #clients] [-u userPrefix] [-s minSize] [-S maxSize] "+
        "[-w minWait] [-W maxWait] [-d duration] [-f refresh] [-?]");
    printExtraUsage();
    System.out.println("size has M|m|K|k, wait/duration/refresh has w|d|h|m|s|M");
    System.out.println("-t for receipt enable probability");
    printExtraHint();
  }
  
  public void parseOptions(String[] args) {
    for (int i = 0; i < args.length; i++) {
      String opt = args[i];
      if (opt.equals("-?")) {
        printUsage();
        System.exit(0);
      } else if (opt.equals("-h")) {
        String arg = args[++i];
        host = arg;
      } else if (opt.equals("-u")) {
      	String arg = args[++i];
      	userPrefix = arg;
      } else if (opt.equals("-f")) {
        String arg = args[++i];
        refreshTime = parseTime(arg);
      } else if (opt.equals("-r")) {
        registerApp = true;
      } else if (opt.equals("-n")) {
        String arg = args[++i];
        appName = arg;
      } else if (opt.equals("-c")) {
        String arg = args[++i];
        numClients = parseInt(arg);
      } else if (opt.equals("-s")) {
        String arg = args[++i];
        minSize = parseSize(arg);
      } else if (opt.equals("-S")) {
        String arg = args[++i];
        maxSize = parseSize(arg);
      } else if (opt.equals("-w")) {
        String arg = args[++i];
        minWaitTime = parseTime(arg);
      } else if (opt.equals("-W")) {
        String arg = args[++i];
        maxWaitTime = parseTime(arg);
      } else if (opt.equals("-d")) {
        String arg = args[++i];
        duration = parseTime(arg);
      } else if (opt.equals("-l")) {
        String arg = args[++i];
        logLevel = parseLogLevel(arg);
      } else {
        i = parseExtraOption(args, i);
      }
    }
  }

  public static int parseSize(String arg) {
    int unit = 1;
    char c = arg.charAt(arg.length()-1);
    switch(c) {
    case 'K':
      unit = 1024; break;
    case 'k':
      unit = 1000; break;
    case 'M':
      unit = 1024*1024; break;
    case 'm':
      unit = 1000*1000; break;
    default:
      if (c < '0' || c > '9') {
        throw new IllegalArgumentException("Invalid unit (M|m|K|k): "+arg);
      }
      return Integer.parseInt(arg);
    }
    return Integer.parseInt(arg.substring(0, arg.length()-1)) * unit;
  }

  public static long parseTime(String arg) {
    long unit = 1;
    char c = arg.charAt(arg.length()-1);
    switch(c) {
    case 'w':
      unit = 7 * 24 * 3600 * 1000; break;
    case 'd':
      unit = 24 * 3600 * 1000; break;
    case 'h':
      unit = 3600 * 1000; break;
    case 'm':
      unit = 60 * 1000; break;
    case 's':
      unit = 1000; break;
    case 'M':
      unit = 1; break;
    default:
      throw new IllegalArgumentException("Invalid unit (w|d|h|m|s|M): "+arg);
    }
    return Long.parseLong(arg.substring(0, arg.length()-1)) * unit;
  }
  
  public static int parseLogLevel(String arg) {
    if (arg.equalsIgnoreCase("s")) {
      return Log.SUPPRESS;
    } else if (arg.equalsIgnoreCase("v")) {
      return Log.VERBOSE;
    } else if (arg.equalsIgnoreCase("d")) {
      return Log.DEBUG;
    } else if (arg.equalsIgnoreCase("i")) {
      return Log.INFO;
    } else if (arg.equalsIgnoreCase("w")) {
      return Log.WARN;
    } else if (arg.equalsIgnoreCase("e")) {
      return Log.ERROR;
    }
    throw new IllegalArgumentException("Invalid log level (s|v|d|i|w|e): "+arg);
  }

  public static int parseInt(String arg) {
    return Integer.parseInt(arg);
  }
  
  public static boolean parseBool(String arg) {
    return Boolean.parseBoolean(arg);
  }
  
  public static String[] parseCommaList(String arg) {
    return arg.split(",");
  }
}