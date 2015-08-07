package com.magnet.mmx.server.plugin.mmxmgmt.bot;

import junit.framework.TestCase;

/**
 * Created by rphadnis on 8/7/15.
 */
public class MMXMetaBuilderTest extends TestCase {

  public void testBuild() throws Exception {
    String json = MMXMetaBuilder.build("rahul", "device1");
    assertNotNull(json);
    String expected = "{\"To\":[{\"devId\":\"device1\",\"userId\":\"rahul\"}]}";
    assertEquals("Non matching json string", expected, json);
  }


  public void testBuild2() throws Exception {
    String json = MMXMetaBuilder.build("rahul", null);
    assertNotNull(json);
    String expected = "{\"To\":[{\"userId\":\"rahul\"}]}";
    assertEquals("Non matching json string", expected, json);
  }

  public void testBuild3() throws Exception {
    String json = MMXMetaBuilder.build(null, null);
    assertNotNull(json);
    String expected = "{\"To\":[{\"userId\":\"rahul\"}]}";
    assertEquals("Non matching json string", expected, json);
  }

}