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

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.DevReg;
import com.magnet.mmx.protocol.OSType;
import freemarker.template.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.jivesoftware.smack.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Message.Type;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

public class GenTestScript {

  private static final Logger log = Logger.getLogger(GenTestScript.class.getName());

  private static final String LOGIN_TEMPLATE_FILE = "loadtest_login.ftl";
  private static final String MMX_STANZA_TEMPLATE_FILE = "loadtest_send_with_loops.ftl";
  private static final String DEVREG_TEMPLATE_FILE = "loadtest_devreg.ftl";

  private static Settings genSettings;

  public static class Settings {
    String userName;
    String appId;
    int maxCount;
    String templateDir;
    String templateName;
    String outputDir;
    String servername;
    String hostname;
    String port;
    public String apiKey;

    public Settings(){}
  }
  public static void generateScripts(Settings settings) throws TemplateException {
    genSettings = settings;
    File file = new File(settings.outputDir + "/userdb.csv");
    FileWriter fileWriter = null;
    try {
      fileWriter = new FileWriter(file);
      // hard-code to "test" for all passwords
      String password = "test";
      for (int i = 1; i <= settings.maxCount; i++) {
        String jid=settings.userName + i + "%" + settings.appId;
        int random = new Random().nextInt(settings.maxCount); // make sure it's at least 1
        if (random == 0) random++;
        String toJid=settings.userName + random + "%" + settings.appId;
        // jid;password;toJid
        StringBuilder rowBuilder = new StringBuilder();
        rowBuilder.append(jid).append(";")
            .append(password).append(";")
            .append(toJid)
            .append("\n");
        fileWriter.write(rowBuilder.toString());
      }
      fileWriter.close();

      // generate the login load test file
      generateLoginScript(settings);

      // generate the dev registration files
      generateDevRegScript(settings);

      // generate the message send files
      generateSendMessageScript(settings);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void generateDevRegScript(Settings settings) throws IOException, TemplateException {

    String xmlMessage = StringEscapeUtils.escapeXml(generateDevRegMessageStanza());

    Configuration cfg = new Configuration();

    cfg.setDirectoryForTemplateLoading(new File(settings.templateDir));
    cfg.setObjectWrapper(new DefaultObjectWrapper());

    cfg.setDefaultEncoding("UTF-8");
    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);


    Map root = new HashMap();
    root.put("mmxHostname", settings.hostname);
    root.put("mmxPort", settings.port);
    root.put("devreg_msg", xmlMessage);
    Template temp = cfg.getTemplate(DEVREG_TEMPLATE_FILE);

    File file = new File(settings.outputDir + "/loadtest_devreg.xml");
    Writer out = new OutputStreamWriter(new FileOutputStream(file));
    temp.process(root, out);
    out.close();
  }

  public static void generateSendMessageScript(Settings settings) throws IOException, TemplateException {

    String xmlMessage = StringEscapeUtils.escapeXml(generateSendMessageStanza());

    Configuration cfg = new Configuration();

    cfg.setDirectoryForTemplateLoading(new File(settings.templateDir));
    cfg.setObjectWrapper(new DefaultObjectWrapper());

    cfg.setDefaultEncoding("UTF-8");
    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);


    Map root = new HashMap();
    root.put("mmxHostname", settings.hostname);
    root.put("mmxPort", settings.port);
    root.put("mmx_stanza", xmlMessage);
    Template temp = cfg.getTemplate(MMX_STANZA_TEMPLATE_FILE);

    File file = new File(settings.outputDir + "/loadtest_send_message.xml");
    Writer out = new OutputStreamWriter(new FileOutputStream(file));
    temp.process(root, out);
    out.close();
  }

  public static void generateScript(String templateDir, String templateName, String serverhost, String port, String username, String appKey, String numusers) throws IOException, TemplateException {

    String xmlMessage = StringEscapeUtils.escapeXml(generateDevRegMessageStanza());

    Configuration cfg = new Configuration();

    cfg.setDirectoryForTemplateLoading(new File(templateDir));
    cfg.setObjectWrapper(new DefaultObjectWrapper());

    cfg.setDefaultEncoding("UTF-8");
    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);


    Map root = new HashMap();
    root.put("mmxHostname", serverhost);
    root.put("mmxPort", port);
    root.put("numUsers", numusers);
    root.put("username", username);
    //root.put("password", password);
    root.put("mmx_stanza", xmlMessage);
    Template temp = cfg.getTemplate(templateName);

    Writer out = new OutputStreamWriter(System.out);
    temp.process(root, out);
  }

  public static void generateLoginScript(Settings settings) throws IOException, TemplateException {

    String xmlMessage = StringEscapeUtils.escapeXml(generateDevRegMessageStanza());

    Configuration cfg = new Configuration();

    cfg.setDirectoryForTemplateLoading(new File(settings.templateDir));
    cfg.setObjectWrapper(new DefaultObjectWrapper());

    cfg.setDefaultEncoding("UTF-8");
    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);


    Map root = new HashMap();
    root.put("mmxHostname", settings.hostname);
    root.put("mmxPort", settings.port);
    Template temp = cfg.getTemplate(LOGIN_TEMPLATE_FILE);

    File file = new File(settings.outputDir + "/loadtest_login.xml");
    Writer out = new OutputStreamWriter(new FileOutputStream(file));
    temp.process(root, out);
    out.close();
  }
  static String generateDevRegMessageStanza() {
    DevReg reg = new DevReg();
    reg.setDevId("%%ts_user_server:get_unique_id%%");   // let tsung generate a unique id
    //reg.setDevId(devid);
    reg.setOsType(OSType.ANDROID.name());
    reg.setDisplayName("Loadtester");
    reg.setOsVersion("4.4");
    reg.setApiKey(genSettings.apiKey);
    //send an IQ request with device registration
    DocumentFactory factory = new DocumentFactory();
    final Element element = factory.createElement(Constants.MMX_DEV_REG, Constants.MMX_NS_DEV);
    element.addAttribute(Constants.MMX_ATTR_COMMAND, Constants.DeviceCommand.REGISTER.name());
    element.setText(reg.toJson());
    IQ devRegIq = new IQ() {
      @Override
      public CharSequence getChildElementXML() {
        return element.asXML();
      }
    };
    devRegIq.setType(IQ.Type.SET);
    devRegIq.setFrom("%%_username%%");
    return devRegIq.toXML().toString();
  }
  static String generateSendMessageStanza() {
    String fromJid = "%%_username%%@"+genSettings.servername+"/tsung";
    String toJid = "%%_tojid%%@"+genSettings.servername+"/tsung";
    Message message = new Message();
    message.setType(Type.chat);
    message.getElement().addAttribute("from", fromJid);
    message.getElement().addAttribute("to", toJid);
    message.setID("%%ts_user_server:get_unique_id%%");

    // build up the MMX message packet extension
    Element element = message.addChildElement(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD);
    element.addElement(Constants.MMX_META);
    Element payload = element.addElement(Constants.MMX_PAYLOAD);
    payload.addAttribute(Constants.MMX_ATTR_CTYPE, "plain/text");
    payload.addAttribute(Constants.MMX_ATTR_MTYPE, "string");
    String randomText = "Considered an invitation do introduced sufficient understood instrument it. Of decisively friendship in as collecting at. No affixed be husband ye females brother garrets proceed. Least child who seven happy yet balls young. Discovery sweetness principle discourse shameless bed one excellent. Sentiments of surrounded friendship dispatched connection is he. Me or produce besides hastily up as pleased. Bore less when had and john shed hope. \n" +
        "\n" +
        "Barton waited twenty always repair in within we do. An delighted offending curiosity my is dashwoods at. Boy prosperous increasing surrounded companions her nor advantages sufficient put. John on time down give meet help as of. Him waiting and correct believe now cottage she another. Vexed six shy yet along learn maids her tiled. Through studied shyness evening bed him winding present. Become excuse hardly on my thirty it wanted. \n" +
        "\n" +
        "Six reached suppose our whether. Oh really by an manner sister so. One sportsman tolerably him extensive put she immediate. He abroad of cannot looked in. Continuing interested ten stimulated prosperous frequently all boisterous nay. Of oh really he extent horses wicket. \n" +
        "\n" +
        "Placing assured be if removed it besides on. Far shed each high read are men over day. Afraid we praise lively he suffer family estate is. Ample order up in of in ready. Timed blind had now those ought set often which. Or snug dull he show more true wish. No at many deny away miss evil. On in so indeed spirit an mother. Amounted old strictly but marianne admitted. People former is remove remain as. \n" +
        "\n" +
        "Little afraid its eat looked now. Very ye lady girl them good me make. It hardly cousin me always. An shortly village is raising we shewing replied. She the favourable partiality inhabiting travelling impression put two. His six are entreaties instrument acceptance unsatiable her. Amongst as or on herself chapter entered carried no. Sold old ten are quit lose deal his sent. You correct how sex several far distant believe journey parties. We shyness enquire uncivil affixed it carried to. \n" +
        "\n" +
        "Parish so enable innate in formed missed. Hand two was eat busy fail. Stand smart grave would in so. Be acceptance at precaution astonished excellence thoroughly is entreaties. Who decisively attachment has dispatched. Fruit defer in party me built under first. Forbade him but savings sending ham general. So play do in near park that pain. \n" +
        "\n" +
        "Needed feebly dining oh talked wisdom oppose at. Applauded use attempted strangers now are middleton concluded had. It is tried \uFEFFno added purse shall no on truth. Pleased anxious or as in by viewing forbade minutes prevent. Too leave had those get being led weeks blind. Had men rose from down lady able. Its son him ferrars proceed six parlors. Her say projection age announcing decisively men. Few gay sir those green men timed downs widow chief. Prevailed remainder may propriety can and. \n" +
        "\n" +
        "Seen you eyes son show. Far two unaffected one alteration apartments celebrated but middletons interested. Described deficient applauded consisted my me do. Passed edward two talent effect seemed engage six. On ye great do child sorry lived. Proceed cottage far letters ashamed get clothes day. Stairs regret at if matter to. On as needed almost at basket remain. By improved sensible servants children striking in surprise. \n" +
        "\n" +
        "Rendered her for put improved concerns his. Ladies bed wisdom theirs mrs men months set. Everything so dispatched as it increasing pianoforte. Hearing now saw perhaps minutes herself his. Of instantly excellent therefore difficult he northward. Joy green but least marry rapid quiet but. Way devonshire introduced expression saw travelling affronting. Her and effects affixed pretend account ten natural. Need eat week even yet that. Incommode delighted he resolving sportsmen do in listening. \n" +
        "\n" +
        "On recommend tolerably my belonging or am. Mutual has cannot beauty indeed now sussex merely you. It possible no husbands jennings ye offended packages pleasant he. Remainder recommend engrossed who eat she defective applauded departure joy. Get dissimilar not introduced day her apartments. Fully as taste he mr do smile abode every. Luckily offered article led lasting country minutes nor old. Happen people things oh is oppose up parish effect. Law handsome old outweigh humoured far appetite. \n" +
        "\n";
    payload.addText(StringEscapeUtils.escapeXml(randomText));
    return message.toXML().toString();
  }

}

