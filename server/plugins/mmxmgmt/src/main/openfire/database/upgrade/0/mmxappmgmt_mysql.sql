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
