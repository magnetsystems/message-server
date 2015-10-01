
CREATE TABLE ofUser (
  username              VARCHAR(64)     NOT NULL,
  plainPassword         VARCHAR(32),
  encryptedPassword     VARCHAR(255),
  name                  VARCHAR(100),
  email                 VARCHAR(100),
  creationDate          VARCHAR(15)     NOT NULL,
  modificationDate      VARCHAR(15)     NOT NULL,
  CONSTRAINT ofUser_pk PRIMARY KEY (username)
);
CREATE INDEX ofUser_cDate_idx ON ofUser (creationDate);


CREATE TABLE ofUserProp (
  username              VARCHAR(64)     NOT NULL,
  name                  VARCHAR(100)    NOT NULL,
  propValue             VARCHAR(4000)   NOT NULL,
  CONSTRAINT ofUserProp_pk PRIMARY KEY (username, name)
);


CREATE TABLE ofUserFlag (
  username              VARCHAR(64)     NOT NULL,
  name                  VARCHAR(100)    NOT NULL,
  startTime             VARCHAR(15),
  endTime               VARCHAR(15),
  CONSTRAINT ofUserFlag_pk PRIMARY KEY (username, name)
);
CREATE INDEX ofUserFlag_sTime_idx ON ofUserFlag (startTime);
CREATE INDEX ofUserFlag_eTime_idx ON ofUserFlag (endTime);


CREATE TABLE ofPrivate (
  username              VARCHAR(64)     NOT NULL,
  name                  VARCHAR(100)    NOT NULL,
  namespace             VARCHAR(200)    NOT NULL,
  privateData           LONGVARCHAR     NOT NULL,
  CONSTRAINT ofPrivate_pk PRIMARY KEY (username, name, namespace)
);


CREATE TABLE ofOffline (
  username              VARCHAR(64)     NOT NULL,
  messageID             BIGINT          NOT NULL,
  creationDate          VARCHAR(15)     NOT NULL,
  messageSize           INTEGER         NOT NULL,
  stanza                LONGVARCHAR     NOT NULL,
  CONSTRAINT ofOffline_pk PRIMARY KEY (username, messageID)
);

CREATE TABLE ofPresence (
  username              VARCHAR(64)     NOT NULL,
  offlinePresence       LONGVARCHAR,
  offlineDate           VARCHAR(15)     NOT NULL,
  CONSTRAINT ofPresence_pk PRIMARY KEY (username)
);

CREATE TABLE ofRoster (
  rosterID              BIGINT          NOT NULL,
  username              VARCHAR(64)     NOT NULL,
  jid                   VARCHAR(1024)   NOT NULL,
  sub                   INTEGER         NOT NULL,
  ask                   INTEGER         NOT NULL,
  recv                  INTEGER         NOT NULL,
  nick                  VARCHAR(255),
  CONSTRAINT ofRoster_pk PRIMARY KEY (rosterID)
);
CREATE INDEX ofRoster_username_idx ON ofRoster (username);
CREATE INDEX ofRoster_jid_idx ON ofRoster (jid);


CREATE TABLE ofRosterGroups (
  rosterID              BIGINT          NOT NULL,
  rank                  INTEGER         NOT NULL,
  groupName             VARCHAR(255)    NOT NULL,
  CONSTRAINT ofRosterGroups_pk PRIMARY KEY (rosterID, rank)
);
CREATE INDEX ofRosterGroup_rosterid_idx ON ofRosterGroups (rosterID);


CREATE TABLE ofVCard (
  username              VARCHAR(64)     NOT NULL,
  vcard                 LONGVARCHAR     NOT NULL,
  CONSTRAINT ofVCard_pk PRIMARY KEY (username)
);


CREATE TABLE ofGroup (
  groupName              VARCHAR(50)     NOT NULL,
  description           VARCHAR(255),
  CONSTRAINT ofGroup_pk PRIMARY KEY (groupName)
);


CREATE TABLE ofGroupProp (
  groupName             VARCHAR(50)     NOT NULL,
  name                  VARCHAR(100)    NOT NULL,
  propValue             VARCHAR(4000)   NOT NULL,
  CONSTRAINT ofGroupProp_pk PRIMARY KEY (groupName, name)
);


CREATE TABLE ofGroupUser (
  groupName             VARCHAR(50)     NOT NULL,
  username              VARCHAR(100)    NOT NULL,
  administrator         INTEGER         NOT NULL,
  CONSTRAINT ofGroupUser_pk PRIMARY KEY (groupName, username, administrator)
);


CREATE TABLE ofID (
  idType                INTEGER         NOT NULL,
  id                    BIGINT          NOT NULL,
  CONSTRAINT ofID_pk PRIMARY KEY (idType)
);


CREATE TABLE ofProperty (
  name        VARCHAR(100)  NOT NULL,
  propValue   VARCHAR(4000) NOT NULL,
  CONSTRAINT ofProperty_pk PRIMARY KEY (name)
);


CREATE TABLE ofVersion (
  name  varchar(50)  NOT NULL,
  version  INTEGER  NOT NULL,
  CONSTRAINT ofVersion_pk PRIMARY KEY (name)
);

CREATE TABLE ofExtComponentConf (
  subdomain             VARCHAR(255)    NOT NULL,
  wildcard              INTEGER         NOT NULL,
  secret                VARCHAR(255),
  permission            VARCHAR(10)     NOT NULL,
  CONSTRAINT ofExtComponentConf_pk PRIMARY KEY (subdomain)
);

CREATE TABLE ofRemoteServerConf (
  xmppDomain            VARCHAR(255)    NOT NULL,
  remotePort            INTEGER,
  permission            VARCHAR(10)     NOT NULL,
  CONSTRAINT ofRemoteServerConf_pk PRIMARY KEY (xmppDomain)
);

CREATE TABLE ofPrivacyList (
  username              VARCHAR(64)     NOT NULL,
  name                  VARCHAR(100)    NOT NULL,
  isDefault             INTEGER         NOT NULL,
  list                  LONGVARCHAR     NOT NULL,
  CONSTRAINT ofPrivacyList_pk PRIMARY KEY (username, name)
);
CREATE INDEX ofPrivacyList_default_idx ON ofPrivacyList (username, isDefault);

CREATE TABLE ofSASLAuthorized (
  username        VARCHAR(64)      NOT NULL,
  principal       VARCHAR(4000)    NOT NULL,
  CONSTRAINT ofSASLAuthorized_pk PRIMARY KEY (username, principal)
);

CREATE TABLE ofSecurityAuditLog (
  msgID                 BIGINT          NOT NULL,
  username              VARCHAR(64)     NOT NULL,
  entryStamp            BIGINT          NOT NULL,
  summary               VARCHAR(255)    NOT NULL,
  node                  VARCHAR(255)    NOT NULL,
  details               LONGVARCHAR,
  CONSTRAINT ofSecurityAuditLog_pk PRIMARY KEY (msgID)
);
CREATE INDEX ofSecurityAuditLog_tstamp_idx ON ofSecurityAuditLog (entryStamp);
CREATE INDEX ofSecurityAuditLog_uname_idx ON ofSecurityAuditLog (username);

CREATE TABLE ofMucService (
  serviceID           BIGINT        NOT NULL,
  subdomain           VARCHAR(255)  NOT NULL,
  description         VARCHAR(255),
  isHidden            INTEGER       NOT NULL,
  CONSTRAINT ofMucService_pk PRIMARY KEY (subdomain)
);
CREATE INDEX ofMucService_serviceid_idx ON ofMucService(serviceID);

CREATE TABLE ofMucServiceProp (
  serviceID           BIGINT        NOT NULL,
  name                VARCHAR(100)  NOT NULL,
  propValue           VARCHAR(4000) NOT NULL,
  CONSTRAINT ofMucServiceProp_pk PRIMARY KEY (serviceID, name)
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
  canChangeSubject    INTEGER       NOT NULL,
  maxUsers            INTEGER       NOT NULL,
  publicRoom          INTEGER       NOT NULL,
  moderated           INTEGER       NOT NULL,
  membersOnly         INTEGER       NOT NULL,
  canInvite           INTEGER       NOT NULL,
  roomPassword        VARCHAR(50)   NULL,
  canDiscoverJID      INTEGER       NOT NULL,
  logEnabled          INTEGER       NOT NULL,
  subject             VARCHAR(100)  NULL,
  rolesToBroadcast    INTEGER       NOT NULL,
  useReservedNick     INTEGER       NOT NULL,
  canChangeNick       INTEGER       NOT NULL,
  canRegister         INTEGER       NOT NULL,
  CONSTRAINT ofMucRoom_pk PRIMARY KEY (serviceID, name)
);
CREATE INDEX ofMucRoom_roomid_idx ON ofMucRoom (roomID);
CREATE INDEX ofMucRoom_serviceid_idx ON ofMucRoom (serviceID);

CREATE TABLE ofMucRoomProp (
  roomID                BIGINT          NOT NULL,
  name                  VARCHAR(100)    NOT NULL,
  propValue             VARCHAR(4000)   NOT NULL,
  CONSTRAINT ofMucRoomProp_pk PRIMARY KEY (roomID, name)
);

CREATE TABLE ofMucAffiliation (
  roomID              BIGINT        NOT NULL,
  jid                 VARCHAR(1024) NOT NULL,
  affiliation         INTEGER       NOT NULL,
  CONSTRAINT ofMucAffiliation_pk PRIMARY KEY (roomID, jid)
);

CREATE TABLE ofMucMember (
  roomID              BIGINT        NOT NULL,
  jid                 VARCHAR(1024) NOT NULL,
  nickname            VARCHAR(255)  NULL,
  firstName           VARCHAR(100)  NULL,
  lastName            VARCHAR(100)  NULL,
  url                 VARCHAR(100)  NULL,
  email               VARCHAR(100)  NULL,
  faqentry            VARCHAR(100)  NULL,
  CONSTRAINT ofMucMember_pk PRIMARY KEY (roomID, jid)
);

CREATE TABLE ofMucConversationLog (
  roomID              BIGINT        NOT NULL,
  sender              VARCHAR(1024) NOT NULL,
  nickname            VARCHAR(255)  NULL,
  logTime             CHAR(15)      NOT NULL,
  subject             VARCHAR(255)  NULL,
  body                LONGVARCHAR   NULL
);
CREATE INDEX ofMucConversationLog_time_idx ON ofMucConversationLog (logTime);

CREATE TABLE ofPubsubNode (
  serviceID           VARCHAR(100)  NOT NULL,
  nodeID              VARCHAR(100)  NOT NULL,
  leaf                INTEGER       NOT NULL,
  creationDate        CHAR(15)      NOT NULL,
  modificationDate    CHAR(15)      NOT NULL,
  parent              VARCHAR(100)  NULL,
  deliverPayloads     INTEGER       NOT NULL,
  maxPayloadSize      INTEGER       NULL,
  persistItems        INTEGER       NULL,
  maxItems            INTEGER       NULL,
  notifyConfigChanges INTEGER       NOT NULL,
  notifyDelete        INTEGER       NOT NULL,
  notifyRetract       INTEGER       NOT NULL,
  presenceBased       INTEGER       NOT NULL,
  sendItemSubscribe   INTEGER       NOT NULL,
  publisherModel      VARCHAR(15)   NOT NULL,
  subscriptionEnabled INTEGER       NOT NULL,
  configSubscription  INTEGER       NOT NULL,
  accessModel         VARCHAR(10)   NOT NULL,
  payloadType         VARCHAR(100)  NULL,
  bodyXSLT            VARCHAR(100)  NULL,
  dataformXSLT        VARCHAR(100)  NULL,
  creator             VARCHAR(1024) NOT NULL,
  description         VARCHAR(255)  NULL,
  language            VARCHAR(255)  NULL,
  name                VARCHAR(50)   NULL,
  replyPolicy         VARCHAR(15)   NULL,
  associationPolicy   VARCHAR(15)   NULL,
  maxLeafNodes        INTEGER       NULL,
  CONSTRAINT ofPubsubNode_pk PRIMARY KEY (serviceID, nodeID)
);

CREATE TABLE ofPubsubNodeJIDs (
  serviceID           VARCHAR(100)  NOT NULL,
  nodeID              VARCHAR(100)  NOT NULL,
  jid                 VARCHAR(1024) NOT NULL,
  associationType     VARCHAR(20)   NOT NULL,
  CONSTRAINT ofPubsubNodeJIDs_pk PRIMARY KEY (serviceID, nodeID, jid)
);

CREATE TABLE ofPubsubNodeGroups (
  serviceID           VARCHAR(100)  NOT NULL,
  nodeID              VARCHAR(100)  NOT NULL,
  rosterGroup         VARCHAR(100)  NOT NULL
);
CREATE INDEX ofPubsubNodeGroups_idx ON ofPubsubNodeGroups (serviceID, nodeID);

CREATE TABLE ofPubsubAffiliation (
  serviceID           VARCHAR(100)  NOT NULL,
  nodeID              VARCHAR(100)  NOT NULL,
  jid                 VARCHAR(1024) NOT NULL,
  affiliation         VARCHAR(10)   NOT NULL,
  CONSTRAINT ofPubsubAffiliation_pk PRIMARY KEY (serviceID, nodeID, jid)
);

CREATE TABLE ofPubsubItem (
  serviceID           VARCHAR(100)  NOT NULL,
  nodeID              VARCHAR(100)  NOT NULL,
  id                  VARCHAR(100)  NOT NULL,
  jid                 VARCHAR(1024) NOT NULL,
  creationDate        CHAR(15)      NOT NULL,
  payload             VARCHAR(4000) NULL,
  CONSTRAINT ofPubsubItem_pk PRIMARY KEY (serviceID, nodeID, id)
);

CREATE TABLE ofPubsubSubscription (
  serviceID           VARCHAR(100)  NOT NULL,
  nodeID              VARCHAR(100)  NOT NULL,
  id                  VARCHAR(100)  NOT NULL,
  jid                 VARCHAR(1024) NOT NULL,
  owner               VARCHAR(1024) NOT NULL,
  state               VARCHAR(15)   NOT NULL,
  deliver             INTEGER       NOT NULL,
  digest              INTEGER       NOT NULL,
  digest_frequency    INTEGER       NOT NULL,
  expire              CHAR(15)      NULL,
  includeBody         INTEGER       NOT NULL,
  showValues          VARCHAR(30)   NOT NULL,
  subscriptionType    VARCHAR(10)   NOT NULL,
  subscriptionDepth   INTEGER       NOT NULL,
  keyword             VARCHAR(200)  NULL,
  CONSTRAINT ofPubsubSubscription_pk PRIMARY KEY (serviceID, nodeID, id)
);

CREATE TABLE ofPubsubDefaultConf (
  serviceID           VARCHAR(100)  NOT NULL,
  leaf                INTEGER       NOT NULL,
  deliverPayloads     INTEGER       NOT NULL,
  maxPayloadSize      INTEGER       NOT NULL,
  persistItems        INTEGER       NOT NULL,
  maxItems            INTEGER       NOT NULL,
  notifyConfigChanges INTEGER       NOT NULL,
  notifyDelete        INTEGER       NOT NULL,
  notifyRetract       INTEGER       NOT NULL,
  presenceBased       INTEGER       NOT NULL,
  sendItemSubscribe   INTEGER       NOT NULL,
  publisherModel      VARCHAR(15)   NOT NULL,
  subscriptionEnabled INTEGER       NOT NULL,
  accessModel         VARCHAR(10)   NOT NULL,
  language            VARCHAR(255)  NULL,
  replyPolicy         VARCHAR(15)   NULL,
  associationPolicy   VARCHAR(15)   NOT NULL,
  maxLeafNodes        INTEGER       NOT NULL,
  CONSTRAINT ofPubsubDefaultConf_pk PRIMARY KEY (serviceID, leaf)
);

INSERT INTO ofID (idType, id) VALUES (18, 1);
INSERT INTO ofID (idType, id) VALUES (19, 1);
INSERT INTO ofID (idType, id) VALUES (23, 1);
INSERT INTO ofID (idType, id) VALUES (26, 2);

INSERT INTO ofVersion (name, version) VALUES ('openfire', 21);

INSERT INTO ofUser (username, plainPassword, name, email, creationDate, modificationDate)
    VALUES ('admin', 'admin', 'Administrator', 'admin@example.com', '0', '0');

INSERT INTO ofMucService (serviceID, subdomain, isHidden) VALUES (1, 'conference', 0);


INSERT INTO ofVersion (name, version) VALUES ('mmxappmgmt', 0);

CREATE TABLE mmxApp (
  id                INTEGER       NOT NULL GENERATED BY DEFAULT AS IDENTITY,
  serverUserId      VARCHAR(200),
  appName           VARCHAR(100)  NOT NULL,
  appId             VARCHAR(16)   NOT NULL,
  apiKey            VARCHAR(100)  DEFAULT NULL,
  encryptedApiKey   VARCHAR(255)  DEFAULT NULL,
  googleApiKey      VARCHAR(100),
  googleProjectId   VARCHAR(100),
  apnsCert          VARBINARY(5000),
  apnsCertPlainPassword VARCHAR(100)      DEFAULT NULL,
  apnsCertEncryptedPassword VARCHAR(255)  DEFAULT NULL,
  apnsCertProduction TINYINT      NULL,
  creationDate      DATETIME      NOT NULL,
  modificationDate  DATETIME     NULL,
  ownerId           VARCHAR(200),
  ownerEmail        VARCHAR(255) NULL,
  guestUserId       VARCHAR(200),
  guestSecret       VARCHAR(255),
  CONSTRAINT mmxApp_pk PRIMARY KEY (appId)
);

CREATE INDEX mmxApp_serverUserJJID ON mmxApp(serverUserId);
CREATE INDEX mmxApp_apiKey ON mmxApp(apiKey);

CREATE TABLE mmxDevice (
  id            INTEGER        NOT NULL GENERATED BY DEFAULT AS IDENTITY,
  name          varchar(100)   NOT NULL,
  ownerJid     varchar(200)    NOT NULL,
  appId        varchar(16)    NOT NULL,
  osType       varchar(20)     NOT NULL,
  deviceId     varchar(50)     NOT NULL,
  tokenType    varchar(10),
  clientToken  varchar(500),
  versionInfo  varchar(20),
  modelInfo    varchar(200),
  phoneNumber  varchar(20),
  phoneNumberRev  varchar(20),
  carrierInfo  varchar(20),
  dateCreated  datetime     NOT NULL,
  dateUpdated  datetime     NULL,
  status       varchar(20)  DEFAULT 'ACTIVE',
  protocolVersionMajor  INTEGER,
  protocolVersionMinor  INTEGER,
  pushStatus   varchar(20)  NULL,
  CONSTRAINT mmxDevice_pk PRIMARY KEY (id),
  CONSTRAINT devicid_type_osType_appid UNIQUE (deviceId,osType, appId)
);

CREATE TABLE mmxMessage (
  id               INTEGER         NOT NULL GENERATED BY DEFAULT AS IDENTITY,
  messageId        varchar(100)    NOT NULL,
  deviceId         varchar(50)     NOT NULL,
  fromJID          varchar(200)    NOT NULL,
  toJID            varchar(200)    NOT NULL,
  dateQueuedUTC    INTEGER        NOT NULL,
  state            varchar(50)     NOT NULL,
  appId            varchar(16)     NOT NULL,
  dateAcknowledgedUTC INTEGER,
  sourceMessageId  varchar(100),
  messageType      varchar(50),
  CONSTRAINT mmxMessage_pk PRIMARY KEY (id)
);

CREATE INDEX mmxMessage_messageId ON mmxMessage(messageId);

 CREATE TABLE mmxWakeupQueue (
    id              INTEGER        NOT NULL GENERATED BY DEFAULT AS IDENTITY,
    deviceId        varchar(50)    NOT NULL,
    clientToken     varchar(500),
    tokenType       varchar(10),
    appId           varchar(16) NULL,
    googleApiKey    varchar(100),
    payload         varchar(400)   NOT NULL,
    messageId       varchar(100)   NOT NULL,
    dateCreatedUTC     INTEGER,
    dateSentUTC        INTEGER,
    CONSTRAINT mmxWakeupQueue_pk PRIMARY KEY (id)
 );

CREATE INDEX mmxWakeupQueue_dateCreated ON mmxWakeupQueue(dateCreatedUTC);

CREATE INDEX mmxWakeupQueue_dateSent ON mmxWakeupQueue(dateSentUTC);

/* TABLE for maintaining the push messages */
 CREATE TABLE mmxPushMessage (
    messageId   varchar(50)     NOT NULL,
    deviceId    varchar(50)     NOT NULL,
    appId       varchar(16)     NOT NULL,
    dateSentUTC            INTEGER        NOT NULL,
    dateAcknowledgedUTC    INTEGER,
    type        varchar(16)     NOT NULL,
    state       varchar(50)     NOT NULL,
    CONSTRAINT mmxPushMessage_pk PRIMARY KEY (messageId)
 );

/* Table for maintaining resource tagging data */

create Table mmxTag (
  id INTEGER NOT NULL GENERATED BY DEFAULT AS IDENTITY,
  tagname VARCHAR(25) NOT NULL,
  creationDate DATETIME NOT NULL,
  appid VARCHAR(16) NOT NULL,
  deviceId INTEGER,
  username VARCHAR(64),
  serviceID varchar(100) DEFAULT NULL,
  nodeID varchar(100) DEFAULT NULL,
  CONSTRAINT tagname_appId_deviceId UNIQUE (tagname, appId, deviceId),
  CONSTRAINT tagname_appId_username UNIQUE  (tagname, appId, username),
  CONSTRAINT tagname_appId_topic UNIQUE (tagname,appid,serviceID, nodeID),
  CONSTRAINT fk_deviceId FOREIGN KEY (deviceId) REFERENCES mmxDevice(id) ON DELETE CASCADE,
  CONSTRAINT fk_username FOREIGN KEY (username) REFERENCES ofUser(username) ON DELETE CASCADE,
  CONSTRAINT fk_service_node FOREIGN KEY (serviceID, nodeID) REFERENCES ofPubsubNode(serviceID, nodeID) ON DELETE CASCADE,
  CONSTRAINT mmxTag_pk PRIMARY KEY (id)
);

/* Table for app specific configuration */
CREATE TABLE mmxAppConfiguration (
    id          INTEGER NOT NULL GENERATED BY DEFAULT AS IDENTITY,
    appId       VARCHAR(16)     NOT NULL,
    configKey   VARCHAR(100)    NOT NULL,
    configValue VARCHAR(300)    NOT NULL,
    CONSTRAINT mmxAppConfiguration_pk PRIMARY KEY (id)
);

ALTER TABLE mmxAppConfiguration ADD CONSTRAINT mmxAppConfiguration_uk UNIQUE (appId, configKey);

INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.admin.api.https.port', '6061') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.cluster.max.apps', '-1') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.rest.https.port', '5221') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.rest.http.port', '5220') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.alert.email.bcc.list', '') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.alert.email.subject', 'Usage limit exceeded') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.alert.email.port', '') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.retry.count', '0') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.alert.email.user', '') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.domain.name', '') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.cluster.max.devices.per.app', '-1') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.alert.email.enabled', 'false') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.rest.enable.https', 'true') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.wakeup.initialwait', '10') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.admin.api.enable.https', 'true') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.alert.inter.email.time.minutes', '15') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.retry.interval.minutes', '15') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.admin.api.port', '6060') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.alert.email.password', '') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.retry.mechanism', 'Standard') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.alert.email.host', '') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.timeout.period.minutes', '180') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.wakeup.frequency', '30') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.push.callback.host', '') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.push.callback.protocol', 'http') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.push.callback.port', '5220') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.apns.feedback.initialwait.min', '10') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.apns.feedback.frequency.min', '360') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.instance.max.xmpp.rate.per.sec', '-1') ;
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.instance.max.http.rate.per.sec', '-1') ;

