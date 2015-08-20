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
package com.magnet.mmx.server.plugin.mmxmgmt.bot;


import com.google.gson.annotations.SerializedName;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MessageBuilder;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MessageIdGenerator;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MessageIdGeneratorImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXExecutors;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.util.GsonData;
import com.magnet.mmx.util.JSONifiable;
import com.magnet.mmx.util.Utils;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import java.security.SecureRandom;
import java.text.DateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ExecutorService;

/**
 * Bot processor that processes and responds with RPSLS messages
 * Note: This doesn't do message receipts.
 */
public class RPSLSPlayerBotProcessor implements AutoResponseProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(RPSLSPlayerBotProcessor.class);
  private AutoRespondingConnection connection;
  private static final String[] POSSIBLE_CHOICES = {"ROCK", "PAPER", "LIZARD", "SPOCK", "SCISSOR"};
  private static final String INVITE_REPLY_MESSAGE = "This is an invite reply message";
  private static final String CHOICE_TEMPLATE = "I chose %s";
  private static final String RPSLS_BOT_POOL_NAME = "RPSLSBOT_POOL";

  private static Random randomGenerator;
  //statically initialize the random number generator
  {
    try {
      //try to use secure random based on this CERT advisory:
      //https://www.securecoding.cert.org/confluence/display/java/MSC02-J.+Generate+strong+random+numbers
      randomGenerator = SecureRandom.getInstance(MMXServerConstants.SECURE_RANDOM_ALGORITHM);
    } catch (Throwable t) {
      LOGGER.error("Problem in initializing the random number generator. Falling back to java.util.Random", t);
      randomGenerator = new Random();
    }
  }

  @Override
  public void initialize(AutoRespondingConnection connection) {
    this.connection = connection;
  }

  @Override
  public void processIncoming(Packet packet) {
    if (packet instanceof Message) {
      Message message = (Message) packet;
      JID fromJID = packet.getFrom();
      JID toJID = packet.getTo();
      packet.setTo(fromJID);

      Element mmx = message.getChildElement(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD);
      Element meta = mmx.element(Constants.MMX_META);
      if (meta == null) {
        LOGGER.info("No meta in the message with id:{}", message.getID());
        return;
      }
      //Simulate SDK ack
      String originalMessageId = packet.getID();
      String from = fromJID.toString();
      String to = toJID.toString();

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Attempting to deliver ack");
      }
      IQ ackIQ = BotRegistrationImpl.buildAckIQ(from, to, originalMessageId);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Ack IQ:{}", ackIQ.toXML());
      }
      connection.sendPacket(ackIQ);
      String metaJSON = meta.getText();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Meta JSON:{}", metaJSON);
      }
      RPSLSGameInfo gameInfo = GsonData.getGson().fromJson(metaJSON, RPSLSGameInfo.class);

      if (gameInfo.getType() == RPSLSMessageType.INVITATION) {
        //process an invitation
        //step 1. Send an acceptance
        //step 2. Send a choice after 2 seconds.
        processRPSLSInvitationMessage(fromJID, toJID, gameInfo);
      } else {
        //ignore the other types
        LOGGER.info("Ignoring a message with type:{} message:{}", gameInfo.getType(), message);
      }
    }
  }

  /**
   * Process the RPSLS invitation message.
   * @param sourceFrom
   * @param sourceTo
   * @param gameInfo
   */
  protected void processRPSLSInvitationMessage(final JID sourceFrom, final JID sourceTo, final RPSLSGameInfo gameInfo) {
    //first build the acceptance message
    LOGGER.debug("Building acceptance message");
    Message acceptance = buildAcceptanceMessage(sourceFrom, sourceTo, gameInfo);
    LOGGER.info("Sending the RPSLS acceptance message {}", acceptance);
    connection.sendPacket(acceptance);
    ExecutorService executor = MMXExecutors.getOrCreate(RPSLS_BOT_POOL_NAME, 5);
    executor.execute(new Runnable() {
      @Override
      public void run() {
        //sleep for two seconds and then send the message.
        try {
          //sleep for 2 seconds and then send the message
          Thread.sleep(2000L);
        } catch (InterruptedException e) {
          LOGGER.debug("Interrupted exception", e);
        } finally {
        }
        Message choiceMessage = buildChoiceMessage(sourceFrom, sourceTo, gameInfo);
        LOGGER.debug("Sending choice message:{}", choiceMessage);
        connection.sendPacket(choiceMessage);
      }
    });
  }


  /**
   * Build the acceptance message.
   * @param sourceFrom
   * @param sourceTo
   * @param gameInfo
   * @return
   */
  protected Message buildAcceptanceMessage(JID sourceFrom, JID sourceTo, RPSLSGameInfo gameInfo) {
    JID fromJID = sourceTo;
    JID toJID = sourceFrom;

    String appId = JIDUtil.getAppId(toJID);
    Message message = new Message();
    message.setTo(toJID);
    message.setFrom(fromJID);

    MessageIdGenerator generator = new MessageIdGeneratorImpl();
    String id = generator.generate(toJID.getNode(), appId, null);
    message.setID(id);
    message.setType(Message.Type.chat);

    Element mmx = message.addChildElement(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD);

    Element internalMeta = mmx.addElement(Constants.MMX_MMXMETA);
    String userId = JIDUtil.getUserId(toJID);
    String devId = fromJID.getResource();
    //mmx meta
    String mmxMetaJSON = MMXMetaBuilder.build(userId, devId);
    internalMeta.setText(mmxMetaJSON);

    Element meta = mmx.addElement(Constants.MMX_META);
    AcceptanceRPSLSGameInfo acceptance = new AcceptanceRPSLSGameInfo();
    acceptance.setGameId(gameInfo.gameId);
    acceptance.setLosses(0);
    acceptance.setWins(0);
    acceptance.setTies(0);
    acceptance.setTimestamp(gameInfo.getTimestamp());
    acceptance.setType(RPSLSMessageType.ACCEPTANCE);
    acceptance.setUsername(userId);

    String acceptanceJSON = acceptance.toJson();
    meta.setText(acceptanceJSON);

    Element payloadElement = mmx.addElement(Constants.MMX_PAYLOAD);
    DateFormat fmt = Utils.buildISO8601DateFormat();
    String formattedDateTime = fmt.format(new Date());
    payloadElement.addAttribute(Constants.MMX_ATTR_STAMP, formattedDateTime);
    String text = INVITE_REPLY_MESSAGE;
    payloadElement.setText(text);
    payloadElement.addAttribute(Constants.MMX_ATTR_CHUNK, MessageBuilder.buildChunkAttributeValue(text));
    return message;
  }

  /**
   * Build a message by randomly choosing from the possible choices.
   * @param sourceFrom
   * @param sourceTo
   * @param gameInfo
   * @return
   */
  protected Message buildChoiceMessage(JID sourceFrom, JID sourceTo, RPSLSGameInfo gameInfo) {
    JID fromJID = sourceTo;
    JID toJID = sourceFrom;

    String appId = JIDUtil.getAppId(toJID);

    Message choiceMessage = new Message();

    MessageIdGenerator generator = new MessageIdGeneratorImpl();
    String id = generator.generate(toJID.getNode(), appId, null);
    choiceMessage.setID(id);
    choiceMessage.setType(Message.Type.chat);
    choiceMessage.setTo(toJID);
    choiceMessage.setFrom(fromJID);

    Element mmx = choiceMessage.addChildElement(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD);
    Element internalMeta = mmx.addElement(Constants.MMX_MMXMETA);
    String userId = JIDUtil.getUserId(toJID);
    String devId = fromJID.getResource();
    //mmx meta
    String mmxMetaJSON = MMXMetaBuilder.build(userId, devId);
    internalMeta.setText(mmxMetaJSON);

    Element meta = mmx.addElement(Constants.MMX_META);
    ChoiceRPSLSGameInfo choice = new ChoiceRPSLSGameInfo();
    choice.setGameId(gameInfo.getGameId());
    choice.setLosses(0);
    choice.setWins(0);
    choice.setTies(0);
    choice.setTimestamp(gameInfo.getTimestamp());
    choice.setType(RPSLSMessageType.CHOICE);
    choice.setUsername(userId);
    String choiceValue = getRandomChoice();
    choice.setChoice(choiceValue);

    String choiceJSON = choice.toJson();
    meta.setText(choiceJSON);

    Element payloadElement = mmx.addElement(Constants.MMX_PAYLOAD);
    DateFormat fmt = Utils.buildISO8601DateFormat();
    String formattedDateTime = fmt.format(new Date());
    payloadElement.addAttribute(Constants.MMX_ATTR_STAMP, formattedDateTime);
    String text = String.format(CHOICE_TEMPLATE, choiceValue);
    payloadElement.setText(text);
    payloadElement.addAttribute(Constants.MMX_ATTR_CHUNK, MessageBuilder.buildChunkAttributeValue(text));

    return choiceMessage;
  }


  @Override
  public void processIncomingRaw(String rawText) {
    LOGGER.info("Process Incoming raw");
  }

  @Override
  public void terminate() {
    LOGGER.info("Terminating Bot processor ");
    if (connection != null) {
      connection = null;
    }
  }

  public String getRandomChoice() {
    int index = randomGenerator.nextInt(5);
    return POSSIBLE_CHOICES[index];
  }


  enum RPSLSMessageType {
    /**
     * Used with invitation message
     */
    INVITATION,//INVITATION

    /**
     * Acceptance message
     */
    ACCEPTANCE,

    /**
     * Send a choice message
     */
    CHOICE
    ;
  }

  /**
   * RPSLS messages use meta with a specific JSON content. This class models that content.
   */
  class RPSLSGameInfo extends JSONifiable {
    private String gameId;
    private int losses;
    private int wins;
    private String username;
    private RPSLSMessageType type;
    private long timestamp;
    private int ties;

    public String getGameId() {
      return gameId;
    }

    public void setGameId(String gameId) {
      this.gameId = gameId;
    }

    public int getLosses() {
      return losses;
    }

    public void setLosses(int losses) {
      this.losses = losses;
    }

    public int getTies() {
      return ties;
    }

    public void setTies(int ties) {
      this.ties = ties;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public void setTimestamp(long timestamp) {
      this.timestamp = timestamp;
    }

    public RPSLSMessageType getType() {
      return type;
    }

    public void setType(RPSLSMessageType type) {
      this.type = type;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public int getWins() {
      return wins;
    }

    public void setWins(int wins) {
      this.wins = wins;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("RPSLGameInfo{");
      sb.append("gameId='").append(gameId).append('\'');
      sb.append(", losses=").append(losses);
      sb.append(", wins=").append(wins);
      sb.append(", username='").append(username).append('\'');
      sb.append(", type=").append(type);
      sb.append(", timestamp=").append(timestamp);
      sb.append(", ties=").append(ties);
      sb.append('}');
      return sb.toString();
    }
  }

  /**
   * Acceptance game info.
   */
  class AcceptanceRPSLSGameInfo extends RPSLSGameInfo {
    @SerializedName("isAccept")
    private boolean acceptance = true;

    public boolean isAcceptance() {
      return acceptance;
    }
  }

  /**
   * Gameinfo with the choice element.
   */
  class ChoiceRPSLSGameInfo extends RPSLSGameInfo {
    private String choice;

    public String getChoice() {
      return choice;
    }

    public void setChoice(String choice) {
      this.choice = choice;
    }
  }

}
