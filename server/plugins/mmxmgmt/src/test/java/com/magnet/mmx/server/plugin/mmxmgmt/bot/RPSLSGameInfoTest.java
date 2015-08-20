package com.magnet.mmx.server.plugin.mmxmgmt.bot;

import com.magnet.mmx.util.GsonData;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
/**
 * Created by rphadnis on 8/19/15.
 */

public class RPSLSGameInfoTest {

  @Test
  public void testGetType() throws Exception {
    String json = "{ \"gameId\" : \"1440024852.501820\", \"losses\" : \"0\", \"username\" : \"seaworld\", \"wins\" : \"0\", \"type\" : \"INVITATION\", \"timestamp\" : \"1440024852000\", \"ties\" : \"0\" }";

    RPSLSPlayerBotProcessor.RPSLSGameInfo gameInfo = GsonData.getGson().fromJson(json, RPSLSPlayerBotProcessor.RPSLSGameInfo.class);

    assertNotNull(gameInfo);
    RPSLSPlayerBotProcessor.RPSLSMessageType type = gameInfo.getType();
    assertNotNull(type);
    assertEquals("No matching", RPSLSPlayerBotProcessor.RPSLSMessageType.INVITATION, type);

  }
}