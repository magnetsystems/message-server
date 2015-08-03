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

package com.magnet.mmx.tsung;

import freemarker.template.TemplateException;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GenTestScriptCli {

  private static final Logger log = Logger.getLogger(GenTestScriptCli.class.getName());

  private static final String SERVER_OPTION_SHORT = "s";
  private static final String SERVER_OPTION_LONG = "server";
  private static final String HOST_OPTION_LONG = "host";
  private static final String HOST_OPTION_SHORT = "h";
  private static final String PORT_OPTION_SHORT = "p";
  private static final String PORT_OPTION_LONG = "port";
  private static final String USERNAME_OPTION_SHORT = "u";
  private static final String USERNAME_OPTION_LONG = "username";
  private static final String APPID_OPTION_SHORT = "a";
  private static final String APPID_OPTION_LONG = "appid";
  private static final String APIKEY_OPTION_SHORT = "k";
  private static final String APIKEY_OPTION_LONG = "apikey";

  private static final String COUNT_OPTION_SHORT = "c";
  private static final String COUNT_OPTION_LONG = "count";
  private static final String TEMPLATE_DIR_OPTION_SHORT = "d";
  private static final String TEMPLATE_DIR_OPTION_LONG = "dir";
  private static final String TEMPLATE_NAME_OPTION_SHORT = "f";
  private static final String TEMPLATE_NAME_OPTION_LONG = "file";

  private static final String HELP_OPTION_SHORT = "help";
  private static final String HELP_OPTION_LONG = "help";

  private String[] args = null;

  private Options options = new Options();

  public static void main(String[] args) throws IOException, TemplateException {

    new GenTestScriptCli(args).parse();

  }

  public GenTestScriptCli(String[] args) {

    this.args = args;
    options.addOption(HELP_OPTION_SHORT, HELP_OPTION_LONG, false, "show this help.");
    options.addOption(HOST_OPTION_SHORT, HOST_OPTION_LONG, true,
        "MMX server hostname");
    options.addOption(SERVER_OPTION_SHORT, SERVER_OPTION_LONG, true,
        "MMX XMPP server name");
    options.addOption(PORT_OPTION_SHORT, PORT_OPTION_LONG, true,
        "MMX server port number");
    options.addOption(USERNAME_OPTION_SHORT, USERNAME_OPTION_LONG, true,
        "MMX client user name");
    options.addOption(APPID_OPTION_SHORT, APPID_OPTION_LONG, true,
        "MMX client appid");
    options.addOption(APIKEY_OPTION_SHORT, APIKEY_OPTION_LONG, true,
        "MMX client apikey");
    options.addOption(COUNT_OPTION_SHORT, COUNT_OPTION_LONG, true,
        "Number of users for load simulation");
    options.addOption(TEMPLATE_DIR_OPTION_SHORT, TEMPLATE_DIR_OPTION_LONG, true,
        "Directory of templates");
    options.addOption(TEMPLATE_NAME_OPTION_SHORT, TEMPLATE_NAME_OPTION_LONG, true,
        "Name of template file");
  }

  public void parse() throws IOException, TemplateException {
    CommandLineParser parser = new BasicParser();
    CommandLine cmd = null;
    String servername = null, hostname = null, port = null, username = null, numusers = null;
    String templateDir = null, templateName = null, appKey =  null, apiKey = null;

    try {
      cmd = parser.parse(options, args);
      if (cmd.hasOption(HELP_OPTION_SHORT)) {
        help();
      }

      if (cmd.hasOption(SERVER_OPTION_SHORT)) {
        servername = cmd.getOptionValue(SERVER_OPTION_SHORT);
      }
      if (cmd.hasOption(HOST_OPTION_SHORT)) {
        hostname = cmd.getOptionValue(HOST_OPTION_SHORT);
      }
      if (cmd.hasOption(PORT_OPTION_SHORT)) {
        port = cmd.getOptionValue(PORT_OPTION_SHORT);
      }
      if (cmd.hasOption(USERNAME_OPTION_SHORT)) {
        username = cmd.getOptionValue(USERNAME_OPTION_SHORT);
      }
      if (cmd.hasOption(APPID_OPTION_SHORT)) {
        appKey = cmd.getOptionValue(APPID_OPTION_SHORT);
      }
      if (cmd.hasOption(APIKEY_OPTION_SHORT)) {
        apiKey = cmd.getOptionValue(APIKEY_OPTION_SHORT);
      }

      if (cmd.hasOption(COUNT_OPTION_SHORT)) {
        numusers = cmd.getOptionValue(COUNT_OPTION_SHORT);
      }
      if (cmd.hasOption(TEMPLATE_DIR_OPTION_SHORT)) {
        templateDir = cmd.getOptionValue(TEMPLATE_DIR_OPTION_SHORT);
      }
      if (cmd.hasOption(TEMPLATE_NAME_OPTION_SHORT)) {
        templateName = cmd.getOptionValue(TEMPLATE_NAME_OPTION_SHORT);
      }
      if (servername == null || port == null || username == null || numusers == null) {
        log.log(Level.SEVERE, "Missing options");
        help();
      }
      GenTestScript.Settings settings = new GenTestScript.Settings();
      settings.userName = username;
      settings.appId = appKey;
      settings.apiKey = apiKey;
      settings.maxCount = Integer.valueOf(numusers);
      settings.outputDir = "output";
      settings.port = port;
      settings.servername = servername;
      settings.hostname = hostname;
      settings.templateDir = templateDir;
      settings.templateName = templateName;

      GenTestScript.generateScripts(settings);

//      GenTestScript.generateScript(templateDir, templateName, servername, port, username, appId, numusers);
//      GenTestScript.generateUserCsv(username, appId, Integer.valueOf(numusers), "output");

    } catch (ParseException e) {
      log.log(Level.SEVERE, "Failed to parse command line properties", e);
      help();
    }
  }

  private void help() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Main [-options]", options);
    System.exit(0);
  }

}
