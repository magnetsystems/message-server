/*   Copyright (c) 2016 Magnet Systems, Inc.
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
package com.magnet.mmx.util;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.magnet.mmx.util.Utils;

/**
 * Test for template macro expansion.
 */
public class TemplateUtilTest {

  public static class User {
    public String firstName;
    public String lastName;
    public boolean isRegistered;
    public int age;
    public String[] questions;
    public Map<String, String> props;
  }

  public static class NameDesc {
    public String name;
    public String desc;

    public NameDesc(String name, String description) {
      this.name = name;
      this.desc = description;
    }
  }

  public static class MessageData {
    public final Map<String, String> content;
    public final String from;
    public final Date date;

    public MessageData(Map<String, String> content, String pubName, Date pubDate) {
      this.content = content;
      this.from = pubName;
      this.date = pubDate;
    }
  }

  @Test
  public void test01EvalVar() {
    User user = new User();
    user.firstName = "First";
    user.lastName = "Last";
    user.isRegistered = true;
    user.age = 35;
    user.questions = new String[] { "question 0", "question 1", "question 2" };
    user.props = new HashMap<String, String>() {{
      this.put("prop1", "PropertyValue1");
      this.put("prop2", "PropertyValue2");
      this.put("prop3", "PropertyValue3");
    }};
    Map<String, Object> maps = new HashMap<String, Object>();
    maps.put("var1", "simple text");
    maps.put("user", user);

    String val = (String) Utils.expandVar(maps, "var1", null);
    assertEquals("simple text", val);
    String firstName = (String) Utils.expandVar(maps, "user.firstName", null);
    assertEquals(user.firstName, firstName);
    String lastName = (String) Utils.expandVar(maps, "user.lastName", null);
    assertEquals(user.lastName, lastName);
    Integer age = (Integer) Utils.expandVar(maps, "user.age", null);
    assertEquals(user.age, age.intValue());
    Boolean bval = (Boolean) Utils.expandVar(maps, "user.isRegistered", null);
    assertEquals(user.isRegistered, bval.booleanValue());
    String question1 = (String) Utils.expandVar(maps, "user.questions[1]", null);
    assertEquals(user.questions[1], question1);
    String prop1 = (String) Utils.expandVar(maps, "user.props[\"prop1\"]", null);
    assertEquals(user.props.get("prop1"), prop1);
    String unknown = (String) Utils.expandVar(maps, "user.props[\"unknown\"]", "");
    assertEquals("", unknown);
    String bogus = (String) Utils.expandVar(maps,  "user.bogus[\"name\"]", "");
    assertEquals("", bogus);
  }

  @Test
  public void test02Eval() {
    Map<String, Object> maps = new HashMap<String, Object>() {{
      Map<String, String> content = new HashMap<String, String>() {{
        this.put("content-type", "text/plain");
        this.put("msg-type", "greetings");
        this.put("text", "Good morning Vietnam");
      }};

      this.put("application", new NameDesc("Quickstart", "Quick Start"));
      this.put("channel", new NameDesc("KOIT", "FM 96.5"));
      this.put("msg", new MessageData(content, "Adrian Cronauer", new Date(1456864262705L)));
    }};

    String template = "${application.name}: ${channel.name} - ${msg.date}\n"+
        "New message from ${msg.from}: ${msg.content[\"text\"]}";
    CharSequence output = Utils.eval(template, maps);

//    System.out.println(output);
    assertEquals("Quickstart: KOIT - Tue Mar 01 12:31:02 PST 2016\n"+
                 "New message from Adrian Cronauer: Good morning Vietnam",
                 output.toString());
  }
}
