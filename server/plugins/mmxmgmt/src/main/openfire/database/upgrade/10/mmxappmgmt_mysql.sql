UPDATE ofVersion SET version=10 WHERE name = 'mmxappmgmt';

ALTER TABLE mmxWakeupQueue MODIFY payload VARCHAR(2000);

/*** push config ***/
 CREATE TABLE mmxTemplate (
   templateId int(11) NOT NULL AUTO_INCREMENT,
   appId varchar(45) COLLATE utf8_unicode_ci NOT NULL,
   templateType varchar(45) COLLATE utf8_unicode_ci NOT NULL,
   templateName varchar(45) COLLATE utf8_unicode_ci NOT NULL,
   template text COLLATE utf8_unicode_ci NOT NULL,
   PRIMARY KEY (templateId),
   UNIQUE KEY template_name_unq (appId,templateName)
 ) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

 CREATE TABLE mmxPushConfig (
   configId int(11) NOT NULL AUTO_INCREMENT,
   appId varchar(255) COLLATE utf8_unicode_ci NOT NULL,
   configName varchar(255) COLLATE utf8_unicode_ci NOT NULL,
   isEnabled bit(1) NOT NULL,
   isSilentPush bit(1) NOT NULL,
   templateId int(11) NOT NULL,
   PRIMARY KEY (configId),
   UNIQUE KEY i_push_config_unq (appId,configName)
 ) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

 CREATE TABLE mmxPushConfigMetadata (
   metadataId int(11) NOT NULL AUTO_INCREMENT,
   configId int(11) NOT NULL,
   name varchar(255) COLLATE utf8_unicode_ci NOT NULL,
   value varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
   PRIMARY KEY (metadataId),
   UNIQUE KEY i_push_config_meta_unq (configId,name)
 ) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

 CREATE TABLE mmxPushConfigMapping (
   mappingId int(11) NOT NULL AUTO_INCREMENT,
   appId varchar(255) COLLATE utf8_unicode_ci NOT NULL,
   channelId varchar(255) COLLATE utf8_unicode_ci NOT NULL,
   configId int(11) NOT NULL,
   PRIMARY KEY (mappingId),
   UNIQUE KEY i_push_config_mapp_unq (appId,channelId)
 ) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

 CREATE TABLE mmxPushSuppress (
   suppressId int(11) NOT NULL AUTO_INCREMENT,
   appId varchar(255) COLLATE utf8_unicode_ci NOT NULL,
   channelId varchar(255) COLLATE utf8_unicode_ci NOT NULL,
   untilDate bigint(20) DEFAULT NULL,
   userId varchar(255) COLLATE utf8_unicode_ci NOT NULL,
   PRIMARY KEY (suppressId),
   UNIQUE KEY i_push_suppress_unq (appId,userId,channelId)
 ) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;


 INSERT INTO mmxTemplate(templateId, appId,templateType,templateName,template)
 VALUES (0, 'system', 'PUSH', 'default-template',
 'mmx.pubsub.notification.type=push\nmmx.pubsub.notification.title=\nmmx.pubsub.notification.body=New message from ${msg.from}\nmmx.pubsub.notification.sound=default\n'
 );

 INSERT INTO mmxTemplate(templateId, appId,templateType,templateName,template)
 VALUES (1, 'system', 'PUSH', 'DefaultPollTemplate',
 'mmx.pubsub.notification.type=push\nmmx.pubsub.notification.title=\nmmx.pubsub.notification.body=Poll: ${msg.content.question[0..*30]}\nmmx.pubsub.notification.sound=default\n'
 );

 INSERT INTO mmxTemplate(templateId, appId,templateType,templateName,template)
 VALUES (2, 'system', 'PUSH', 'DefaultPollAnswerTemplate',
 'mmx.pubsub.notification.type=push\nmmx.pubsub.notification.title=\nmmx.pubsub.notification.body=${msg.from} voted on ${msg.content.question[0..*30]}\nmmx.pubsub.notification.sound=default\n'
 );

 INSERT INTO mmxPushConfig(configId, appId,configName,isEnabled,isSilentPush,templateId)
 select 0, t.appId, 'default-config', true, false, t.templateId
 from mmxTemplate t
 where t.appId = 'system'
 and t.templateName = 'default-template';

 INSERT INTO mmxPushConfig(configId, appId,configName,isEnabled,isSilentPush,templateId)
 select 1, t.appId, 'DefaultPollConfig', true, false, t.templateId
 from mmxTemplate t
 where t.appId = 'system'
 and t.templateName = 'DefaultPollTemplate';

 INSERT INTO mmxPushConfig(configId, appId,configName,isEnabled,isSilentPush,templateId)
 select 2, t.appId, 'DefaultPollAnswerConfig', true, false, t.templateId
 from mmxTemplate t
 where t.appId = 'system'
 and t.templateName = 'DefaultPollAnswerTemplate';

 INSERT INTO mmxPushConfigMapping(mappingId, appId,channelId,configId)
 select 0, c.appId, '', c.configId
 from mmxPushConfig c
 where c.appId = 'system'
 and c.configName = 'default-config';

 INSERT INTO mmxPushConfigMapping(mappingId, appId,channelId,configId)
 select 1, c.appId, '', c.configId
 from mmxPushConfig c
 where c.appId = 'system'
 and c.configName = 'DefaultPollConfig';

 INSERT INTO mmxPushConfigMapping(mappingId, appId,channelId,configId)
 select 2, c.appId, '', c.configId
 from mmxPushConfig c
 where c.appId = 'system'
 and c.configName = 'DefaultPollAnswerConfig';
