
UPDATE ofVersion set version=3 where name = 'mmxappmgmt';

INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.admin.api.https.port', '6061') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.cluster.max.apps', '-1') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.rest.https.port', '5221') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.rest.http.port', '5220') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.alert.email.bcc.list', '') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.alert.email.subject', 'Usage limit exceeded') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.alert.email.port', '') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.retry.count', '0') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.alert.email.user', '') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.instance.max.inapp.message.rate.per.sec', '-1') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
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
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.instance.max.push.message.rate.per.sec', '-1') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.alert.email.host', '') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.timeout.period.minutes', '180') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.push.callbackurl', '') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.wakeup.frequency', '30') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.push.callback.host', '') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.push.callback.protocol', 'http') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.push.callback.port', '5220') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.apns.feedback.initialwait.min', '2') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);
INSERT INTO ofProperty (name, propValue) VALUES( 'mmx.apns.feedback.frequency.min', '360') ON DUPLICATE KEY UPDATE name=VALUES(name), propValue=VALUES(propValue);

DELETE FROM ofProperty WHERE name = 'mmx.push.callbackurl';

ALTER TABLE mmxDevice ADD COLUMN pushStatus varchar(20)  NULL;
UPDATE mmxDevice SET pushStatus = 'VALID' WHERE clientToken IS NOT NULL;


ALTER TABLE mmxMessage DROP COLUMN dateQueued;
ALTER TABLE mmxMessage DROP COLUMN dateAcknowledged;

ALTER TABLE mmxMessage ADD COLUMN dateQueuedUTC int(11);
ALTER TABLE mmxMessage ADD COLUMN dateAcknowledgedUTC int(11);

ALTER TABLE mmxWakeupQueue DROP COLUMN dateCreated;
ALTER TABLE mmxWakeupQueue DROP COLUMN dateSent;

ALTER TABLE mmxWakeupQueue ADD COLUMN dateCreatedUTC int(11);
ALTER TABLE mmxWakeupQueue ADD COLUMN dateSentUTC  int(11);

ALTER TABLE mmxPushMessage ADD COLUMN dateSentUTC int(11);
ALTER TABLE mmxPushMessage ADD COLUMN dateAcknowledgedUTC int(11);

ALTER TABLE mmxPushMessage DROP COLUMN dateSent;
ALTER TABLE mmxPushMessage DROP COLUMN dateAcknowledged;
