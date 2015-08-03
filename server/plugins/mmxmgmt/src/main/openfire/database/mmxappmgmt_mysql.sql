# $Revision$
# $Date$

INSERT INTO ofVersion (name, version) VALUES ('mmxappmgmt', 4);

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
  apnsCertProduction tinyint      NULL,
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
  status       varchar(20)  NOT NULL DEFAULT 'ACTIVE',
  protocolVersionMajor  int(11),
  protocolVersionMinor  int(11),
  pushStatus   varchar(20)  NULL, /* push status indicating if Push message can be sent to this device */
  PRIMARY KEY (id),
  UNIQUE KEY `devicid_type_osType_appid` (`deviceId`,`osType`, `appId`)
);

CREATE TABLE mmxMessage (
  id               int(11)         NOT NULL AUTO_INCREMENT,
  messageId        varchar(100)    NOT NULL,
  deviceId         varchar(50)     NOT NULL, /* deviceId */
  fromJID          varchar(200)    NOT NULL, /* from JID */
  toJID            varchar(200)    NOT NULL, /* to JID   */
  dateQueuedUTC    int(11)        NOT NULL,
  state            varchar(50)     NOT NULL,
  appId            varchar(16)     NOT NULL,
  dateAcknowledgedUTC  int(11),
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
    appId           varchar(16) NULL,
    googleApiKey    varchar(100),
    payload         varchar(400)   NOT NULL,
    messageId       varchar(100)   NOT NULL,
    dateCreatedUTC     int(11),
    dateSentUTC        int(11),     /* date when we sent the wakeup */
    PRIMARY KEY (id)
 );

CREATE INDEX mmxWakeupQueue_dateCreated ON mmxWakeupQueue(dateCreatedUTC);

CREATE INDEX mmxWakeupQueue_dateSent ON mmxWakeupQueue(dateSentUTC);

/* TABLE for maintaining the push messages */
 CREATE TABLE mmxPushMessage (
    messageId   varchar(50)     NOT NULL,
    deviceId    varchar(50)     NOT NULL,   /* deviceId that is target for this push message */
    appId       varchar(16)     NOT NULL,   /* appId */
    dateSentUTC            int(11)        NOT NULL,   /* date when sent */
    dateAcknowledgedUTC    int(11),           /* date when receiver acknowledged the message */
    type        varchar(16)     NOT NULL,   /* type : consoleping, ping, pingpong, other */
    state       varchar(50)     NOT NULL,   /* PUSHED and ACKNOWLEDGED */
    PRIMARY KEY (messageId)
 );

/* Table for maintaining resource tagging data */

create Table mmxTag (
  id INT NOT NULL AUTO_INCREMENT,
  tagname VARCHAR(25) NOT NULL,
  creationDate DATETIME NOT NULL,
  appid VARCHAR(16) NOT NULL,
  deviceId INT,
  username VARCHAR(64),
  serviceID varchar(100) DEFAULT NULL,
  nodeID varchar(100) DEFAULT NULL,
  PRIMARY KEY(id),
  CONSTRAINT UNIQUE `tagname_appId_deviceId` (tagname, appId, deviceId),
  CONSTRAINT UNIQUE `tagname_appId_username` (tagname, appId, username),
  CONSTRAINT UNIQUE `tagname_appId_topic` (tagname,appid,serviceID, nodeID),
  CONSTRAINT CHECK (deviceId IS NOT NULL OR ofUsername IS NOT NULL OR (serviceID IS NOT NULL AND nodeID IS NOT NULL)),
  FOREIGN KEY (deviceId) REFERENCES mmxDevice(id) ON DELETE CASCADE,
  FOREIGN KEY (username) REFERENCES ofUser(username) ON DELETE CASCADE,
  FOREIGN KEY (serviceID, nodeID) REFERENCES ofPubsubNode(serviceID, nodeID) ON DELETE CASCADE
);

/* Table for app specific configuration */
CREATE TABLE mmxAppConfiguration (
    id          INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    appId       VARCHAR(16)     NOT NULL,
    configKey   VARCHAR(100)    NOT NULL,
    configValue VARCHAR(300)    NOT NULL,
    FOREIGN KEY (appId) REFERENCES mmxApp(appId) ON DELETE CASCADE
);

ALTER TABLE mmxAppConfiguration ADD UNIQUE KEY `mmxAppConfiguration_uk` (appId, configKey);

INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.admin.api.https.port', '6061') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.cluster.max.apps', '-1') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.rest.https.port', '5221') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.rest.http.port', '5220') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.alert.email.bcc.list', '') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.alert.email.subject', 'Usage limit exceeded') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.alert.email.port', '') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.retry.count', '0') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.alert.email.user', '') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.domain.name', '') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.cluster.max.devices.per.app', '-1') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.alert.email.enabled', 'false') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.rest.enable.https', 'true') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.wakeup.initialwait', '10') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.admin.api.enable.https', 'true') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.alert.inter.email.time.minutes', '15') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.retry.interval.minutes', '15') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.admin.api.port', '6060') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.alert.email.password', '') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.retry.mechanism', 'Standard') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.alert.email.host', '') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.timeout.period.minutes', '180') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.wakeup.frequency', '30') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.push.callback.host', '') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.push.callback.protocol', 'http') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.push.callback.port', '5220') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.apns.feedback.initialwait.min', '10') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.apns.feedback.frequency.min', '360') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.instance.max.xmpp.rate.per.sec', '-1') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.instance.max.http.rate.per.sec', '-1') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
