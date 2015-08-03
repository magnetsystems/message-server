# $Revision: 1650 $
# $Date: 2005-07-20 00:18:17 -0300 (Wed, 20 Jul 2005) $

CREATE TABLE ofUser (
  username              VARCHAR(64)     NOT NULL,
  plainPassword         VARCHAR(32),
  encryptedPassword     VARCHAR(255),
  name                  VARCHAR(100),
  email                 VARCHAR(100),
  creationDate          CHAR(15)        NOT NULL,
  modificationDate      CHAR(15)        NOT NULL,
  PRIMARY KEY (username),
  INDEX ofUser_cDate_idx (creationDate)
);

CREATE TABLE ofUserProp (
  username              VARCHAR(64)     NOT NULL,
  name                  VARCHAR(100)    NOT NULL,
  propValue             TEXT            NOT NULL,
  PRIMARY KEY (username, name)
);

CREATE TABLE ofUserFlag (
  username              VARCHAR(64)     NOT NULL,
  name                  VARCHAR(100)    NOT NULL,
  startTime             CHAR(15),
  endTime               CHAR(15),
  PRIMARY KEY (username, name),
  INDEX ofUserFlag_sTime_idx (startTime),
  INDEX ofUserFlag_eTime_idx (endTime)
);

CREATE TABLE ofPrivate (
  username              VARCHAR(64)     NOT NULL,
  name                  VARCHAR(100)    NOT NULL,
  namespace             VARCHAR(200)    NOT NULL,
  privateData           TEXT            NOT NULL,
  PRIMARY KEY (username, name, namespace(100))
);

CREATE TABLE ofOffline (
  username              VARCHAR(200)     NOT NULL,
  messageID             BIGINT          NOT NULL,
  creationDate          CHAR(15)        NOT NULL,
  messageSize           INTEGER         NOT NULL,
  packetId              VARCHAR(100)    DEFAULT NULL,
  stanza                MEDIUMTEXT      NOT NULL,
  PRIMARY KEY (username, messageID)
);

CREATE TABLE ofPresence (
  username              VARCHAR(64)     NOT NULL,
  offlinePresence       TEXT,
  offlineDate           CHAR(15)     NOT NULL,
  PRIMARY KEY (username)
);

CREATE TABLE ofRoster (
  rosterID              BIGINT          NOT NULL,
  username              VARCHAR(64)     NOT NULL,
  jid                   VARCHAR(1024)   NOT NULL,
  sub                   TINYINT         NOT NULL,
  ask                   TINYINT         NOT NULL,
  recv                  TINYINT         NOT NULL,
  nick                  VARCHAR(255),
  PRIMARY KEY (rosterID),
  INDEX ofRoster_unameid_idx (username),
  INDEX ofRoster_jid_idx (jid(255))
);

CREATE TABLE ofRosterGroups (
  rosterID              BIGINT          NOT NULL,
  rank                  TINYINT         NOT NULL,
  groupName             VARCHAR(255)    NOT NULL,
  PRIMARY KEY (rosterID, rank),
  INDEX ofRosterGroup_rosterid_idx (rosterID)
);

CREATE TABLE ofVCard (
  username              VARCHAR(64)     NOT NULL,
  vcard                 MEDIUMTEXT      NOT NULL,
  PRIMARY KEY (username)
);

CREATE TABLE ofGroup (
  groupName             VARCHAR(50)     NOT NULL,
  description           VARCHAR(255),
  PRIMARY KEY (groupName)
);

CREATE TABLE ofGroupProp (
  groupName             VARCHAR(50)     NOT NULL,
  name                  VARCHAR(100)    NOT NULL,
  propValue             TEXT            NOT NULL,
  PRIMARY KEY (groupName, name)
);

CREATE TABLE ofGroupUser (
  groupName             VARCHAR(50)     NOT NULL,
  username              VARCHAR(100)    NOT NULL,
  administrator         TINYINT         NOT NULL,
  PRIMARY KEY (groupName, username, administrator)
);

CREATE TABLE ofID (
  idType                INTEGER         NOT NULL,
  id                    BIGINT          NOT NULL,
  PRIMARY KEY (idType)
);

CREATE TABLE ofProperty (
  name        VARCHAR(100)              NOT NULL,
  propValue   TEXT                      NOT NULL,
  PRIMARY KEY (name)
);


CREATE TABLE ofVersion (
  name     VARCHAR(50)  NOT NULL,
  version  INTEGER  NOT NULL,
  PRIMARY KEY (name)
);

CREATE TABLE ofExtComponentConf (
  subdomain             VARCHAR(255)    NOT NULL,
  wildcard              TINYINT         NOT NULL,
  secret                VARCHAR(255),
  permission            VARCHAR(10)     NOT NULL,
  PRIMARY KEY (subdomain)
);

CREATE TABLE ofRemoteServerConf (
  xmppDomain            VARCHAR(255)    NOT NULL,
  remotePort            INTEGER,
  permission            VARCHAR(10)     NOT NULL,
  PRIMARY KEY (xmppDomain)
);

CREATE TABLE ofPrivacyList (
  username              VARCHAR(64)     NOT NULL,
  name                  VARCHAR(100)    NOT NULL,
  isDefault             TINYINT         NOT NULL,
  list                  TEXT            NOT NULL,
  PRIMARY KEY (username, name),
  INDEX ofPrivacyList_default_idx (username, isDefault)
);

CREATE TABLE ofSASLAuthorized (
  username            VARCHAR(64)   NOT NULL,
  principal           TEXT          NOT NULL,
  PRIMARY KEY (username, principal(200))
);

CREATE TABLE ofSecurityAuditLog (
  msgID                 BIGINT          NOT NULL,
  username              VARCHAR(64)     NOT NULL,
  entryStamp            BIGINT          NOT NULL,
  summary               VARCHAR(255)    NOT NULL,
  node                  VARCHAR(255)    NOT NULL,
  details               TEXT,
  PRIMARY KEY (msgID),
  INDEX ofSecurityAuditLog_tstamp_idx (entryStamp),
  INDEX ofSecurityAuditLog_uname_idx (username)
);

# MUC Tables

CREATE TABLE ofMucService (
  serviceID           BIGINT        NOT NULL,
  subdomain           VARCHAR(255)  NOT NULL,
  description         VARCHAR(255),
  isHidden            TINYINT       NOT NULL,
  PRIMARY KEY (subdomain),
  INDEX ofMucService_serviceid_idx (serviceID)
);

CREATE TABLE ofMucServiceProp (
  serviceID           BIGINT        NOT NULL,
  name                VARCHAR(100)  NOT NULL,
  propValue           TEXT          NOT NULL,
  PRIMARY KEY (serviceID, name)
);

CREATE TABLE ofMucRoom (
  serviceID           BIGINT        NOT NULL,
  roomID              BIGINT        NOT NULL,
  creationDate        CHAR(15)      NOT NULL,
  modificationDate    CHAR(15)      NOT NULL,
  name                VARCHAR(50)   NOT NULL,
  naturalName         VARCHAR(255)  NOT NULL,
  description         VARCHAR(255),
  lockedDate          CHAR(15)      NOT NULL,
  emptyDate           CHAR(15)      NULL,
  canChangeSubject    TINYINT       NOT NULL,
  maxUsers            INTEGER       NOT NULL,
  publicRoom          TINYINT       NOT NULL,
  moderated           TINYINT       NOT NULL,
  membersOnly         TINYINT       NOT NULL,
  canInvite           TINYINT       NOT NULL,
  roomPassword        VARCHAR(50)   NULL,
  canDiscoverJID      TINYINT       NOT NULL,
  logEnabled          TINYINT       NOT NULL,
  subject             VARCHAR(100)  NULL,
  rolesToBroadcast    TINYINT       NOT NULL,
  useReservedNick     TINYINT       NOT NULL,
  canChangeNick       TINYINT       NOT NULL,
  canRegister         TINYINT       NOT NULL,
  PRIMARY KEY (serviceID,name),
  INDEX ofMucRoom_roomid_idx (roomID),
  INDEX ofMucRoom_serviceid_idx (serviceID)
);

CREATE TABLE ofMucRoomProp (
  roomID                BIGINT          NOT NULL,
  name                  VARCHAR(100)    NOT NULL,
  propValue             TEXT            NOT NULL,
  PRIMARY KEY (roomID, name)
);

CREATE TABLE ofMucAffiliation (
  roomID              BIGINT        NOT NULL,
  jid                 TEXT          NOT NULL,
  affiliation         TINYINT       NOT NULL,
  PRIMARY KEY (roomID,jid(70))
);

CREATE TABLE ofMucMember (
  roomID              BIGINT        NOT NULL,
  jid                 TEXT          NOT NULL,
  nickname            VARCHAR(255)  NULL,
  firstName           VARCHAR(100)  NULL,
  lastName            VARCHAR(100)  NULL,
  url                 VARCHAR(100)  NULL,
  email               VARCHAR(100)  NULL,
  faqentry            VARCHAR(100)  NULL,
  PRIMARY KEY (roomID,jid(70))
);

CREATE TABLE ofMucConversationLog (
  roomID              BIGINT        NOT NULL,
  sender              TEXT          NOT NULL,
  nickname            VARCHAR(255)  NULL,
  logTime             CHAR(15)      NOT NULL,
  subject             VARCHAR(255)  NULL,
  body                TEXT          NULL,
  INDEX ofMucConversationLog_time_idx (logTime)
);

# PubSub Tables

CREATE TABLE ofPubsubNode (
  serviceID           VARCHAR(100)  NOT NULL,
  nodeID              VARCHAR(100)  NOT NULL,
  leaf                TINYINT       NOT NULL,
  creationDate        CHAR(15)      NOT NULL,
  modificationDate    CHAR(15)      NOT NULL,
  parent              VARCHAR(100)  NULL,
  deliverPayloads     TINYINT       NOT NULL,
  maxPayloadSize      INTEGER       NULL,
  persistItems        TINYINT       NULL,
  maxItems            INTEGER       NULL,
  notifyConfigChanges TINYINT       NOT NULL,
  notifyDelete        TINYINT       NOT NULL,
  notifyRetract       TINYINT       NOT NULL,
  presenceBased       TINYINT       NOT NULL,
  sendItemSubscribe   TINYINT       NOT NULL,
  publisherModel      VARCHAR(15)   NOT NULL,
  subscriptionEnabled TINYINT       NOT NULL,
  configSubscription  TINYINT       NOT NULL,
  accessModel         VARCHAR(10)   NOT NULL,
  payloadType         VARCHAR(100)  NULL,
  bodyXSLT            VARCHAR(100)  NULL,
  dataformXSLT        VARCHAR(100)  NULL,
  creator             VARCHAR(255) NOT NULL,
  description         VARCHAR(255)  NULL,
  language            VARCHAR(255)  NULL,
  name                VARCHAR(50)   NULL,
  replyPolicy         VARCHAR(15)   NULL,
  associationPolicy   VARCHAR(15)   NULL,
  maxLeafNodes        INTEGER       NULL,
  PRIMARY KEY (serviceID, nodeID)
);

CREATE TABLE ofPubsubNodeJIDs (
  serviceID           VARCHAR(100)  NOT NULL,
  nodeID              VARCHAR(100)  NOT NULL,
  jid                 VARCHAR(255)  NOT NULL,
  associationType     VARCHAR(20)   NOT NULL,
  PRIMARY KEY (serviceID, nodeID, jid(70))
);

CREATE TABLE ofPubsubNodeGroups (
  serviceID           VARCHAR(100)  NOT NULL,
  nodeID              VARCHAR(100)  NOT NULL,
  rosterGroup         VARCHAR(100)   NOT NULL,
  INDEX ofPubsubNodeGroups_idx (serviceID, nodeID)
);

CREATE TABLE ofPubsubAffiliation (
  serviceID           VARCHAR(100)  NOT NULL,
  nodeID              VARCHAR(100)  NOT NULL,
  jid                 VARCHAR(255) NOT NULL,
  affiliation         VARCHAR(10)   NOT NULL,
  PRIMARY KEY (serviceID, nodeID, jid(70))
);

CREATE TABLE ofPubsubItem (
  serviceID           VARCHAR(100)  NOT NULL,
  nodeID              VARCHAR(100)  NOT NULL,
  id                  VARCHAR(100)  NOT NULL,
  jid                 VARCHAR(255)  NOT NULL,
  creationDate        CHAR(15)      NOT NULL,
  payload             MEDIUMTEXT    NULL,
  PRIMARY KEY (serviceID, nodeID, id)
);

CREATE TABLE ofPubsubSubscription (
  serviceID           VARCHAR(100)  NOT NULL,
  nodeID              VARCHAR(100)  NOT NULL,
  id                  VARCHAR(100)  NOT NULL,
  jid                 VARCHAR(255) NOT NULL,
  owner               VARCHAR(255) NOT NULL,
  state               VARCHAR(15)   NOT NULL,
  deliver             TINYINT       NOT NULL,
  digest              TINYINT       NOT NULL,
  digest_frequency    INT           NOT NULL,
  expire              CHAR(15)      NULL,
  includeBody         TINYINT       NOT NULL,
  showValues          VARCHAR(30)   NULL,
  subscriptionType    VARCHAR(10)   NOT NULL,
  subscriptionDepth   TINYINT       NOT NULL,
  keyword             VARCHAR(200)  NULL,
  PRIMARY KEY (serviceID, nodeID, id)
);

CREATE TABLE ofPubsubDefaultConf (
  serviceID           VARCHAR(100)  NOT NULL,
  leaf                TINYINT       NOT NULL,
  deliverPayloads     TINYINT       NOT NULL,
  maxPayloadSize      INTEGER       NOT NULL,
  persistItems        TINYINT       NOT NULL,
  maxItems            INTEGER       NOT NULL,
  notifyConfigChanges TINYINT       NOT NULL,
  notifyDelete        TINYINT       NOT NULL,
  notifyRetract       TINYINT       NOT NULL,
  presenceBased       TINYINT       NOT NULL,
  sendItemSubscribe   TINYINT       NOT NULL,
  publisherModel      VARCHAR(15)   NOT NULL,
  subscriptionEnabled TINYINT       NOT NULL,
  accessModel         VARCHAR(10)   NOT NULL,
  language            VARCHAR(255)  NULL,
  replyPolicy         VARCHAR(15)   NULL,
  associationPolicy   VARCHAR(15)   NOT NULL,
  maxLeafNodes        INTEGER       NOT NULL,
  PRIMARY KEY (serviceID, leaf)
);

# Finally, insert default table values.

INSERT INTO ofID (idType, id) VALUES (18, 1);
INSERT INTO ofID (idType, id) VALUES (19, 1);
INSERT INTO ofID (idType, id) VALUES (23, 1);
INSERT INTO ofID (idType, id) VALUES (26, 2);

INSERT INTO ofVersion (name, version) VALUES ('openfire', 21);

# Entry for admin user
INSERT INTO ofUser (username, plainPassword, name, email, creationDate, modificationDate)
    VALUES ('admin', 'admin', 'Administrator', 'admin@example.com', '0', '0');

# Entry for default conference service
INSERT INTO ofMucService (serviceID, subdomain, isHidden) VALUES (1, 'conference', 0);

insert into ofProperty values ('provider.lockout.className', 'com.magnet.mmx.lockout.MmxLockoutProvider');

insert into ofProperty values ('xmpp.parser.buffer.size', '2097152');

insert into ofProperty values('xmpp.routing.strict', 'true');

insert into ofProperty values('xmpp.client.idle', '-1');

insert into ofProperty values('xmpp.client.idle.ping', 'false');
# $Revision$
# $Date$

INSERT INTO ofVersion (name, version) VALUES ('mmxappmgmt', 0);

CREATE TABLE mmxApp (
  id                INT           NOT NULL AUTO_INCREMENT PRIMARY KEY,
  serverUserId      VARCHAR(200),
  appName           VARCHAR(100)  NOT NULL,
  appId             VARCHAR(16)   NOT NULL, /* appId is a string of 16 characters */
  apiKey            VARCHAR(100)  DEFAULT NULL,
  encryptedApiKey   VARCHAR(255)  DEFAULT NULL,
  googleApiKey      VARCHAR(100),
  googleProjectId   VARCHAR(100),
  apnsCert          VARBINARY(5000), /* in .p12 binary file format */
  apnsCertPlainPassword VARCHAR(100)      DEFAULT NULL,
  apnsCertEncryptedPassword VARCHAR(255)  DEFAULT NULL,
  creationDate      datetime      NOT NULL,
  modificationDate  datetime     NULL,
  ownerId           VARCHAR(200),
  ownerEmail        VARCHAR(255) DEFAULT NULL,
  guestUserId       VARCHAR(200),
  guestSecret       VARCHAR(255), 	
  UNIQUE KEY `mmxApp_appId` (`appId`)
);

CREATE INDEX mmxApp_serverUserJJID ON mmxApp(serverUserId);
CREATE INDEX mmxApp_apiKey ON mmxApp(apiKey);

CREATE TABLE mmxDevice (
  id            int(11)        NOT NULL AUTO_INCREMENT,
  name          varchar(100)   NOT NULL,           /* display name of the device */
  ownerJid     varchar(200)    NOT NULL,           /* bare user identifier       */
  appId        varchar(16)    NOT NULL,
  osType       varchar(20)     NOT NULL,           /* os type of the device (iOS,ANDRIOD) */
  deviceId     varchar(50)     NOT NULL,           /* deviceId                            */
  tokenType    varchar(10),           /* token type */
  clientToken  varchar(500),          /* client token for push notifications */
  versionInfo  varchar(20),           /* version string for the OS */
  modelInfo    varchar(200),          /* model info */
  phoneNumber  varchar(20),           /* phoneNumber */
  phoneNumberRev  varchar(20),        /* phoneNumber reversed */
  carrierInfo  varchar(20),           /* carrier information */
  dateCreated  datetime     NOT NULL,
  dateUpdated  datetime     NULL,
  status        varchar(20) NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (id),
  UNIQUE KEY `devicid_type_osType_appid` (`deviceId`,`osType`, `appId`)
);

CREATE TABLE mmxMessage (
  id               int(11)         NOT NULL AUTO_INCREMENT,
  messageId        varchar(100)    NOT NULL,
  deviceId         varchar(50)     NOT NULL, /* deviceId */
  fromJID          varchar(200)    NOT NULL, /* from JID */
  toJID            varchar(200)    NOT NULL, /* to JID   */
  dateQueued       datetime        NOT NULL,
  state            varchar(50)     NOT NULL,
  appId            varchar(16)     NOT NULL,
  dateAcknowledged datetime,
  sourceMessageId  varchar(100),
  messageType      varchar(50),
  PRIMARY KEY (id)
  );

CREATE INDEX mmxMessage_messageId ON mmxMessage(messageId);

 CREATE TABLE mmxWakeupQueue (
    id              int(11)        NOT NULL AUTO_INCREMENT,
    deviceId        varchar(50)    NOT NULL, /* deviceId */
    clientToken     varchar(500),            /* client token for push notifications */
    tokenType       varchar(10),
    googleApiKey    varchar(100),
    payload         varchar(400)   NOT NULL,
    messageId       varchar(100)   NOT NULL,
    dateCreated     datetime,
    dateSent        datetime,     /* date when we sent the wakeup */
    PRIMARY KEY (id)
 );

CREATE INDEX mmxWakeupQueue_dateCreated ON mmxWakeupQueue(dateCreated);

CREATE INDEX mmxWakeupQueue_dateSent ON mmxWakeupQueue(dateSent);

/* TABLE for maintaining the push messages */
 CREATE TABLE mmxPushMessage (
    messageId   varchar(50)     NOT NULL,
    deviceId    varchar(50)     NOT NULL,   /* deviceId that is target for this push message */
    appId       varchar(16)     NOT NULL,   /* appId */
    dateSent    datetime        NOT NULL,   /* date when sent */
    dateAcknowledged    datetime,           /* date when receiver acknowledged the message */
    type        varchar(16)     NOT NULL,   /* type : consoleping, ping, pingpong, other */
    state       varchar(50)     NOT NULL,   /* PUSHED and ACKNOWLEDGED */
    PRIMARY KEY (messageId)
 );
